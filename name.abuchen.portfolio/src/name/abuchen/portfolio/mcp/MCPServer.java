package name.abuchen.portfolio.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.apache.hc.core5.http.HttpStatus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import name.abuchen.portfolio.PortfolioLog;

public class MCPServer
{
    private static final String HOSTNAME = "127.0.0.1"; //$NON-NLS-1$
    private static final String ENDPOINT = "/mcp"; //$NON-NLS-1$
    private static final String PROTOCOL_VERSION = "2024-11-05"; //$NON-NLS-1$
    private static final int NO_DELAY = 0;

    private final MCPTools tools;
    private final int port;

    private HttpServer server;
    private ExecutorService executorService;

    public MCPServer(MCPTools tools, int port)
    {
        this.tools = tools;
        this.port = port;
    }

    public void start() throws IOException
    {
        if (server != null)
            stop();

        server = HttpServer.create(new InetSocketAddress(HOSTNAME, port), 0);
        server.createContext(ENDPOINT, this::handleExchange);
        executorService = Executors.newSingleThreadExecutor();
        server.setExecutor(executorService);
        server.start();

        PortfolioLog.info("MCP server started at http://" + HOSTNAME + ":" + port + ENDPOINT); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public void stop()
    {
        if (server != null)
        {
            server.stop(NO_DELAY);
            server = null;
        }

        if (executorService != null)
        {
            executorService.shutdown();
            executorService = null;
        }
    }

    public String getEndpointUrl()
    {
        return "http://" + HOSTNAME + ":" + port + ENDPOINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    JsonObject handle(JsonObject request)
    {
        if (!request.has("method"))
            return errorResponse(getId(request), -32600, "Invalid Request");

        String method = request.get("method").getAsString();
        JsonElement id = request.get("id");

        if (id == null || id.isJsonNull())
        {
            // notification
            if ("notifications/initialized".equals(method))
                return null;
            return null;
        }

        try
        {
            return switch (method)
            {
                case "initialize" -> successResponse(id, initializeResult());
                case "ping" -> successResponse(id, new JsonObject());
                case "tools/list" -> successResponse(id, toolsListResult());
                case "tools/call" -> successResponse(id, toolsCallResult(request));
                default -> errorResponse(id, -32601, "Method not found: " + method);
            };
        }
        catch (MCPException e)
        {
            return errorResponse(id, -32000, e.getMessage());
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return errorResponse(id, -32603, e.getMessage());
        }
    }

    private void handleExchange(HttpExchange exchange) throws IOException
    {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()))
        {
            exchange.sendResponseHeaders(HttpStatus.SC_METHOD_NOT_ALLOWED, -1);
            exchange.close();
            return;
        }

        String body;
        try (InputStream input = exchange.getRequestBody())
        {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        JsonObject request = JsonParser.parseString(body).getAsJsonObject();
        JsonObject response = handle(request);

        if (response == null)
        {
            exchange.sendResponseHeaders(HttpStatus.SC_ACCEPTED, -1);
            exchange.close();
            return;
        }

        byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
        exchange.sendResponseHeaders(HttpStatus.SC_OK, responseBytes.length);

        try (OutputStream output = exchange.getResponseBody())
        {
            output.write(responseBytes);
        }
    }

    private JsonObject initializeResult()
    {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", PROTOCOL_VERSION);

        JsonObject capabilities = new JsonObject();
        JsonObject toolsCapability = new JsonObject();
        capabilities.add("tools", toolsCapability);
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "portfolio-performance");
        serverInfo.addProperty("version", "0.84.3");
        result.add("serverInfo", serverInfo);

        return result;
    }

    private JsonObject toolsListResult()
    {
        JsonObject result = new JsonObject();
        result.add("tools", tools.listToolDefinitions());
        return result;
    }

    private JsonObject toolsCallResult(JsonObject request) throws Exception
    {
        JsonObject params = request.getAsJsonObject("params");
        if (params == null || !params.has("name"))
            throw new MCPException("Missing tool name");

        String name = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") && params.get("arguments").isJsonObject()
                        ? params.getAsJsonObject("arguments")
                        : new JsonObject();

        String text = tools.callTool(name, arguments);

        JsonObject contentItem = new JsonObject();
        contentItem.addProperty("type", "text");
        contentItem.addProperty("text", text);

        var content = new com.google.gson.JsonArray();
        content.add(contentItem);

        JsonObject result = new JsonObject();
        result.add("content", content);
        result.addProperty("isError", false);
        return result;
    }

    private JsonObject successResponse(JsonElement id, JsonObject result)
    {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        response.add("result", result);
        return response;
    }

    private JsonObject errorResponse(JsonElement id, int code, String message)
    {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (id != null && !id.isJsonNull())
            response.add("id", id);

        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return response;
    }

    private JsonElement getId(JsonObject request)
    {
        return request.has("id") ? request.get("id") : null;
    }
}
