package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.Assert;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * A client with EUR and USD cash accounts, two investment accounts, a EUR and
 * a USD instrument, and exchange rates USD/EUR 0.9 and EUR/USD 1.1 (no others),
 * plus helpers to call the transaction write handlers directly and through the
 * router.
 */
@SuppressWarnings("nls")
public class TransactionFixture
{
    public static final String PATH = "/tmp/tx.portfolio";
    public static final BigDecimal USD_EUR = new BigDecimal("0.9");
    public static final BigDecimal EUR_USD = new BigDecimal("1.1");

    public final Client client = new Client();
    public final Security eurSecurity;
    public final Security usdSecurity;
    public final Account cash;
    public final Account cash2;
    public final Account usdCash;
    public final Portfolio broker;
    public final Portfolio broker2;
    public final FakeHost.FakeOpenFile file;

    private IEclipsePreferences node;
    private FakeHost host;
    private String fileId;
    private Router router;

    public TransactionFixture()
    {
        eurSecurity = new SecurityBuilder().addTo(client);
        eurSecurity.setName("EUR Share");
        usdSecurity = new SecurityBuilder(CurrencyUnit.USD).addTo(client);
        usdSecurity.setName("USD Share");

        cash = new AccountBuilder().addTo(client);
        cash.setName("Cash");
        cash2 = new AccountBuilder().addTo(client);
        cash2.setName("Cash 2");
        usdCash = new AccountBuilder(CurrencyUnit.USD).addTo(client);
        usdCash.setName("USD Cash");

        broker = new PortfolioBuilder(cash).addTo(client);
        broker.setName("Broker");
        broker2 = new PortfolioBuilder(cash2).addTo(client);
        broker2.setName("Broker 2");

        file = new FakeHost.FakeOpenFile(PATH, "tx", client, new StubRates(client));
    }

    /** exchange rates for USD/EUR and EUR/USD only */
    private static final class StubRates extends ExchangeRateProviderFactory
    {
        StubRates(Client client)
        {
            super(client);
        }

        @Override
        public ExchangeRateTimeSeries getTimeSeries(String base, String term)
        {
            if (CurrencyUnit.USD.equals(base) && CurrencyUnit.EUR.equals(term))
                return series(base, term, USD_EUR);
            if (CurrencyUnit.EUR.equals(base) && CurrencyUnit.USD.equals(term))
                return series(base, term, EUR_USD);
            return series(base, term, null);
        }

        private static ExchangeRateTimeSeries series(String base, String term, BigDecimal rate)
        {
            return new ExchangeRateTimeSeries()
            {
                @Override
                public String getBaseCurrency()
                {
                    return base;
                }

                @Override
                public String getTermCurrency()
                {
                    return term;
                }

                @Override
                public Optional<ExchangeRateProvider> getProvider()
                {
                    return Optional.empty();
                }

                @Override
                public List<ExchangeRate> getRates()
                {
                    return rate == null ? List.of() : List.of(new ExchangeRate(LocalDate.of(2000, 1, 1), rate));
                }

                @Override
                public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
                {
                    return rate == null ? Optional.empty() : Optional.of(new ExchangeRate(requestedTime, rate));
                }

                @Override
                public int getWeight()
                {
                    return 1;
                }
            };
        }
    }

    public static JsonObject json(String json)
    {
        return JsonParser.parseString(json.replace('\'', '"')).getAsJsonObject();
    }

    public WriteContext context(boolean dryRun, String clientRef)
    {
        return new WriteContext(file, dryRun, clientRef);
    }

    /** creates through the handler, as a real run without clientRef */
    public JsonObject create(String body)
    {
        return TransactionsHandler.create(context(false, null), json(body)).entity();
    }

    public JsonObject patch(String uuid, String body)
    {
        return TransactionsHandler.patch(context(false, null), uuid, json(body)).entity();
    }

    /** the 422 of a create, failing if it succeeds */
    public ApiException createFails(String body)
    {
        try
        {
            TransactionsHandler.create(context(false, null), json(body));
            Assert.fail("expected ApiException for " + body);
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    /** the 422 of a patch, failing if it succeeds */
    public ApiException patchFails(String uuid, String body)
    {
        try
        {
            TransactionsHandler.patch(context(false, null), uuid, json(body));
            Assert.fail("expected ApiException for " + body);
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    /** asserts a 422 carrying the given field and code */
    public static void assertError(ApiException e, String field, String code)
    {
        assertThat(e.getStatus(), is(422));
        assertThat(e.getErrors().toString(),
                        e.getErrors().stream().anyMatch(err -> err.field().equals(field) && err.code().equals(code)),
                        is(true));
    }

    /** the transaction with the given UUID, of either leg */
    public TransactionPair<?> find(String uuid)
    {
        return TransactionsHandler.find(client, uuid);
    }

    /** the number of transaction legs in the client */
    public int legCount()
    {
        return client.getAccounts().stream().mapToInt(a -> a.getTransactions().size()).sum()
                        + client.getPortfolios().stream().mapToInt(p -> p.getTransactions().size()).sum();
    }

    public static long amount(String value)
    {
        return Amounts.toAmount(new BigDecimal(value));
    }

    public static Money eur(String value)
    {
        return Money.of(CurrencyUnit.EUR, amount(value));
    }

    public static Money usd(String value)
    {
        return Money.of(CurrencyUnit.USD, amount(value));
    }

    public static long shares(String value)
    {
        return Amounts.toShares(new BigDecimal(value));
    }

    public static Optional<Transaction.Unit> unit(Transaction transaction, Transaction.Unit.Type type, boolean forex)
    {
        return transaction.getUnits().filter(u -> u.getType() == type && (u.getForex() != null) == forex).findFirst();
    }

    // routing

    public FakeHost host()
    {
        ensureRouter();
        return host;
    }

    /**
     * the real routing table, serving this fixture's file; one instance per
     * fixture, like the server's, so that in-memory state such as the
     * idempotency keys of master data creates survives between calls
     */
    public Router router()
    {
        ensureRouter();
        if (router == null)
            router = ApiRoutes.create(new FileAccessRegistry(node), host,
                            new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
        return router;
    }

    /** the id under which the router serves this fixture's file */
    public String fileId()
    {
        ensureRouter();
        return fileId;
    }

    /** a call through the real router, as the HTTP server makes it */
    public Response call(String method, String path, Map<String, String> query, String body) throws Exception
    {
        var fullPath = "/v1/files/" + fileId() + path;
        var match = router().match(method, fullPath);
        return match.handler().handle(new Request(method, fullPath, match.pathParams(), query,
                        body == null ? new byte[0] : body.replace('\'', '"').getBytes(StandardCharsets.UTF_8)));
    }

    public static JsonObject body(Response response)
    {
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void ensureRouter()
    {
        if (node != null)
            return;

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(PATH, true);
        fileId = registry.byPath(PATH).orElseThrow().uuid();
        host = new FakeHost(List.of(file));
    }

    public void dispose() throws Exception
    {
        if (node != null)
            node.removeNode();
    }
}
