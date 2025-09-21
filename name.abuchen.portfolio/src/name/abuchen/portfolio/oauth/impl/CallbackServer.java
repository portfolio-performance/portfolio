package name.abuchen.portfolio.oauth.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.apache.hc.core5.http.HttpStatus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;

public class CallbackServer
{
    private static class SuccessHttpHandler implements HttpHandler
    {
        private Consumer<AuthorizationCode> callbackHandler;

        public SuccessHttpHandler(Consumer<AuthorizationCode> callbackHandler)
        {
            this.callbackHandler = callbackHandler;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            String query = exchange.getRequestURI().getQuery();
            AuthorizationCode codeResponse = AuthorizationCode.parse(query);
            handleResponse(exchange, codeResponse);
        }

        private void handleResponse(HttpExchange exchange, AuthorizationCode authorizationCode) throws IOException
        {
            var htmlResponse = getSuccessHtml();

            byte[] responseBytes = htmlResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
            exchange.sendResponseHeaders(HttpStatus.SC_OK, responseBytes.length);

            try (OutputStream outputStream = exchange.getResponseBody())
            {
                outputStream.write(responseBytes);
            }

            callbackHandler.accept(authorizationCode);
        }

        private String getSuccessHtml()
        {
            try (Scanner scanner = new Scanner(CallbackServer.class.getResourceAsStream("success.html"), //$NON-NLS-1$
                            StandardCharsets.UTF_8.name()))
            {
                var template = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

                template = template.replace("{heading}", Messages.LabelLoginSuccessful); //$NON-NLS-1$

                template = template.replace("{message}", //$NON-NLS-1$
                                Messages.LabelCloseBrowserWindowAndGoBackToApplication);
                return template;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return "<html><body>" + Messages.LabelLoginSuccessful + "</body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    private static final String HOSTNAME = "localhost"; //$NON-NLS-1$
    private static final int[] PORTS = new int[] { 49968, 55968, 59968 };

    private static final String ENDPOINT_SUCCESS = "/success"; //$NON-NLS-1$
    private static final int NO_DELAY = 0;

    private HttpServer server;
    private ScheduledExecutorService scheduledExecutorService;

    private Consumer<AuthorizationCode> callbackHandler;

    public void setCallbackHandler(Consumer<AuthorizationCode> callbackHandler)
    {
        this.callbackHandler = callbackHandler;
    }

    public void start() throws IOException
    {
        if (this.server != null)
        {
            stop();
        }

        for (int port : PORTS)
        {
            try
            {
                this.server = HttpServer.create(new InetSocketAddress(HOSTNAME, port), 0);
                break;
            }
            catch (BindException e)
            {
                PortfolioLog.info(MessageFormat.format(Messages.OAuthPortInUse, port));
            }
        }

        if (this.server == null)
            throw new IOException(Messages.OAuthFailedToStartCallbackServer);

        this.server.createContext(ENDPOINT_SUCCESS, new SuccessHttpHandler(code -> callbackHandler.accept(code)));
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.server.setExecutor(scheduledExecutorService);
        this.server.start();
        PortfolioLog.info(MessageFormat.format(Messages.OAuthCallbackServerStarted, HOSTNAME,
                        this.server.getAddress().getPort()));
    }

    public void stop()
    {
        if (this.server != null)
        {
            this.server.stop(NO_DELAY);
            this.scheduledExecutorService.shutdown();
        }
    }

    public String getSuccessEndpoint()
    {
        if (this.server == null)
            throw new IllegalArgumentException(Messages.OAuthCallbackServerNotRunning);
        return String.format("http://%s:%d%s", HOSTNAME, this.server.getAddress().getPort(), ENDPOINT_SUCCESS); //$NON-NLS-1$
    }
}
