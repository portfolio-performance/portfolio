package name.abuchen.portfolio.ui.addons;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.RestApiConstants;
import name.abuchen.portfolio.rest.RestApiServer;
import name.abuchen.portfolio.rest.RestApiWorkspace;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.ui.Messages;
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
    }

    @Inject
    private ClientInputFactory clientInputFactory;

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
        var tokenStore = RestApiWorkspace.createTokenStore();
        var port = preferences.getInt(RestApiConstants.PREF_PORT, RestApiConstants.DEFAULT_PORT);

        try
        {
            server = new RestApiServer(port, tokenStore::getOrCreate, ApiRoutes.create(registry, new Host()));
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
}
