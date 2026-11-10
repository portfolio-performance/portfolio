package name.abuchen.portfolio.ui.addons;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

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
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ApiAccessApprovalDialog;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.editor.EditorActivationState;

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
