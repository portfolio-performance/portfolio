package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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

/** Exercises calculation query parameters through the real routing table. */
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
        for (var route : calculationRoutes().entrySet())
            assertThat("on " + route.getKey(),
                            get(route.getKey(), with(route.getValue(), "reportingCurrency", "USD"))
                                            .get("reportingCurrency").getAsString(),
                            is("USD"));
    }

    /** the former {@code currency} parameter name is rejected on every calculation route */
    @Test
    public void testTheFormerParameterNameIsRejected()
    {
        for (var route : calculationRoutes().entrySet())
        {
            try
            {
                get(route.getKey(), with(route.getValue(), "currency", "USD"));
                throw new AssertionError("expected ApiException on " + route.getKey());
            }
            catch (Exception e)
            {
                if (!(e instanceof ApiException problem))
                    throw new AssertionError("expected ApiException on " + route.getKey(), e);

                assertThat("on " + route.getKey(), problem.getStatus(), is(400));
                assertThat("on " + route.getKey(), problem.getErrors().get(0).field(), is("currency"));
                assertThat("on " + route.getKey(), problem.getErrors().get(0).code(), is("unknown-parameter"));
            }
        }
    }

    /** the field error names the parameter the client actually sent */
    @Test
    public void testUnknownCurrencyErrorNamesTheParameter()
    {
        for (var route : calculationRoutes().entrySet())
        {
            try
            {
                get(route.getKey(), with(route.getValue(), "reportingCurrency", "ZZZ"));
                throw new AssertionError("expected ApiException on " + route.getKey());
            }
            catch (Exception e)
            {
                if (!(e instanceof ApiException problem))
                    throw new AssertionError("expected ApiException on " + route.getKey(), e);

                assertThat("on " + route.getKey(), problem.getStatus(), is(400));
                assertThat("on " + route.getKey(), problem.getErrors().get(0).field(), is("reportingCurrency"));
                assertThat("on " + route.getKey(), problem.getErrors().get(0).code(), is("unknown-currency"));
            }
        }
    }

    /** every route that converts a whole report into a reporting currency */
    private Map<String, Map<String, String>> calculationRoutes()
    {
        var interval = Map.of("openingDate", "2024-01-01", "closingDate", "2024-12-31");

        var routes = new LinkedHashMap<String, Map<String, String>>();
        routes.put("/v1/files/" + fileId + "/holdings", Map.of("date", "2024-12-31"));
        routes.put("/v1/files/" + fileId + "/performance", interval);
        routes.put("/v1/files/" + fileId + "/performance/instruments", interval);
        routes.put("/v1/files/" + fileId + "/performance/instruments/" + security.getUUID(), interval);
        return routes;
    }

    private static Map<String, String> with(Map<String, String> query, String name, String value)
    {
        var result = new LinkedHashMap<>(query);
        result.put(name, value);
        return result;
    }

    private JsonObject get(String path, Map<String, String> query) throws Exception
    {
        var match = router.match("GET", path);
        var response = match.handler().handle(new Request("GET", path, match.pathParams(), query, new byte[0]));
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
