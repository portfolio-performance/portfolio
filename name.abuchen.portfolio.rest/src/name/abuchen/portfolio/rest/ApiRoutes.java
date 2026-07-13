package name.abuchen.portfolio.rest;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.internal.AccountsHandler;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.internal.FileResolver;
import name.abuchen.portfolio.rest.internal.FilesHandler;
import name.abuchen.portfolio.rest.internal.PortfoliosHandler;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.internal.SecuritiesHandler;
import name.abuchen.portfolio.rest.spi.HostApplication;

/**
 * Registers all v1 routes. Every route is marshalled to the UI thread; writes
 * are additionally rejected with 423 while an application-modal dialog is open
 * or the user edits a table cell.
 */
public final class ApiRoutes
{
    private ApiRoutes()
    {
    }

    public static Router create(FileAccessRegistry registry, HostApplication host)
    {
        var router = new Router();
        var resolver = new FileResolver(registry, host);
        var files = new FilesHandler(registry, host);

        router.add("GET", "/v1/files", onUiThread(host, files::list)); //$NON-NLS-1$ //$NON-NLS-2$

        router.add("GET", "/v1/files/{file}/instruments", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, SecuritiesHandler.list(client))));
        router.add("GET", "/v1/files/{file}/instruments/{uuid}", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, SecuritiesHandler.get(client, req.pathParam("uuid"))))); //$NON-NLS-1$
        router.add("PATCH", "/v1/files/{file}/instruments/{uuid}", write(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200,
                                        SecuritiesHandler.patch(client, req.pathParam("uuid"), parseObject(req))))); //$NON-NLS-1$
        router.add("DELETE", "/v1/files/{file}/instruments/{uuid}", write(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> {
                            SecuritiesHandler.delete(client, req.pathParam("uuid")); //$NON-NLS-1$
                            return Response.noContent();
                        }));

        router.add("GET", "/v1/files/{file}/cash-accounts", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, AccountsHandler.list(client))));
        router.add("GET", "/v1/files/{file}/cash-accounts/{uuid}", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, AccountsHandler.get(client, req.pathParam("uuid"))))); //$NON-NLS-1$

        router.add("GET", "/v1/files/{file}/investment-accounts", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, PortfoliosHandler.list(client))));
        router.add("GET", "/v1/files/{file}/investment-accounts/{uuid}", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, PortfoliosHandler.get(client, req.pathParam("uuid"))))); //$NON-NLS-1$

        return router;
    }

    /**
     * Runs the handler on the UI thread. Everything that touches the open files
     * or the model must go through here - including resolving the {file}
     * segment, which reads the list of open files.
     */
    private static Router.Handler onUiThread(HostApplication host, Router.Handler handler)
    {
        return request -> host.syncExec(() -> handler.handle(request));
    }

    private static Router.Handler read(FileResolver resolver, HostApplication host,
                    BiFunction<Client, Request, Response> body)
    {
        return onUiThread(host, request -> {
            var resolved = resolver.resolve(request.pathParam("file")); //$NON-NLS-1$
            return body.apply(resolved.file().getClient(), request);
        });
    }

    private static Router.Handler write(FileResolver resolver, HostApplication host,
                    BiFunction<Client, Request, Response> body)
    {
        return onUiThread(host, request -> {
            // resolve first: an unknown file is a 404 even while the user edits
            var resolved = resolver.resolve(request.pathParam("file")); //$NON-NLS-1$

            if (host.isUserEditing())
                throw ApiException.locked();

            return body.apply(resolved.file().getClient(), request);
        });
    }

    private static JsonObject parseObject(Request request)
    {
        try
        {
            var element = JsonParser.parseString(new String(request.body(), StandardCharsets.UTF_8));
            if (!element.isJsonObject())
                throw ApiException.badRequest("request body must be a JSON object"); //$NON-NLS-1$
            return element.getAsJsonObject();
        }
        catch (JsonSyntaxException e)
        {
            throw ApiException.badRequest("request body is not valid JSON"); //$NON-NLS-1$
        }
    }
}
