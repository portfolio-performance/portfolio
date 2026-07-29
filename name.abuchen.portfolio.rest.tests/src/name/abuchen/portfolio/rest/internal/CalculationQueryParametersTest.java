package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * The query parameters of the calculation endpoints, exercised through the real
 * routing table rather than by calling a handler directly.
 * <p/>
 * Every other test in this fragment hands the handlers their arguments
 * positionally, which says nothing about the name a client has to put in the
 * query string. Without this, renaming a parameter in {@code ApiRoutes} alone -
 * or forgetting to - leaves the whole suite green while the endpoint silently
 * ignores what the specification tells clients to send.
 */
@SuppressWarnings("nls")
public class CalculationQueryParametersTest
{
    private static final String PATH = "/tmp/x.portfolio";

    private IEclipsePreferences node;
    private Router router;
    private Client client;
    private Security security;
    private String fileId;

    @Before
    public void setUp()
    {
        client = new Client();

        security = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addPrice("2024-12-31", Values.Quote.factorize(120)) //
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
        node.removeNode();
    }

    /**
     * Request and response call the reporting currency by the same name, on
     * every endpoint that has one, so a client can send back what it read.
     */
    @Test
    public void testReportingCurrencyIsHonouredOnEveryCalculationRoute() throws Exception
    {
        for (String path : calculationRoutes())
            assertThat("on " + path, get(path, Map.of("openingDate", "2024-01-01", "closingDate", "2024-12-31",
                            "date", "2024-12-31", "reportingCurrency", "USD")).get("reportingCurrency").getAsString(),
                            is("USD"));
    }

    /**
     * And the name the parameter used to have is no longer one: an unknown
     * query parameter is ignored, so a client still sending `?currency=` would
     * quietly get the base currency instead of an error. Pinning it here is the
     * only thing that keeps the rename honest.
     */
    @Test
    public void testTheFormerParameterNameNoLongerSelectsTheCurrency() throws Exception
    {
        for (String path : calculationRoutes())
            assertThat("on " + path, get(path, Map.of("openingDate", "2024-01-01", "closingDate", "2024-12-31",
                            "date", "2024-12-31", "currency", "USD")).get("reportingCurrency").getAsString(),
                            is("EUR"));
    }

    /** the field error names the parameter the client actually sent */
    @Test
    public void testUnknownCurrencyErrorNamesTheParameter()
    {
        for (String path : calculationRoutes())
        {
            try
            {
                get(path, Map.of("openingDate", "2024-01-01", "closingDate", "2024-12-31", "date", "2024-12-31",
                                "reportingCurrency", "ZZZ"));
                throw new AssertionError("expected ApiException on " + path);
            }
            catch (Exception e)
            {
                if (!(e instanceof ApiException problem))
                    throw new AssertionError("expected ApiException on " + path, e);

                assertThat("on " + path, problem.getStatus(), is(400));
                assertThat("on " + path, problem.getErrors().get(0).field(), is("reportingCurrency"));
                assertThat("on " + path, problem.getErrors().get(0).code(), is("unknown-currency"));
            }
        }
    }

    /** every route that converts a whole report into a reporting currency */
    private List<String> calculationRoutes()
    {
        return List.of("/v1/files/" + fileId + "/holdings", //
                        "/v1/files/" + fileId + "/performance", //
                        "/v1/files/" + fileId + "/performance/instruments", //
                        "/v1/files/" + fileId + "/performance/instruments/" + security.getUUID());
    }

    private JsonObject get(String path, Map<String, String> query) throws Exception
    {
        var match = router.match("GET", path);
        var response = match.handler().handle(new Request("GET", path, match.pathParams(), query, new byte[0]));
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
