package name.abuchen.portfolio.ui.addons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.RestApiConstants;
import name.abuchen.portfolio.rest.RestApiServer;
import name.abuchen.portfolio.rest.RestApiWorkspace;
import name.abuchen.portfolio.rest.spi.ApiAccessRequest;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.rest.spi.PasswordRequiredException;
import name.abuchen.portfolio.rest.spi.PriceUpdateTarget;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ApiAccessApprovalDialog;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.editor.ClientInputListener;
import name.abuchen.portfolio.ui.editor.EditorActivationState;
import name.abuchen.portfolio.ui.handlers.OpenFileHandler;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;

/**
 * Starts and stops the REST API server with the application and implements
 * the HostApplication SPI of the rest plugin on top of ClientInputFactory
 * and the SWT display.
 */
public class RestApiAddon
{
    private static final class ClientInputOpenFile implements OpenFile
    {
        private final ClientInput input;

        private ClientInputOpenFile(ClientInput input)
        {
            this.input = input;
        }

        @Override
        public String getPath()
        {
            return input.getFile().getAbsolutePath();
        }

        @Override
        public String getLabel()
        {
            return input.getLabel();
        }

        @Override
        public Client getClient()
        {
            return input.getClient();
        }

        @Override
        public ExchangeRateProviderFactory getExchangeRateProviderFactory()
        {
            // created eagerly in ClientInput#setClient; listOpenFiles filters
            // inputs without a client, so this is never null here
            return input.getExchangeRateProviderFacory();
        }

        @Override
        public boolean isDirty()
        {
            return input.isDirty();
        }

        @Override
        public void save() throws IOException
        {
            input.saveWithoutUI();
        }
    }

    private final class Host implements HostApplication
    {
        @Override
        public List<OpenFile> listOpenFiles()
        {
            return clientInputFactory.listOpenClients().stream() //
                            .filter(input -> input.getFile() != null && input.getClient() != null) //
                            .<OpenFile>map(ClientInputOpenFile::new) //
                            .toList();
        }

        @Override
        public <T> T syncExec(Callable<T> callable) throws Exception
        {
            var result = new AtomicReference<T>();
            var failure = new AtomicReference<Exception>();
            Display.getDefault().syncExec(() -> {
                try
                {
                    result.set(callable.call());
                }
                catch (Exception e)
                {
                    failure.set(e);
                }
            });
            if (failure.get() != null)
                throw failure.get();
            return result.get();
        }

        @Override
        public boolean isUserEditing()
        {
            return isModalShellOpen() || EditorActivationState.isAnyEditorActive();
        }

        private boolean isModalShellOpen()
        {
            var modal = SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL | SWT.PRIMARY_MODAL;
            for (var shell : Display.getDefault().getShells())
            {
                if (shell.isVisible() && (shell.getStyle() & modal) != 0)
                    return true;
            }
            return false;
        }

        @Override
        public void requestApiAccessApproval(ApiAccessRequest request)
        {
            Display.getDefault().asyncExec(() -> showApprovalWhenIdle(this, request));
        }

        @Override
        public CompletableFuture<OpenFile> openFile(Path path) throws IOException
        {
            return RestApiAddon.this.openFile(path);
        }

        @Override
        public void startPriceUpdate(OpenFile file, List<Security> securities, Set<PriceUpdateTarget> targets,
                        Consumer<IStatus> onDone)
        {
            var selected = Collections.newSetFromMap(new IdentityHashMap<Security, Boolean>());
            selected.addAll(securities);

            var jobTargets = EnumSet.noneOf(UpdatePricesJob.Target.class);
            if (targets.contains(PriceUpdateTarget.LATEST))
                jobTargets.add(UpdatePricesJob.Target.LATEST);
            if (targets.contains(PriceUpdateTarget.HISTORIC))
                jobTargets.add(UpdatePricesJob.Target.HISTORIC);

            var job = new UpdatePricesJob(file.getClient(), selected::contains, jobTargets);
            // nobody is asked to log in on the API's behalf
            job.suppressAuthenticationDialog(true);
            job.addJobChangeListener(new JobChangeAdapter()
            {
                @Override
                public void done(IJobChangeEvent event)
                {
                    onDone.accept(event.getResult());
                }
            });
            job.schedule();
        }
    }

    @Inject
    private ClientInputFactory clientInputFactory;

    @Inject
    private ECommandService commandService;

    @Inject
    private MApplication application;

    @Inject
    private EModelService modelService;

    @Inject
    private EHandlerService handlerService;

    private IEclipsePreferences preferences;
    private RestApiServer server;

    private final IEclipsePreferences.IPreferenceChangeListener listener = event -> {
        if (RestApiConstants.PREF_ENABLED.equals(event.getKey()) || RestApiConstants.PREF_PORT.equals(event.getKey()))
            Display.getDefault().asyncExec(this::restart);
    };

