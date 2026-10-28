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

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
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

        var host = new FakeHost(List.of(new FakeHost.FakeOpenFile(FILE, "mcp", client)));
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

    /** the listing names the value to send as `file`, which is what stops a guess */
    @Test
    public void testTheFileListingNamesTheAddressToSend()
    {
        // always the id, even with an alias set: an alias can be renamed between
        // the listing and the next call
        var files = text(call("pp_list_files", "{}"));
        assertThat(files, containsString("\"use\":\"" + fileId + "\""));
        assertThat(files, containsString("\"alias\":\"sample\""));

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

    /** and an ordinary portfolio stays well below the cap */
    @Test
    public void testAnOrdinaryResultIsNotTruncated()
    {
        assertThat(text(call("pp_list_instruments", "{\"file\":\"sample\"}")), not(containsString("TRUNCATED")));
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
