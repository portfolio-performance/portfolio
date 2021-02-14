package name.abuchen.portfolio.server;

import java.util.EnumSet;

import javax.inject.Inject;
import javax.servlet.DispatcherType;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;

import name.abuchen.portfolio.ui.UIConstants;

public class WebServerAddon
{
    private static final String PATH = "/api/v0/*"; //$NON-NLS-1$

    @Inject
    private IEclipseContext context;

    private int serverPort = 5712;

    private Server webServer;

    @Inject
    @Optional
    public void setServerPort(
                    @Preference(nodePath = "name.abuchen.portfolio.ui", value = UIConstants.Preferences.WEB_SERVER_PORT) int serverPort)
    {
        if (this.serverPort == serverPort)
            return;

        this.serverPort = serverPort;

        if (webServer != null)
        {
            stopWebServer();
            startWebServer();
        }
    }

    @Inject
    @Optional
    public void runWebServer(
                    @Preference(nodePath = "name.abuchen.portfolio.ui", value = UIConstants.Preferences.RUN_WEB_SERVER) boolean runWebServer)
    {
        if (runWebServer)
        {
            // already started
            if (webServer != null)
                return;

            startWebServer();
        }
        else
        {
            // already stopped
            if (webServer == null)
                return;

            stopWebServer();
        }
    }

    private void startWebServer()
    {
        webServer = new Server(serverPort);

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/"); //$NON-NLS-1$
        servletContextHandler.setClassLoader(getClass().getClassLoader());
        servletContextHandler.addFilter(
                        new FilterHolder(ContextInjectionFactory.make(TokenAuthenticationFilter.class, context)), PATH,
                        EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(new FilterHolder(ContextInjectionFactory.make(SetClientFilter.class, context)),
                        PATH, EnumSet.of(DispatcherType.REQUEST));

        webServer.setHandler(servletContextHandler);

        ServletHolder servletHolder = servletContextHandler.addServlet(ServletContainer.class, PATH);
        servletHolder.setInitOrder(0);
        servletHolder.setInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS, JerseyApplication.class.getName());

        try
        {
            webServer.start();
        }
        catch (Exception e)
        {
            ServerLog.error(e);
            webServer = null;
        }
    }

    private void stopWebServer()
    {
        try
        {
            webServer.stop();
            webServer.destroy();
        }
        catch (Exception e)
        {
            ServerLog.error(e);
        }
        finally
        {
            webServer = null;
        }
    }
}
