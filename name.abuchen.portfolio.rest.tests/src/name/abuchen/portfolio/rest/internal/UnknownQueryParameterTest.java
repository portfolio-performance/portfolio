package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.RestApiServer;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/** Exercises strict query-parameter validation through the real routing table. */
@SuppressWarnings("nls")
public class UnknownQueryParameterTest
{
    private static final String PATH = "/tmp/x.portfolio";
    private static final String TOKEN = "test-token";

    private IEclipsePreferences node;
    private Router router;
    private String fileId;
    private RestApiServer server;
    private HttpClient http;

    @Before
    public void setUp()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(1000)).addTo(client);
        new PortfolioBuilder(account) //
                        .buy(security, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(PATH, true);
        fileId = registry.byPath(PATH).orElseThrow().uuid();

        var host = new FakeHost(List.of(new FakeHost.FakeOpenFile(PATH, "x", client)));
        router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
    }

    @After
    public void tearDown() throws Exception
    {
        if (server != null)
            server.stop();
        node.removeNode();
    }

    /** the reported case */
    @Test
    public void testUnknownParameterAloneIsRejected()
    {
        var problem = expectRejection("/v1/files/" + fileId + "/holdings", "currency=USD");

        assertThat(problem.getStatus(), is(400));
        assertThat(problem.getType(), is("invalid-request"));
        assertThat(problem.getErrors().size(), is(1));
        assertThat(problem.getErrors().get(0).field(), is("currency"));
        assertThat(problem.getErrors().get(0).code(), is("unknown-parameter"));
    }

    /** the message names what the endpoint does accept, so the client can fix it unaided */
    @Test
    public void testMessageNamesThePermittedParameters()
    {
        var message = expectRejection("/v1/files/" + fileId + "/holdings", "currency=USD").getErrors().get(0).message();

        assertThat(message, containsString("currency"));
        assertThat(message, containsString("date"));
        assertThat(message, containsString("reportingCurrency"));
    }

    /** the valid parameters alongside it are not itemized - only the offending one is */
    @Test
    public void testUnknownParameterAlongsideValidOnesIsRejected()
    {
        var problem = expectRejection("/v1/files/" + fileId + "/performance",
                        "openingDate=2023-01-01&closingDate=2024-12-31&reportingCurrency=USD&period=ytd");

        assertThat(problem.getStatus(), is(400));
        assertThat(problem.getErrors().size(), is(1));
        assertThat(problem.getErrors().get(0).field(), is("period"));
    }

    /** every unknown parameter is reported in one response */
    @Test
    public void testEveryUnknownParameterIsReportedAtOnce()
    {
        var problem = expectRejection("/v1/files/" + fileId + "/performance",
                        "openingDate=2023-01-01&period=ytd&currency=USD&taxesAndFees=excluded");

        assertThat(problem.getStatus(), is(400));
        assertThat(problem.getErrors().size(), is(3));

        // sorted, so the response does not depend on the query string's order
        assertThat(problem.getErrors().get(0).field(), is("currency"));
        assertThat(problem.getErrors().get(1).field(), is("period"));
        assertThat(problem.getErrors().get(2).field(), is("taxesAndFees"));

        // taxesAndFees is a real parameter - just not of this endpoint
        assertThat(problem.getErrors().get(2).code(), is("unknown-parameter"));
    }

    /** parameter names are case-sensitive */
    @Test
    public void testParameterNamesAreCaseSensitive()
    {
        var problem = expectRejection("/v1/files/" + fileId + "/holdings", "reportingcurrency=USD");

        assertThat(problem.getStatus(), is(400));
        assertThat(problem.getErrors().get(0).field(), is("reportingcurrency"));
        assertThat(problem.getErrors().get(0).code(), is("unknown-parameter"));
        assertThat(problem.getErrors().get(0).message(), containsString("case-sensitive"));
        assertThat(problem.getErrors().get(0).message(), containsString("reportingCurrency"));
    }

    /** and the correctly spelled name is still honoured */
    @Test
    public void testTheCorrectlySpelledNameIsAccepted() throws Exception
    {
        var response = get("/v1/files/" + fileId + "/holdings", "reportingCurrency=USD");
        assertThat(response.get("reportingCurrency").getAsString(), is("USD"));
    }

    /** a route that declares no parameters accepts none, and says so */
    @Test
    public void testRouteWithoutParametersAcceptsNone()
    {
        var problem = expectRejection("/v1/files/" + fileId + "/instruments", "limit=10");

        assertThat(problem.getStatus(), is(400));
        assertThat(problem.getErrors().get(0).field(), is("limit"));
        assertThat(problem.getErrors().get(0).message(), containsString("no query parameters"));
        assertThat(problem.getErrors().get(0).message(), not(containsString("accepts:")));
    }

    /**
     * The check runs before the handler resolves the {file} scope: the request
     * is malformed whether or not the file exists, and reporting the typo is
     * more actionable than a 404 the client cannot act on either.
     */
    @Test
    public void testMalformedRequestIsReportedBeforeAnUnknownFile()
    {
        var problem = expectRejection("/v1/files/no-such-file/holdings", "currency=USD");

        assertThat(problem.getStatus(), is(400));
        assertThat(problem.getErrors().get(0).field(), is("currency"));
    }

    /** a request without any query string is unaffected */
    @Test
    public void testNoQueryStringIsFine() throws Exception
    {
        assertThat(get("/v1/files/" + fileId + "/holdings", null).has("totalAssets"), is(true));
    }

    /** the reported request fails end to end over HTTP */
    @Test
    public void testTheReportedRequestFailsOverHttp() throws Exception
    {
        server = new RestApiServer(0, TOKEN::equals, router);
        server.start();
        http = HttpClient.newHttpClient();

        var uri = URI.create("http://127.0.0.1:" + server.getPort() + "/v1/files/" + fileId + "/holdings?currency=USD");
        var response = http.send(
                        HttpRequest.newBuilder(uri).header("Authorization", "Bearer " + TOKEN).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(400));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), is("application/problem+json"));

        var problem = JsonParser.parseString(response.body()).getAsJsonObject();
        assertThat(problem.get("type").getAsString(), containsString("invalid-request"));
        assertThat(problem.get("status").getAsInt(), is(400));

        var error = problem.get("errors").getAsJsonArray().get(0).getAsJsonObject();
        assertThat(error.get("field").getAsString(), is("currency"));
        assertThat(error.get("code").getAsString(), is("unknown-parameter"));
        assertThat(error.get("message").getAsString(), containsString("reportingCurrency"));

        // nothing of the report leaked out alongside the error
        assertThat(response.body(), not(containsString("totalAssets")));
    }

    private ApiException expectRejection(String path, String query)
    {
        try
        {
            get(path, query);
            throw new AssertionError("expected ApiException for " + path + "?" + query);
        }
        catch (ApiException e)
        {
            return e;
        }
        catch (Exception e)
        {
            throw new AssertionError("expected ApiException for " + path + "?" + query, e);
        }
    }

    /** drives the router with the query names a client actually sends */
    private JsonObject get(String path, String query) throws Exception
    {
        var match = router.match("GET", path);
        var request = new Request("GET", path, match.pathParams(), Request.parseQuery(query), new byte[0]);
        var response = match.handler().handle(request);
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