    @Inject
    @Optional
    public void onAppStartupComplete(@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) Event event)
    {
        preferences = RestApiWorkspace.preferences();
        preferences.addPreferenceChangeListener(listener);
        restart();
    }

    @PreDestroy
    public void shutdown()
    {
        if (preferences != null)
            preferences.removePreferenceChangeListener(listener);
        stopServer();
    }

    private void restart()
    {
        stopServer();

        if (preferences == null || !preferences.getBoolean(RestApiConstants.PREF_ENABLED, false))
            return;

        var registry = RestApiWorkspace.createFileAccessRegistry();
        var clientStore = RestApiWorkspace.getClientStore();
        var port = preferences.getInt(RestApiConstants.PREF_PORT, RestApiConstants.DEFAULT_PORT);

        try
        {
            var host = new Host();
            server = new RestApiServer(port, token -> clientStore.authenticate(token).isPresent(),
                            ApiRoutes.create(registry, host, new PairingService(clientStore, host)));
            server.start();
            PortfolioLog.info(MessageFormat.format(Messages.MsgRestApiServerStarted, server.getPort()));
        }
        catch (IOException e)
        {
            // deliberately no port hopping: log and leave the server off
            server = null;
            PortfolioLog.error(e);
        }
    }

    private void stopServer()
    {
        if (server != null)
        {
            server.stop();
            server = null;
        }
    }

    /**
     * Shows the approval dialog on the UI thread. While the user is in the
     * middle of an edit (modal dialog, cell editor), the prompt is deferred
     * rather than stacked on top; the request meanwhile stays pending.
     */
    private void showApprovalWhenIdle(HostApplication host, ApiAccessRequest request)
    {
        if (!Instant.now().isBefore(request.getExpiresAt()))
            return; // expired while waiting; the service reports it

        if (host.isUserEditing())
        {
            Display.getDefault().timerExec(500, () -> showApprovalWhenIdle(host, request));
            return;
        }

        var anyFileEnabled = RestApiWorkspace.createFileAccessRegistry().all().stream()
                        .anyMatch(FileAccessRegistry.FileAccess::enabled);

        new ApiAccessApprovalDialog(Display.getDefault().getActiveShell(), request, anyFileEnabled,
                        this::openRestApiPreferences).open();
    }

    /**
     * Opens the file like the File/Open command does, minus the file dialog: a portfolio
     * part is created (or an existing one for the same file activated), which
     * loads the file in the background. The future completes once the client
     * is loaded. Encrypted files are refused because the part would prompt the
     * user for the password.
     */
    private CompletableFuture<OpenFile> openFile(Path path) throws IOException
    {
        var fileName = path.toString();
        var file = new File(fileName);

        if (!file.isFile())
            throw new FileNotFoundException(fileName);

        if (ClientFactory.isEncrypted(file))
            throw new PasswordRequiredException(fileName);

        var mainStack = (MPartStack) modelService.find(UIConstants.PartStack.MAIN, application);
        var partService = modelService.getTopLevelWindowFor(mainStack).getContext().get(EPartService.class);

        var existing = modelService.findElements(application, UIConstants.Part.PORTFOLIO, MPart.class, null)
                        .stream() //
                        .filter(part -> fileName
                                        .equals(part.getPersistedState().get(UIConstants.PersistedState.FILENAME)))
                        .findFirst();

        if (existing.isPresent())
        {
            partService.showPart(existing.get(), PartState.ACTIVATE);
        }
        else
        {
            // create the input before the part does, so that the jobs
            // scheduled after loading do not show modal dialogs
            var input = clientInputFactory.lookup(file);
            if (input.getClient() == null)
                input.setInteractive(false);

            OpenFileHandler.openPart(fileName, partService.getActivePart(), application, partService, modelService);
        }

        // the part looked up (and started loading) the input when it was
        // rendered; the factory returns that same cached input
        var input = clientInputFactory.lookup(file);

        var future = new CompletableFuture<OpenFile>();

        if (input.getClient() != null)
        {
            future.complete(new ClientInputOpenFile(input));
            return future;
        }

        var listener = new ClientInputListener()
        {
            @Override
            public void onLoaded()
            {
                future.complete(new ClientInputOpenFile(input));
            }

            @Override
            public void onError(String message)
            {
                future.completeExceptionally(new IOException(message));
            }

            @Override
            public void onDisposed()
            {
                future.completeExceptionally(new IOException(MessageFormat.format("{0} was closed", fileName))); //$NON-NLS-1$
            }
        };
        input.addListener(listener);

        // the listeners are notified while iterating the listener list, hence
        // remove the listener only after the notification is done
        future.whenComplete((result, error) -> Display.getDefault().asyncExec(() -> input.removeListener(listener)));

        return future;
    }

    private void openRestApiPreferences()
    {
        try
        {
            var command = commandService.getCommand(UIConstants.Command.PREFERENCES);
            var page = command.getParameter(UIConstants.Parameter.PAGE);
            var parameterized = new ParameterizedCommand(command,
                            new Parameterization[] { new Parameterization(page, "restapi") }); //$NON-NLS-1$
            if (handlerService.canExecute(parameterized))
                handlerService.executeHandler(parameterized);
        }
        catch (NotDefinedException e)
        {
            PortfolioLog.error(e);
        }
    }
}
