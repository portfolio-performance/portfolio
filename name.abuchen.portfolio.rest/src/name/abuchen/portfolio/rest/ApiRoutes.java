package name.abuchen.portfolio.rest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
import name.abuchen.portfolio.rest.internal.IdempotencyIndex;
import name.abuchen.portfolio.rest.internal.InstrumentChangeLog;
import name.abuchen.portfolio.rest.internal.InvestmentPlansHandler;
import name.abuchen.portfolio.rest.internal.MasterDataWrites;
import name.abuchen.portfolio.rest.internal.OpenApiHandler;
import name.abuchen.portfolio.rest.internal.PairingHandler;
import name.abuchen.portfolio.rest.internal.PerformanceCalendarHandler;
import name.abuchen.portfolio.rest.internal.PerformanceHandler;
import name.abuchen.portfolio.rest.internal.PortfoliosHandler;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.internal.SecuritiesHandler;
import name.abuchen.portfolio.rest.internal.SecurityEventsHandler;
import name.abuchen.portfolio.rest.internal.SecurityPerformanceHandler;
import name.abuchen.portfolio.rest.internal.SecurityPricesHandler;
import name.abuchen.portfolio.rest.internal.TaxonomiesHandler;
import name.abuchen.portfolio.rest.internal.TradesHandler;
import name.abuchen.portfolio.rest.internal.TransactionsHandler;
import name.abuchen.portfolio.rest.internal.WatchlistsHandler;
import name.abuchen.portfolio.rest.internal.WriteContext;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;

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
        // idempotency keys of master data creates, see IdempotencyIndex
        var idempotency = new IdempotencyIndex();

        // the API's own description: a static resource, no UI thread, no auth
        router.add("GET", RestApiConstants.OPENAPI_ENDPOINT, request -> OpenApiHandler.serve()); //$NON-NLS-1$

        // pairing endpoints run on the HTTP worker thread: the service is
        // thread-safe and prompting the user is asynchronous by contract
        router.add("POST", "/v1/auth/requests", //$NON-NLS-1$ //$NON-NLS-2$
                        request -> PairingHandler.create(pairing, parseObject(request)));
        router.add("GET", "/v1/auth/requests/{id}", //$NON-NLS-1$ //$NON-NLS-2$
                        request -> PairingHandler.poll(pairing, request.pathParam("id"))); //$NON-NLS-1$

        router.add("GET", "/v1/files", onUiThread(host, files::list)); //$NON-NLS-1$ //$NON-NLS-2$
        // literal segment: must precede the {file} routes (Router is first-match).
        // Runs on the HTTP worker thread: the handler marshals to the UI thread
        // itself and waits for the file to load outside of it
        router.add("POST", "/v1/files/open", //$NON-NLS-1$ //$NON-NLS-2$
                        request -> files.open(FilesHandler.openRequestPath(parseObject(request))));
        router.add("GET", "/v1/files/{file}", onUiThread(host, //$NON-NLS-1$ //$NON-NLS-2$
                        request -> Response.json(200, FilesHandler.get(resolver.resolve(request.pathParam("file")))))); //$NON-NLS-1$
        router.add("POST", "/v1/files/{file}/save", writeResolved(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (resolved, req) -> Response.json(200, FilesHandler.save(resolved))));

        router.add("GET", "/v1/files/{file}/instruments", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, SecuritiesHandler.list(client))));
        router.add("POST", "/v1/files/{file}/instruments", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = SecuritiesHandler.create(context, idempotency, parseObject(req));
                            return created(req, result, "instruments", result.entity().get("uuid").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
                        }));
        // literal sub-collection: must precede the {uuid} route (Router is first-match)
        router.add("GET", "/v1/files/{file}/instruments/attribute-types", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, SecuritiesHandler.attributeTypes(client))));
        router.add("GET", "/v1/files/{file}/instruments/{uuid}", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, SecuritiesHandler.get(client, req.pathParam("uuid"))))); //$NON-NLS-1$
        router.add("GET", "/v1/files/{file}/instruments/{uuid}/prices", read(resolver, host,
                        (client, req) -> Response.json(200, SecurityPricesHandler.list(client,
                                        req.pathParam("uuid"), req.queryParam("from"), req.queryParam("to")))));
        router.add("PUT", "/v1/files/{file}/instruments/{uuid}/prices", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200,
                                        SecurityPricesHandler.upsert(context, req.pathParam("uuid"), parseObject(req))))); //$NON-NLS-1$
        router.add("DELETE", "/v1/files/{file}/instruments/{uuid}/prices", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, SecurityPricesHandler.delete(context,
                                        req.pathParam("uuid"), req.queryParam("from"), req.queryParam("to"))))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        router.add("GET", "/v1/files/{file}/instruments/{uuid}/events", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, SecurityEventsHandler.list(client, req.pathParam("uuid"))))); //$NON-NLS-1$
        router.add("POST", "/v1/files/{file}/instruments/{uuid}/events", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = SecurityEventsHandler.create(context, idempotency, req.pathParam("uuid"), //$NON-NLS-1$
                                            parseObject(req));
                            return Response.json(result.changed() ? 201 : 200, result.entity());
                        }));
        router.add("DELETE", "/v1/files/{file}/instruments/{uuid}/events", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, SecurityEventsHandler.delete(context,
                                        req.pathParam("uuid"), req.queryParam("date"), req.queryParam("type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        req.queryParam("details"))))); //$NON-NLS-1$
        // PATCH accepts application/json as well as application/merge-patch+json:
        // the body is a JSON Merge Patch either way
        router.add("PATCH", "/v1/files/{file}/instruments/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = SecuritiesHandler.patch(context.client(), req.pathParam("uuid"), //$NON-NLS-1$
                                            parseObject(req), context.dryRun());
                            if (!context.dryRun())
                                InstrumentChangeLog.record(context.file().getLabel(), result.instrumentName(),
                                                result.changes());
                            return Response.json(200, result.entity());
                        }));
        router.add("DELETE", "/v1/files/{file}/instruments/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            if (context.dryRun())
                                return Response.json(200,
                                                SecuritiesHandler.deletePreview(context.client(), req.pathParam("uuid"))); //$NON-NLS-1$
                            var name = SecuritiesHandler.delete(context.client(), req.pathParam("uuid")); //$NON-NLS-1$
                            InstrumentChangeLog.recordDeletion(context.file().getLabel(), name);
                            return Response.noContent();
                        }));

        router.add("GET", "/v1/files/{file}/cash-accounts", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200,
                                        AccountsHandler.list(context.client(), context.factory(), req.queryParam("date"))))); //$NON-NLS-1$
        router.add("POST", "/v1/files/{file}/cash-accounts", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = AccountsHandler.create(context, idempotency, parseObject(req));
                            return created(req, result, "cash-accounts", result.entity().get("uuid").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
                        }));
        router.add("GET", "/v1/files/{file}/cash-accounts/{uuid}", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, AccountsHandler.get(context.client(), context.factory(),
                                        req.pathParam("uuid"), req.queryParam("date"))))); //$NON-NLS-1$ //$NON-NLS-2$
        router.add("PATCH", "/v1/files/{file}/cash-accounts/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, AccountsHandler
                                        .patch(context, req.pathParam("uuid"), parseObject(req)).entity()))); //$NON-NLS-1$
        router.add("DELETE", "/v1/files/{file}/cash-accounts/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> deleted(AccountsHandler.delete(context, req.pathParam("uuid"))))); //$NON-NLS-1$

        router.add("GET", "/v1/files/{file}/investment-accounts", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, PortfoliosHandler.list(context.client(), context.factory(),
                                        req.queryParam("date"), req.queryParam("currency"))))); //$NON-NLS-1$ //$NON-NLS-2$
        router.add("POST", "/v1/files/{file}/investment-accounts", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = PortfoliosHandler.create(context, idempotency, parseObject(req));
                            return created(req, result, "investment-accounts", //$NON-NLS-1$
                                            result.entity().get("uuid").getAsString()); //$NON-NLS-1$
                        }));
        router.add("GET", "/v1/files/{file}/investment-accounts/{uuid}", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, PortfoliosHandler.get(context.client(), context.factory(),
                                        req.pathParam("uuid"), req.queryParam("date"), req.queryParam("currency"))))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        router.add("PATCH", "/v1/files/{file}/investment-accounts/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, PortfoliosHandler
                                        .patch(context, req.pathParam("uuid"), parseObject(req)).entity()))); //$NON-NLS-1$
        router.add("DELETE", "/v1/files/{file}/investment-accounts/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> deleted(PortfoliosHandler.delete(context, req.pathParam("uuid"))))); //$NON-NLS-1$

        // watchlists and investment plans have no identifier: addressed by their (unique) name
        router.add("GET", "/v1/files/{file}/watchlists", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, WatchlistsHandler.list(client))));
        router.add("POST", "/v1/files/{file}/watchlists", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = WatchlistsHandler.create(context, idempotency, parseObject(req));
                            return created(req, result, "watchlists", result.entity().get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
                        }));
        router.add("GET", "/v1/files/{file}/watchlists/{name}", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, WatchlistsHandler.get(client, req.pathParam("name"))))); //$NON-NLS-1$
        router.add("PATCH", "/v1/files/{file}/watchlists/{name}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, WatchlistsHandler
                                        .patch(context, req.pathParam("name"), parseObject(req)).entity()))); //$NON-NLS-1$
        router.add("DELETE", "/v1/files/{file}/watchlists/{name}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> deleted(WatchlistsHandler.delete(context, req.pathParam("name"))))); //$NON-NLS-1$
        router.add("PUT", "/v1/files/{file}/watchlists/{name}/instruments/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, WatchlistsHandler.addInstrument(context,
                                        req.pathParam("name"), req.pathParam("uuid")).entity()))); //$NON-NLS-1$ //$NON-NLS-2$
        router.add("DELETE", "/v1/files/{file}/watchlists/{name}/instruments/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, WatchlistsHandler.removeInstrument(context,
                                        req.pathParam("name"), req.pathParam("uuid")).entity()))); //$NON-NLS-1$ //$NON-NLS-2$

        router.add("GET", "/v1/files/{file}/investment-plans", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, InvestmentPlansHandler.list(client))));
        router.add("POST", "/v1/files/{file}/investment-plans", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = InvestmentPlansHandler.create(context, idempotency, parseObject(req));
                            return created(req, result, "investment-plans", //$NON-NLS-1$
                                            result.entity().get("name").getAsString()); //$NON-NLS-1$
                        }));
        router.add("GET", "/v1/files/{file}/investment-plans/{name}", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, InvestmentPlansHandler.get(client, req.pathParam("name"))))); //$NON-NLS-1$
        router.add("PATCH", "/v1/files/{file}/investment-plans/{name}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, InvestmentPlansHandler
                                        .patch(context, req.pathParam("name"), parseObject(req)).entity()))); //$NON-NLS-1$
        router.add("DELETE", "/v1/files/{file}/investment-plans/{name}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> deleted(InvestmentPlansHandler.delete(context, req.pathParam("name"))))); //$NON-NLS-1$

        router.add("GET", "/v1/files/{file}/taxonomies", read(resolver, host,
                        (client, req) -> Response.json(200, TaxonomiesHandler.list(client))));

        router.add("GET", "/v1/files/{file}/transactions", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, TransactionsHandler.list(client,
                                        new TransactionsHandler.Filter(req.queryParam("from"), //$NON-NLS-1$
                                                        req.queryParam("to"), req.queryParam("type"), //$NON-NLS-1$ //$NON-NLS-2$
                                                        req.queryParam("instrument"), //$NON-NLS-1$
                                                        req.queryParam("cashAccount"), //$NON-NLS-1$
                                                        req.queryParam("investmentAccount")))))); //$NON-NLS-1$
        router.add("POST", "/v1/files/{file}/transactions", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var result = TransactionsHandler.create(context, parseObject(req));
                            if (!result.changed())
                                return Response.json(200, result.entity());

                            var location = "/v1/files/" + req.pathParam("file") + "/transactions/" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                            + result.entity().get("uuid").getAsString(); //$NON-NLS-1$
                            return new Response(201, "application/json", //$NON-NLS-1$
                                            result.entity().toString().getBytes(StandardCharsets.UTF_8),
                                            Map.of("Location", location)); //$NON-NLS-1$
                        }));
        router.add("GET", "/v1/files/{file}/transactions/{uuid}", read(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> Response.json(200, TransactionsHandler.get(client, req.pathParam("uuid"))))); //$NON-NLS-1$
        // PATCH accepts application/json as well as application/merge-patch+json:
        // the body is a JSON Merge Patch either way
        router.add("PATCH", "/v1/files/{file}/transactions/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, TransactionsHandler
                                        .patch(context, req.pathParam("uuid"), parseObject(req)).entity()))); //$NON-NLS-1$
        router.add("DELETE", "/v1/files/{file}/transactions/{uuid}", writeWith(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> {
                            var preview = TransactionsHandler.delete(context, req.pathParam("uuid")); //$NON-NLS-1$
                            return preview != null ? Response.json(200, preview) : Response.noContent();
                        }));

        router.add("GET", "/v1/files/{file}/holdings", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, HoldingsHandler.list(context.client(), context.factory(),
                                        req.queryParam("date"), req.queryParam("openingDate"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("currency"), req.queryParam("costMethod"))))); //$NON-NLS-1$ //$NON-NLS-2$

        router.add("GET", "/v1/files/{file}/performance", calc(resolver, host, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, PerformanceHandler.list(context.client(),
                                        context.factory(), req.queryParam("openingDate"), //$NON-NLS-1$
                                        req.queryParam("closingDate"), req.queryParam("currency"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("costMethod"))))); //$NON-NLS-1$

        router.add("GET", "/v1/files/{file}/performance/series", calc(resolver, host,
                        (context, req) -> Response.json(200, PerformanceHandler.series(context.client(),
                                        context.factory(), req.queryParam("openingDate"),
                                        req.queryParam("closingDate"), req.queryParam("currency")))));

        router.add("GET", "/v1/files/{file}/performance/calendar", calc(resolver, host,
                        (context, req) -> Response.json(200, PerformanceCalendarHandler.list(context.client(),
                                        context.factory(), req.queryParam("openingDate"),
                                        req.queryParam("closingDate"), req.queryParam("currency")))));

        router.add("GET", "/v1/files/{file}/trades", calc(resolver, host,
                        (context, req) -> Response.json(200, TradesHandler.list(context.client(), context.factory(),
                                        req.queryParam("currency"), req.queryParam("onlyClosed")))));

        router.add("GET", "/v1/files/{file}/performance/securities", calc(resolver, host,
                        (context, req) -> Response.json(200, SecurityPerformanceHandler.list(context.client(),
                                        context.factory(), req.queryParam("openingDate"),
                                        req.queryParam("closingDate"), req.queryParam("currency"),
                                        req.queryParam("costMethod")))));

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
                    BiFunction<OpenFile, Request, Response> body)
    {
        return writeResolved(resolver, host, (resolved, request) -> body.apply(resolved.file(), request));
    }

    /**
     * Like {@link #write}, but hands the body a {@link WriteContext}: the file
     * plus the dry-run flag and the idempotency key of the request. Used by
     * the handlers that support {@code ?dry_run=true} and {@code clientRef}.
     */
    /* package */ static Router.Handler writeWith(FileResolver resolver, HostApplication host,
                    BiFunction<WriteContext, Request, Response> body)
    {
        return writeResolved(resolver, host,
                        (resolved, request) -> body.apply(WriteContext.of(resolved.file(), request), request));
    }

    /** true if the request asks for a dry run ({@code ?dry_run=true}); 400 for an invalid value */
    /* package */ static boolean isDryRun(Request request)
    {
        return WriteContext.isDryRun(request);
    }

    /** like {@link #write}, for handlers that also need the file's access record (id, alias) */
    private static Router.Handler writeResolved(FileResolver resolver, HostApplication host,
                    BiFunction<FileResolver.ResolvedFile, Request, Response> body)
    {
        return onUiThread(host, request -> {
            // resolve first: an unknown file is a 404 even while the user edits
            var resolved = resolver.resolve(request.pathParam("file")); //$NON-NLS-1$

            if (host.isUserEditing())
                throw ApiException.locked();

            return body.apply(resolved, request);
        });
    }

    /**
     * The answer of a create: {@code 201} with a {@code Location} if the
     * entity was created, {@code 200} for a dry run or a replayed create.
     * {@code id} is the new entity's identifier within {@code collection},
     * percent-encoded as one path segment.
     */
    private static Response created(Request request, MasterDataWrites.WriteResult result, String collection,
                    String id)
    {
        if (!result.changed())
            return Response.json(200, result.entity());

        var location = "/v1/files/" + request.pathParam("file") + "/" + collection + "/" + encodeSegment(id); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return new Response(201, "application/json", //$NON-NLS-1$
                        result.entity().toString().getBytes(StandardCharsets.UTF_8), Map.of("Location", location)); //$NON-NLS-1$
    }

    /** the answer of a delete: the preview of a dry run ({@code 200}), else {@code 204} */
    private static Response deleted(JsonObject preview)
    {
        return preview != null ? Response.json(200, preview) : Response.noContent();
    }

    /** percent-encodes a value as a single path segment (a space is %20, not +) */
    /* package */ static String encodeSegment(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); //$NON-NLS-1$ //$NON-NLS-2$
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
