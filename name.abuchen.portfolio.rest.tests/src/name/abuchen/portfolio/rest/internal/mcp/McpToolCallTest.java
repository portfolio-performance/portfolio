package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * What a tool call does short of the wire: the arguments it insists on, the
 * route it reaches, and what it says when the API's own answer would mislead.
 */
@SuppressWarnings("nls")
public class McpToolCallTest
{
    private static final String FILE = "/tmp/mcp-call.portfolio";

    private IEclipsePreferences node;
    private Router router;
    private FakeHost host;
    private Client client;
    private Security security;
    private String fileId;

    @Before
    public void setUp()
    {
        client = new Client();
        security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(FILE, true);
        registry.setAlias(FILE, "sample");
        fileId = registry.byPath(FILE).orElseThrow().uuid();

        host = new FakeHost(List.of(new FakeHost.FakeOpenFile(FILE, "mcp", client)));
        router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    @Test
    public void testAFileIsAddressableByAliasAsWellAsById()
    {
        assertThat(text(call("pp_list_instruments", "{\"file\":\"sample\"}")), containsString("ACME"));
        assertThat(text(call("pp_list_instruments", "{\"file\":\"" + fileId + "\"}")), containsString("ACME"));
    }

    /** handed back inline, so the model recovers in this call rather than spending a turn */
    @Test
    public void testAMissingFileAnswersWithTheFilesThemselves()
    {
        var result = call("pp_list_instruments", "{}");

        assertThat(isError(result), is(true));
        assertThat(text(result), containsString("no default file"));
        // the address to send, which is the id - the alias is for display
        assertThat(text(result), containsString("use `" + fileId + "`"));
    }

    @Test
    public void testAnUnknownToolIsATooErrorNotAProtocolError()
    {
        var result = call("pp_list_kittens", "{}");

        assertThat(isError(result), is(true));
        assertThat(text(result), containsString("tools/list"));
    }

    /** ignoring a misspelled argument would answer a question nobody asked */
    @Test
    public void testAnUnknownArgumentIsRefusedWithTheAcceptedNames()
    {
        var result = call("pp_list_holdings", "{\"file\":\"sample\",\"Date\":\"2026-01-01\"}");

        assertThat(isError(result), is(true));
        assertThat(text(result), containsString("`Date`"));
        assertThat(text(result), containsString("did you mean `date`"));
    }

    /** every 404 is byte-identical, so the file list is what tells the two apart */
    @Test
    public void testAnUnknownFileAndAnUnknownEntityAreToldApart()
    {
        var unknownFile = text(call("pp_get_instrument", "{\"file\":\"nope\",\"uuid\":\"" + security.getUUID() + "\"}"));
        assertThat(unknownFile, containsString("not sharing a file called `nope`"));
        assertThat(unknownFile, containsString("These files are available"));

        var unknownEntity = text(call("pp_get_instrument", "{\"file\":\"sample\",\"uuid\":\"00000000-0000-0000-0000-000000000000\"}"));
        assertThat(unknownEntity, containsString("The file is shared"));
        assertThat(unknownEntity, containsString("will not start working"));
    }

    /**
     * A uuid is case-insensitive on input, and a stray space around an id is
     * not part of it: both used to be refused - one as not found, the other as
     * misshapen.
     */
    @Test
    public void testIdsAreFoundWhateverTheirCaseAndSurroundingSpace()
    {
        var upper = text(call("pp_get_instrument",
                        "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID().toUpperCase() + "\"}"));
        assertThat(upper, containsString("\"uuid\":\"" + security.getUUID() + "\""));

        var padded = call("pp_get_instrument",
                        "{\"file\":\" sample \",\"uuid\":\"" + security.getUUID() + " \"}");
        assertThat(isError(padded), is(false));
        assertThat(text(padded), containsString("\"uuid\":\"" + security.getUUID() + "\""));
    }

    /**
     * The prose tells a model what to do; the trailer states the same fact as
     * data. A 404 is never retryable — the same id will not start working — and
     * a locked server is, once the user has closed their dialog.
     */
    @Test
    public void testARefusalSaysWhetherTryingAgainCanHelp()
    {
        var notFound = text(call("pp_get_instrument", "{\"file\":\"sample\",\"uuid\":\"00000000-0000-0000-0000-000000000000\"}"));
        assertThat(notFound, containsString("retryable: no"));
        assertThat(notFound, containsString("reasonCode: not-found"));
        assertThat(notFound, not(containsString("retryAfterSeconds")));

        // only a write is refused while the user edits; reads still answer
        host.setUserEditing(true);
        var locked = text(call("pp_update_instrument",
                        "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID() + "\",\"note\":\"seen\"}"));
        assertThat(locked, containsString("retryable: yes"));
        assertThat(locked, containsString("reasonCode: user-interaction"));
        assertThat(locked, containsString("retryAfterSeconds: 5"));
    }

    /** the listing names the value to send as `file`, which is what stops a guess */
    @Test
    public void testTheFileListingNamesTheAddressToSend()
    {
        // always the id, even with an alias set: an alias can be renamed between
        // the listing and the next call
        var files = text(call("pp_list_files", "{}"));
        assertThat(files, containsString("\"columns\":[\"alias\",\"use\",\"label\"]"));
        assertThat(files, containsString("[\"sample\",\"" + fileId + "\",\"mcp\"]"));
        // the id is the use value again, and the local path no tool takes
        assertThat(files, not(containsString("\"id\"")));
        assertThat(files, not(containsString("\"path\"")));

        // and that value is what a tool accepts
        assertThat(isError(call("pp_list_instruments", "{\"file\":\"" + fileId + "\"}")), is(false));
    }

    @Test
    public void testAWriteSaysItIsNotSavedToDisk()
    {
        var result = call("pp_update_instrument",
                        "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID() + "\",\"note\":\"seen\"}");

        assertThat(isError(result), is(false));
        assertThat(security.getNote(), is("seen"));
        assertThat(text(result), containsString("not saved to disk"));
    }

    /** a patch that restates the stored value changed nothing, and says so */
    @Test
    public void testAPatchThatRestatesAValueSaysNothingChanged()
    {
        security.setNote("seen");
        var result = call("pp_update_instrument",
                        "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID() + "\",\"note\":\"seen\"}");

        assertThat(isError(result), is(false));
        assertThat(text(result), containsString("already had that value"));
        assertThat(text(result), not(containsString("not saved to disk")));
    }

    /** a merge patch clears a field when given null, which absent must not do */
    @Test
    public void testAnExplicitNullClearsAndAnAbsentArgumentDoesNot()
    {
        security.setIsin("DE0001234567");
        security.setWkn("123456");

        call("pp_update_instrument", "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID() + "\",\"isin\":null}");

        assertThat(security.getIsin(), is((String) null));
        assertThat(security.getWkn(), is("123456"));
    }

    @Test
    public void testADeleteNamesWhatItRemovedAndSaysItIsNotSaved()
    {
        var name = security.getName();
        var result = call("pp_delete_instrument",
                        "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID() + "\"}");

        assertThat(isError(result), is(false));
        assertThat(text(result), containsString("\"deleted\":true"));
        assertThat(text(result), containsString("\"uuid\":\"" + security.getUUID() + "\""));
        assertThat(text(result), containsString("\"name\":" + new JsonPrimitive(name)));
        assertThat(text(result), not(containsString("returned nothing to show")));
        assertThat(text(result), containsString("not saved to disk"));
        assertThat(client.getSecurities().size(), is(0));
    }

    /** `query` and the trade period reach their routes as query parameters */
    @Test
    public void testQueryAndPeriodArgumentsReachTheirRoutes()
    {
        var found = call("pp_list_instruments", "{\"file\":\"sample\",\"query\":\"acm\"}");
        assertThat(isError(found), is(false));
        assertThat(text(found), containsString(security.getUUID()));

        var none = text(call("pp_list_instruments", "{\"file\":\"sample\",\"query\":\"zzz\"}"));
        assertThat(none, containsString("\"items\":[]"));

        var trades = call("pp_list_trades", "{\"file\":\"sample\",\"from\":\"2020-01-01\",\"to\":\"2020-12-31\"}");
        assertThat(isError(trades), is(false));
        assertThat(text(trades), containsString("\"from\":\"2020-01-01\""));

        var reversed = text(call("pp_list_trades", "{\"file\":\"sample\",\"from\":\"2021-01-01\",\"to\":\"2020-01-01\"}"));
        assertThat(reversed, containsString("invalid-range"));
    }

    /** an array query argument is comma-joined, which is how the API spells it */
    @Test
    public void testAnArrayArgumentReachesTheRouteAsOneCommaJoinedParameter()
    {
        var result = call("pp_list_trades", "{\"file\":\"sample\",\"status\":[\"open\",\"closed\"]}");

        assertThat(isError(result), is(false));
        assertThat(text(result), containsString("items"));
    }

    /** the model must not be able to reach the operations that are not tools */
    @Test
    public void testThePairingAndSpecificationOperationsAreUnreachable()
    {
        for (String name : List.of("pp_get_openapi_document", "pp_create_pairing_request", "pp_poll_pairing_request"))
            assertThat(name, isError(call(name, "{}")), is(true));
    }

    /**
     * A list too large for one result is sent as its first page, not cut: as
     * many whole instruments as fit, a page that says where they lie, and the
     * offset of the next page named before the items - so the rest is one
     * call away instead of lost.
     */
    @Test
    public void testAnOversizedListComesBackAsItsFirstPage()
    {
        for (int ii = 0; ii < 200; ii++)
            new SecurityBuilder().addTo(client).setName("ACME " + ii + " " + "x".repeat(600));

        var result = text(call("pp_list_instruments", "{\"file\":\"sample\"}"));

        assertThat(result.length(), is(lessThanOrEqualTo(McpEndpoint.MAX_RESULT_CHARS)));
        assertThat(result, not(containsString("TRUNCATED")));
        assertThat(result, containsString("**One page of " + client.getSecurities().size() + ".**"));
        assertThat(result, containsString("`query` is quicker than paging"));

        var body = body(result);
        var page = body.getAsJsonObject("page");
        var rows = body.getAsJsonObject("items").getAsJsonArray("rows");
        assertThat(page.get("offset").getAsInt(), is(0));
        assertThat(page.get("total").getAsInt(), is(client.getSecurities().size()));
        assertThat(page.get("nextOffset").getAsInt(), is(rows.size()));
        assertThat(result, containsString("`offset: " + rows.size() + "`"));
        // the guidance comes before the items, where a reader still sees it
        assertThat(result.indexOf("One page of"), is(lessThan(result.indexOf("\"rows\""))));
    }

    /** following nextOffset walks the whole list, each instrument exactly once and in the file's order */
    @Test
    public void testThePagesOfAListFitTogether()
    {
        for (int ii = 0; ii < 200; ii++)
            new SecurityBuilder().addTo(client).setName("ACME " + ii + " " + "x".repeat(600));

        var seen = new java.util.ArrayList<String>();
        JsonElement next = new JsonPrimitive(0);
        var calls = 0;
        while (!next.isJsonNull())
        {
            assertThat("the pages must end", ++calls, is(lessThan(10)));
            var body = body(text(call("pp_list_instruments",
                            "{\"file\":\"sample\",\"offset\":" + next.getAsInt() + "}")));
            var columns = body.getAsJsonObject("items").getAsJsonArray("columns");
            var uuid = columns.asList().indexOf(new JsonPrimitive("uuid"));
            for (var row : body.getAsJsonObject("items").getAsJsonArray("rows"))
                seen.add(row.getAsJsonArray().get(uuid).getAsString());
            next = body.getAsJsonObject("page").get("nextOffset");
        }

        assertThat(calls, is(greaterThan(1)));
        assertThat(seen, is(client.getSecurities().stream().map(Security::getUUID).toList()));
    }

    /** a page the caller asked for says where the next one starts, even when it fits */
    @Test
    public void testARequestedPageNamesTheNextOffset()
    {
        for (int ii = 0; ii < 5; ii++)
            new SecurityBuilder().addTo(client).setName("ACME " + ii);

        var result = text(call("pp_list_instruments", "{\"file\":\"sample\",\"limit\":2}"));

        assertThat(result, containsString("This is one page of `page.total` items"));
        var page = body(result).getAsJsonObject("page");
        assertThat(page.get("limit").getAsInt(), is(2));
        assertThat(page.get("nextOffset").getAsInt(), is(2));
    }

    /** a limit or an offset of the wrong shape is refused before it reaches the route */
    @Test
    public void testAPagingArgumentOfTheWrongShapeIsRefused()
    {
        var fraction = call("pp_list_instruments", "{\"file\":\"sample\",\"limit\":2.5}");
        assertThat(isError(fraction), is(true));
        assertThat(text(fraction), containsString("`limit` must be `integer`"));

        var negative = call("pp_list_trades", "{\"file\":\"sample\",\"offset\":-1}");
        assertThat(isError(negative), is(true));
        assertThat(text(negative), containsString("`offset` must be at least 0"));
    }

    /**
     * An unmarked truncation is read as a complete answer. A list that cannot
     * be paged - the holdings - is still cut after a whole item, and says so,
     * with the advice surviving the cut.
     */
    @Test
    public void testAnOversizedResultThatCannotBePagedIsTruncatedWithAMarker()
    {
        var tool = McpTools.byName("pp_list_holdings").orElseThrow();

        var items = new JsonArray();
        for (int ii = 0; ii < 300; ii++)
        {
            var holding = new JsonObject();
            holding.addProperty("type", "instrument");
            holding.addProperty("name", "ACME " + ii + " " + "x".repeat(600));
            holding.addProperty("weight", 0.001);
            items.add(holding);
        }
        var payload = new JsonObject();
        payload.addProperty("date", "2024-12-31");
        payload.add("items", items);

        var fitted = McpEndpoint.fit(tool, JsonParser.parseString("{\"file\":\"sample\"}").getAsJsonObject(),
                        payload, McpEndpoint.MAX_RESULT_CHARS);

        // the guidance is reserved before the payload is cut, so the result uses
        // at most the allowance rather than filling it exactly
        assertThat(fitted.length(), is(lessThanOrEqualTo(McpEndpoint.MAX_RESULT_CHARS)));
        assertThat(fitted, containsString("TRUNCATED"));
        assertThat(fitted, containsString("ask a narrower question"));
        assertThat(fitted, containsString("more positions than fit in one answer"));
    }

    /**
     * The cap applied by the endpoint itself, on the one report that cannot be
     * narrowed: `pp_get_overall_performance` has no metric groups to drop, so the
     * advice is the only thing left worth keeping, and it is what tells a model to
     * answer from the fragment rather than guess the rest of the reconciliation.
     */
    @Test
    public void testTheAdviceForARouteThatCannotBeNarrowedSurvivesTheCap()
    {
        var tool = McpTools.byName("pp_get_overall_performance").orElseThrow();

        // a payload no fixture produces on its own: an oversized report is what
        // this path exists for, and only a synthetic one reaches the real limit
        var payload = new JsonObject();
        payload.addProperty("openingDate", "2024-01-01");
        payload.addProperty("closingDate", "2024-12-31");
        payload.addProperty("reportingCurrency", "EUR");

        var warnings = new JsonArray();
        for (int ii = 0; ii < 1_200; ii++)
        {
            var warning = new JsonObject();
            warning.addProperty("currency", "X" + ii);
            warning.addProperty("reportingCurrency", "EUR");
            warning.addProperty("reason", "no exchange rate series for this pair, converted at 1:1 " + ii);
            warnings.add(warning);
        }
        payload.add("fxWarnings", warnings);

        var arguments = JsonParser.parseString("{\"file\":\"sample\",\"openingDate\":\"2024-01-01\"}")
                        .getAsJsonObject();

        var fitted = McpEndpoint.fit(tool, arguments, payload, McpEndpoint.MAX_RESULT_CHARS);

        assertThat(fitted.length(), is(lessThanOrEqualTo(McpEndpoint.MAX_RESULT_CHARS)));
        assertThat(fitted, containsString("TRUNCATED"));
        assertThat(fitted, containsString("cannot be narrowed further"));
    }

    /**
     * An optional argument that is explicitly null says "no preference", which is
     * what omitting it says. Rejecting it would refuse questions the API can
     * answer, and a model writes null for that often enough to matter.
     */
    @Test
    public void testAnOptionalArgumentExplicitlyNullIsAccepted()
    {
        var holdings = call("pp_list_holdings",
                        "{\"file\":\"sample\",\"date\":null,\"reportingCurrency\":null}");
        assertThat(isError(holdings), is(false));

        var trades = call("pp_list_trades", "{\"file\":\"sample\",\"status\":null,\"grouping\":null}");
        assertThat(isError(trades), is(false));

        var performance = call("pp_get_overall_performance",
                        "{\"file\":\"sample\",\"openingDate\":\"2024-01-01\",\"closingDate\":null,\"costMethod\":null}");
        assertThat(isError(performance), is(false));
    }

    /**
     * A report that was narrowed to fit and still did not fit cannot be told to
     * narrow it again: the advice has to name what is left to change instead, and
     * it has to survive the cut, which is the part a model acts on.
     */
    @Test
    public void testTheAdviceAfterANarrowedReportNamesWhatIsLeftToChange()
    {
        var account = new AccountBuilder().addTo(client);

        // an instrument the interval never touched is not in the report at all,
        // so the fixture has to trade each one to make the report large
        for (int ii = 0; ii < 200; ii++)
        {
            var security = new SecurityBuilder().addPrice("2024-06-01", Values.Quote.factorize(1000)).addTo(client);
            security.setName("ACME " + ii + " " + "x".repeat(600));

            new PortfolioBuilder(account) //
                            .buy(security, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                            .sell(security, "2024-09-01", Values.Share.factorize(10), Values.Amount.factorize(1200)) //
                            .addTo(client);
        }

        var result = text(call("pp_list_instrument_performance",
                        "{\"file\":\"sample\",\"openingDate\":\"2024-01-01\",\"closingDate\":\"2024-12-31\"}"));

        assertThat(result.length(), is(lessThanOrEqualTo(McpEndpoint.MAX_RESULT_CHARS)));
        assertThat(result, containsString("TRUNCATED"));
        assertThat(result, containsString("already leaves out"));
        assertThat(result, containsString("`timeWeighted`"));
        assertThat(result, not(containsString("ask again without them")));
        assertThat(result, containsString("pp_get_instrument_performance"));
        assertThat(result, containsString("pp_get_instrument_performance for one instrument at a time."));
    }

    /**
     * Cutting the metric groups from the answer at hand gives exactly what the
     * narrowed request would have returned - for the list and for one item -
     * which is what makes it safe to skip the second calculation.
     */
    @Test
    public void testNarrowingIsWhatTheNarrowedRequestReturns() throws Exception
    {
        var account = new AccountBuilder().addTo(client);
        Security traded = null;
        for (int ii = 0; ii < 3; ii++)
        {
            traded = new SecurityBuilder().addPrice("2024-03-01", Values.Quote.factorize(100 + ii))
                            .addPrice("2024-06-01", Values.Quote.factorize(120 + ii)).addTo(client);
            traded.setName("Traded " + ii);
            new PortfolioBuilder(account) //
                            .buy(traded, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                            .sell(traded, "2024-09-01", Values.Share.factorize(5), Values.Amount.factorize(700)) //
                            .addTo(client);
        }

        var keep = McpEnvelope.NARROWED_METRICS;
        var narrowedQuery = Map.of("openingDate", "2024-01-01", "closingDate", "2024-12-31", "metrics",
                        String.join(",", keep));
        var fullQuery = Map.of("openingDate", "2024-01-01", "closingDate", "2024-12-31");

        var list = "/v1/files/" + fileId + "/performance/instruments";
        assertThat(McpEnvelope.narrow(get(list, fullQuery), keep), is(get(list, narrowedQuery)));

        var item = list + "/" + traded.getUUID();
        assertThat(McpEnvelope.narrow(get(item, fullQuery), keep), is(get(item, narrowedQuery)));
    }

    /**
     * A report too large to return is cut down from the answer already
     * computed: one trip to the UI thread for the file, one calculation - not a
     * second one for the narrowed question, which would take another slot.
     */
    @Test
    public void testAnOversizedReportIsNarrowedWithoutASecondCalculation() throws Exception
    {
        var account = new AccountBuilder().addTo(client);
        // in a table the names dominate, and narrowing cannot shorten them: many
        // instruments with short names are what makes the groups count
        for (int ii = 0; ii < 1000; ii++)
        {
            var traded = new SecurityBuilder().addPrice("2024-06-01", Values.Quote.factorize(1000)).addTo(client);
            traded.setName("ACME " + ii);
            new PortfolioBuilder(account) //
                            .buy(traded, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                            .sell(traded, "2024-09-01", Values.Share.factorize(10), Values.Amount.factorize(1200)) //
                            .addTo(client);
        }

        var path = "/v1/files/" + fileId + "/performance/instruments";
        var full = get(path, Map.of("openingDate", "2024-01-01", "closingDate", "2024-12-31"));
        // the fixture is built so that only the narrowed report fits, as the
        // model reads it
        var tool = McpTools.byName("pp_list_instrument_performance").orElseThrow();
        assertThat(McpEnvelope.render(tool, new JsonObject(), full, List.of()).length(),
                        is(greaterThan(McpEndpoint.MAX_RESULT_CHARS)));
        assertThat(McpEnvelope.render(tool, new JsonObject(), McpEnvelope.narrow(full, McpEnvelope.NARROWED_METRICS),
                        List.of()).length(), is(lessThan(McpEndpoint.MAX_RESULT_CHARS - 2_000)));

        var before = host.callablesRun();
        var result = text(call("pp_list_instrument_performance",
                        "{\"file\":\"sample\",\"openingDate\":\"2024-01-01\",\"closingDate\":\"2024-12-31\"}"));

        assertThat(result, containsString("Narrowed to fit"));
        assertThat(result, not(containsString("TRUNCATED")));
        assertThat(host.callablesRun() - before, is(1));
    }

    /** and an ordinary portfolio stays well below the cap */
    @Test
    public void testAnOrdinaryResultIsNotTruncated()
    {
        assertThat(text(call("pp_list_instruments", "{\"file\":\"sample\"}")), not(containsString("TRUNCATED")));
    }

    /**
     * A merge patch that names no writable field is a no-op the API accepts, not
     * a 400 about the body: the tool's arguments live in the path, so the body it
     * sends is empty, and an empty patch changes nothing rather than failing.
     */
    @Test
    public void testAnEmptyPatchReturnsTheInstrumentAndSaysNothingChanged()
    {
        var result = call("pp_update_instrument", "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID() + "\"}");

        assertThat(isError(result), is(false));
        assertThat(text(result), containsString("Nothing changed"));
        assertThat(text(result), not(containsString("not saved to disk")));
        assertThat(security.getNote(), is((String) null));
    }

    /** a value that cannot be an id must not reach a route that answers 404 - or worse */
    @Test
    public void testAUuidThatIsNotAnIdIsRefusedBeforeTheRoute()
    {
        var result = call("pp_get_instrument", "{\"file\":\"sample\",\"uuid\":\"attribute-types\"}");

        assertThat(isError(result), is(true));
        assertThat(text(result), containsString("`uuid` must be an entity uuid as pp_list_instruments reports it"));
        assertThat(text(result), containsString("reasonCode: misshapen-argument"));
        assertThat(text(result), containsString("retryable: no"));

        // an account tool names the list its uuids come from, not the instruments
        var cash = call("pp_get_cash_account", "{\"file\":\"sample\",\"uuid\":\"cash\"}");
        assertThat(text(cash), containsString("as pp_list_cash_accounts reports it"));
        var investment = call("pp_get_investment_account", "{\"file\":\"sample\",\"uuid\":\"depot\"}");
        assertThat(text(investment), containsString("as pp_list_investment_accounts reports it"));
    }

    /** a wrong type is refused here rather than surfacing as an endpoint validation error */
    @Test
    public void testAnArgumentOfTheWrongShapeIsRefusedWithTheDeclaredShape()
    {
        var result = call("pp_list_holdings", "{\"file\":\"sample\",\"date\":20260101}");

        assertThat(isError(result), is(true));
        assertThat(text(result), containsString("`date` must be `string`, not a number"));
    }

    /** an empty selection is a mistake; silently defaulting would answer another question */
    @Test
    public void testAnEmptySelectionIsRefusedRatherThanDefaulted()
    {
        var result = call("pp_list_trades", "{\"file\":\"sample\",\"status\":[]}");

        assertThat(isError(result), is(true));
        assertThat(text(result), containsString("`status` must hold at least 1 value"));
        assertThat(text(result), containsString("an empty list names nothing"));
    }

    /** a metric group sent as a bare string is the same mistake as an unknown value */
    @Test
    public void testAMetricsSentAsASingleValueIsRefusedRatherThanAccepted()
    {
        var result = call("pp_list_instrument_performance", "{\"file\":\"sample\",\"openingDate\":\"2024-01-01\",\"metrics\":\"risk\"}");

        assertThat(isError(result), is(true));
        assertThat(text(result), containsString("`metrics` must be `array`, not the string `risk`"));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /** a REST read through the same routing table, as parsed JSON */
    private JsonElement get(String path, Map<String, String> query) throws Exception
    {
        var match = router.match("GET", path);
        var response = match.handler().handle(new Request("GET", path, match.pathParams(), query, new byte[0]));
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8));
    }

    private JsonObject call(String tool, String arguments)
    {
        return resultOf("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool
                        + "\",\"arguments\":" + arguments + "}}");
    }

    private JsonObject resultOf(String body)
    {
        var request = new Request("POST", "/mcp", Map.of(), Map.of(), body.getBytes(StandardCharsets.UTF_8),
                        Request.Authorization.VALID, null);
        var response = McpEndpoint.handle(router, request);
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject()
                        .getAsJsonObject("result");
    }

    private static boolean isError(JsonObject result)
    {
        return result.has("isError") && result.get("isError").getAsBoolean();
    }

    /** the JSON a result carries after its guidance */
    private static JsonObject body(String text)
    {
        var start = text.startsWith("{") ? 0 : text.indexOf("\n\n{\"") + 2;
        return JsonParser.parseString(text.substring(start)).getAsJsonObject();
    }

    private static String text(JsonObject result)
    {
        return result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
    }
}
