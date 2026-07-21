package name.abuchen.portfolio.rest;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.rest.internal.AccountsHandler;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.internal.FileResolver;
import name.abuchen.portfolio.rest.internal.FilesHandler;
import name.abuchen.portfolio.rest.internal.HoldingsHandler;
import name.abuchen.portfolio.rest.internal.OpenApiHandler;
import name.abuchen.portfolio.rest.internal.PairingHandler;
import name.abuchen.portfolio.rest.internal.PerformanceHandler;
import name.abuchen.portfolio.rest.internal.PortfoliosHandler;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.internal.SecuritiesHandler;
import name.abuchen.portfolio.rest.spi.HostApplication;

/**
 * Registers all v1 routes. Reads and writes are marshalled to the UI thread;
 * writes are additionally rejected with 423 while an application-modal dialog
 * is open or the user edits a table cell. Calculation endpoints only resolve
 * the {file} scope on the UI thread and compute on the HTTP worker thread.
 */
public final class ApiRoutes
{
    private ApiRoutes()
    {
    }

    public static Router create(FileAccessRegistry registry, HostApplication host, PairingService pairing)
    {
        var router = new Router();
        var resolver = new FileResolver(registry, host);
        var files = new FilesHandler(registry, host);

        // the API's own description: a static resource, no UI thread, no auth
        router.add("GET", RestApiConstants.OPENAPI_ENDPOINT, request -> OpenApiHandler.serve()); //$NON-NLS-1$

        // pairing endpoints run on the HTTP worker thread: the service is
        // thread-safe and prompting the user is asynchronous by contract
        router.add("POST", "/v1/auth/requests", //$NON-NLS-1$ //$NON-NLS-2$
                        request -> PairingHandler.create(pairing, parseObject(request)));
        router.add("GET", "/v1/auth/requests/{id}", //$NON-NLS-1$ //$NON-NLS-2$
                        request -> PairingHandler.poll(pairing, request.pathParam("id"))); //$NON-NLS-1$

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

        router.add("GET", "/v1/files/{file}/holdings", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, HoldingsHandler.list(context.client(), context.factory(),
                                        req.queryParam("date"), req.queryParam("currency"))))); //$NON-NLS-1$ //$NON-NLS-2$

        router.add("GET", "/v1/files/{file}/performance", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, PerformanceHandler.list(context.client(),
                                        context.factory(), req.queryParam("openingDate"), //$NON-NLS-1$
                                        req.queryParam("closingDate"), req.queryParam("currency"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("costMethod"))))); //$NON-NLS-1$

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

    /** what a calculation endpoint needs, fetched from the host on the UI thread */
    /* package */ record CalcContext(Client client, ExchangeRateProviderFactory factory)
    {
    }

    /**
     * For read-only calculation endpoints: resolves the {file} scope on the UI
     * thread, but runs the calculation itself on the HTTP worker thread so
     * that an expensive computation cannot freeze the UI. Deliberately without
     * a consistency guard: a concurrent user edit may - rarely - yield a
     * transiently inconsistent response or an internal error; retrying is
     * cheap for the client, blocking the UI is not.
     */
    private static Router.Handler calc(FileResolver resolver, HostApplication host,
                    BiFunction<CalcContext, Request, Response> body)
    {
        return request -> {
            var context = host.syncExec(() -> {
                var file = resolver.resolve(request.pathParam("file")).file(); //$NON-NLS-1$
                return new CalcContext(file.getClient(), file.getExchangeRateProviderFactory());
            });
            return body.apply(context, request);
        };
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
