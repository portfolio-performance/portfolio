package name.abuchen.portfolio.ui.addons;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
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
import name.abuchen.portfolio.rest.spi.WriteConfirmation;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ApiAccessApprovalDialog;
import name.abuchen.portfolio.ui.dialogs.ModelWriteApprovalDialog;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.editor.EditorActivationState;
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
        public <T> CompletableFuture<T> asyncExec(Callable<T> callable)
        {
            var display = Display.getDefault();

            // on the UI thread already: queueing the callable behind the code
            // that is running and waiting for it would wait for ever
            if (display.getThread() == Thread.currentThread())
                return run(callable);

            var future = new CompletableFuture<T>();
            try
            {
                display.asyncExec(() -> {
                    try
                    {
                        future.complete(callable.call());
                    }
                    catch (Exception e)
                    {
                        future.completeExceptionally(e);
                    }
                    catch (Error e)
                    {
                        // the caller learns of it; the application's error
                        // handler still sees it, as it did with syncExec
                        future.completeExceptionally(e);
                        throw e;
                    }
                });
            }
            catch (RuntimeException e)
            {
                // the display is disposed: the application is shutting down
                future.completeExceptionally(e);
            }
            return future;
        }

        private static <T> CompletableFuture<T> run(Callable<T> callable)
        {
            try
            {
                return CompletableFuture.completedFuture(callable.call());
            }
            catch (Exception e)
            {
                return CompletableFuture.failedFuture(e);
            }
            catch (Error e)
            {
                // as on the queue: the caller learns of it, and so does the log
                PortfolioLog.error(e);
                return CompletableFuture.failedFuture(e);
            }
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
        public void requestModelWriteApproval(WriteConfirmation request)
        {
            // called from an HTTP worker thread; the dialog belongs to the UI
            // thread and the call must not wait, because the caller parks on the
            // answer rather than on this method
            Display.getDefault().asyncExec(() -> showWriteApprovalWhenIdle(this, request));
        }

        @Override
        public void updateQuotes(Client client, List<Security> securities)
        {
            // the application's own job, exactly as the menu item schedules it:
            // its progress dialog, its feeds, its markDirty() when a quote lands
            new UpdatePricesJob(client, securities).schedule();
        }

        @Override
        public void updateAllQuotes(Client client)
        {
            new UpdatePricesJob(client, EnumSet.allOf(UpdatePricesJob.Target.class)).schedule();
        }
    }

    @Inject
    private ClientInputFactory clientInputFactory;

    @Inject
    private ECommandService commandService;

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
     * Shows the dialog asking whether a change to a file may be made, on the UI
     * thread and not while the user is in the middle of an edit. Unlike the
     * access prompt this one has an owner waiting: the route parks on the answer,
     * so a prompt deferred too long simply ends with the request's expiry and the
     * change is refused as unanswered - never applied without a yes.
     */
    private void showWriteApprovalWhenIdle(HostApplication host, WriteConfirmation request)
    {
        if (!Instant.now().isBefore(request.getExpiresAt()))
            return; // expired while waiting; the caller already reported it

        if (host.isUserEditing())
        {
            Display.getDefault().timerExec(500, () -> showWriteApprovalWhenIdle(host, request));
            return;
        }

        try
        {
            new ModelWriteApprovalDialog(Display.getDefault().getActiveShell(), request).open();
        }
        catch (RuntimeException | Error e)
        {
            // nothing was shown, so nothing was ever asked. Refuse rather than
            // let the caller sit out the expiry of a question nobody could see:
            // a refusal says plainly that no change was made, and it says it now
            PortfolioLog.error(e);
            request.decline();
        }
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
