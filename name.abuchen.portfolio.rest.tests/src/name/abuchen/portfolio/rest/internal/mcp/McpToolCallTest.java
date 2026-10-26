package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
        assertThat(text(result), containsString("sample"));
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

        var unknownEntity = text(call("pp_get_instrument", "{\"file\":\"sample\",\"uuid\":\"no-such-uuid\"}"));
        assertThat(unknownEntity, containsString("The file is shared"));
        assertThat(unknownEntity, containsString("will not start working"));
    }

    /**
     * The prose tells a model what to do; the trailer states the same fact as
     * data. A 404 is never retryable — the same id will not start working — and
     * a locked server is, once the user has closed their dialog.
     */
    @Test
    public void testARefusalSaysWhetherTryingAgainCanHelp()
    {
        var notFound = text(call("pp_get_instrument", "{\"file\":\"sample\",\"uuid\":\"no-such-uuid\"}"));
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
        var files = text(call("pp_list_files", "{}"));
        assertThat(files, containsString("\"use\":\"sample\""));

        // and that value is what a tool accepts
        assertThat(isError(call("pp_list_instruments", "{\"file\":\"sample\"}")), is(false));
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
    public void testADeleteHasNoPayloadButStillSaysItIsNotSaved()
    {
        var result = call("pp_delete_instrument",
                        "{\"file\":\"sample\",\"uuid\":\"" + security.getUUID() + "\"}");

        assertThat(isError(result), is(false));
        assertThat(text(result), containsString("returned nothing to show"));
        assertThat(text(result), containsString("not saved to disk"));
        assertThat(client.getSecurities().size(), is(0));
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

    /** an unmarked truncation is read as a complete answer */
    @Test
    public void testAnOversizedResultIsTruncatedWithAMarker()
    {
        for (int ii = 0; ii < 200; ii++)
            new SecurityBuilder().addTo(client).setName("ACME " + ii + " " + "x".repeat(600));

        var result = call("pp_list_instruments", "{\"file\":\"sample\"}");

        assertThat(text(result).length(), is(McpEndpoint.MAX_RESULT_CHARS));
        assertThat(text(result), containsString("TRUNCATED"));
        assertThat(text(result), containsString("ask for a narrower question"));
    }

    /**
     * The advice is the part that tells a model to ask smaller, so it has to
     * survive the cut. Appending it after the cap put it past the limit where
     * nothing reads it, and the marker alone only says the answer was cut.
     */
    @Test
    public void testTheOversizeAdviceSurvivesTheCut()
    {
        for (int ii = 0; ii < 200; ii++)
            new SecurityBuilder().addTo(client).setName("ACME " + ii + " " + "x".repeat(600));

        var instruments = text(call("pp_list_instruments", "{\"file\":\"sample\"}"));

        assertThat(instruments.length(), is(McpEndpoint.MAX_RESULT_CHARS));
        assertThat(instruments, containsString("pp_get_instrument"));
        assertThat(instruments, containsString("ask the user which part of the portfolio they mean"));
    }

    /** the advice for a performance report names the groups that dominate its size */
    @Test
    public void testTheOversizeAdviceForAPerformanceReportNamesWhatToDrop()
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

        assertThat(result.length(), is(McpEndpoint.MAX_RESULT_CHARS));
        assertThat(result, containsString("TRUNCATED"));
        assertThat(result, containsString("pp_get_instrument_performance"));
        assertThat(result, containsString("timeWeighted"));
    }

    /** and an ordinary portfolio stays well below the cap */
    @Test
    public void testAnOrdinaryResultIsNotTruncated()
    {
        assertThat(text(call("pp_list_instruments", "{\"file\":\"sample\"}")), not(containsString("TRUNCATED")));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

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

    private static String text(JsonObject result)
    {
        return result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
    }
}
