package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;

/**
 * <code>transactions --currency</code>: the rate sources and their order, the
 * honest handling of chained, missing and stale rates, and the lookup of the
 * desktop app's ECB cache. Today is 2026-10-16 (see {@link CliRun#CLOCK}).
 */
public class TransactionsConversionTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Client client;
    private Account euro;
    private Account dollars;
    private Path cache;

    @Before
    public void setUp()
    {
        client = new Client();
        euro = new AccountBuilder("EUR").addTo(client);
        euro.setName("Euro");
        dollars = new AccountBuilder("USD").addTo(client);
        dollars.setName("Dollars");
        cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
    }

    private CliRun run(Map<String, String> env, String... options) throws IOException
    {
        return runOn("Linux", env, options);
    }

    private CliRun runOn(String osName, Map<String, String> env, String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>(List.of("transactions", "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.runOn(osName, env, args.toArray(String[]::new));
    }

    /** With the cache file of this test. */
    private CliRun run(String... options) throws IOException
    {
        return run(Map.of("PP_ECB_RATES", cache.toString()), options);
    }

    private static JsonArray items(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("items");
    }

    private static JsonObject converted(CliRun run, int index)
    {
        return items(run).get(index).getAsJsonObject().getAsJsonObject("converted");
    }

    private static JsonObject meta(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("meta");
    }

    private static List<JsonObject> warnings(CliRun run, String code)
    {
        var warnings = new ArrayList<JsonObject>();
        for (var warning : run.json().getAsJsonObject().getAsJsonArray("warnings"))
        {
            if (warning.getAsJsonObject().get("code").getAsString().equals(code))
                warnings.add(warning.getAsJsonObject());
        }
        return warnings;
    }

    private static String decimal(JsonObject json, String member)
    {
        return json.get(member).getAsBigDecimal().toPlainString();
    }

    private static String text(JsonObject json, String member)
    {
        return json.get(member).getAsString();
    }

    private static AccountTransaction deposit(Account account, String date, long amount)
    {
        return TransactionFixtures.cash(account, AccountTransaction.Type.DEPOSIT, date, amount);
    }

    /** What core itself computes over the same rates. */
    private static CurrencyConverter core(String term, EcbCacheFiles.Series... series)
    {
        return CoreRates.converter(term, series);
    }

    @Test
    public void theSameCurrencyIsTheIdentityAndNeverReadsTheCache() throws IOException
    {
        deposit(euro, "2026-03-02", 100_500);

        var run = run("--currency", "EUR");

        assertThat(run.stdout, run.exitCode, is(0));
        var converted = converted(run, 0);
        assertThat(text(converted, "currencyCode"), is("EUR"));
        assertThat(decimal(converted, "amount"), is("1005.00"));
        assertThat(decimal(converted, "rate"), is("1"));
        assertThat(text(converted, "rateDate"), is("2026-03-02"));
        assertThat(text(converted, "rateSource"), is("identity"));
        assertThat(text(meta(run), "currency"), is("EUR"));

        // the cache file named by PP_ECB_RATES doesn't exist, and nothing says so
        assertThat(run.json().getAsJsonObject().getAsJsonArray("warnings").size(), is(0));
        assertThat(meta(run).has("exchangeRates"), is(false));
    }

    @Test
    public void aForexUnitOfTheRecordGivesItsInverseRateRoundedLikeCore() throws IOException
    {
        var usd = new SecurityBuilder("USD").addTo(client);
        var depot = new PortfolioBuilder(euro).addTo(client);
        var entry = TransactionFixtures.buy(depot, euro, usd, "2026-03-02T09:30", 10_00000000L, 100_500);
        entry.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of("EUR", 100_000), Money.of("USD", 108_500), new BigDecimal("0.9216589862")));

        var run = run("--currency", "USD");

        assertThat(run.stdout, run.exitCode, is(0));
        var converted = converted(run, 0);
        var inverse = new ExchangeRate(LocalDate.parse("2026-03-02"), new BigDecimal("0.9216589862")).inverse()
                        .getValue();
        assertThat(decimal(converted, "rate"), is(inverse.toPlainString()));
        assertThat(decimal(converted, "rate"), is("1.0850000000"));
        // 1005.00 * 1.085 = 1090.425, rounded half down in minor units
        assertThat(decimal(converted, "amount"), is("1090.42"));
        assertThat(text(converted, "currencyCode"), is("USD"));
        assertThat(text(converted, "rateDate"), is("2026-03-02"));
        assertThat(text(converted, "rateSource"), is("transaction"));
        assertThat(meta(run).has("exchangeRates"), is(false));
        assertThat(run.json().getAsJsonObject().getAsJsonArray("warnings").size(), is(0));
    }

    @Test
    public void aCrossCurrencyTransferGivesTheInverseOfItsRate() throws IOException
    {
        TransactionFixtures.crossCurrencyTransfer(euro, dollars, "2026-02-01", 100_000, 108_500, "0.9216589862");

        var run = run("--currency", "USD");

        assertThat(run.stdout, run.exitCode, is(0));
        var converted = converted(run, 0);
        assertThat(decimal(converted, "rate"), is("1.0850000000"));
        assertThat(decimal(converted, "amount"), is("1085.00"));
        assertThat(text(converted, "rateSource"), is("transaction"));
        assertThat(text(converted, "rateDate"), is("2026-02-01"));
    }

    @Test
    public void ecbRatesConvertLikeCoreAndNameTheCacheInMeta() throws IOException
    {
        var usd = EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.0850").rate("2026-10-10", "1.0900")
                        .rate("2026-10-14", "1.1000");
        EcbCacheFiles.write(cache, usd);
        deposit(euro, "2026-10-12", 100_000);

        var run = run("--currency", "USD");

        assertThat(run.stdout, run.exitCode, is(0));
        var converted = converted(run, 0);
        assertThat(decimal(converted, "rate"), is("1.0900"));
        assertThat(decimal(converted, "amount"), is("1090.00"));
        assertThat(text(converted, "rateDate"), is("2026-10-10"));
        assertThat(text(converted, "rateSource"), is("ecb"));

        var core = core("USD", usd);
        var date = LocalDate.parse("2026-10-12");
        assertThat(new BigDecimal(decimal(converted, "rate")).compareTo(core.getRate(date, "EUR").getValue()), is(0));
        assertThat(new BigDecimal(decimal(converted, "amount")).movePointRight(2).longValue(),
                        is(core.convert(date, Money.of("EUR", 100_000)).getAmount()));

        var exchangeRates = meta(run).getAsJsonObject("exchangeRates");
        assertThat(text(exchangeRates, "path"), is(cache.toString()));
        assertThat(text(exchangeRates, "latestRateDate"), is("2026-10-14"));
        assertThat(run.json().getAsJsonObject().getAsJsonArray("warnings").size(), is(0));
    }

    @Test
    public void aChainedRateUsesTheOldestStepDateAndMatchesCoreWhereEveryStepHasARate() throws IOException
    {
        var usd = EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.0850").rate("2026-10-10", "1.0900");
        var chf = EcbCacheFiles.series("EUR", "CHF").rate("2026-10-05", "0.9400");
        EcbCacheFiles.write(cache, usd, chf);
        deposit(dollars, "2026-10-12", 100_000);

        var run = run("--currency", "CHF");

        assertThat(run.stdout, run.exitCode, is(0));
        var converted = converted(run, 0);
        assertThat(text(converted, "rateSource"), is("ecb"));
        // USD -> EUR on 10-10, EUR -> CHF on 10-05: the oldest step counts
        assertThat(text(converted, "rateDate"), is("2026-10-05"));

        var core = core("CHF", usd, chf);
        var date = LocalDate.parse("2026-10-12");
        assertThat(new BigDecimal(decimal(converted, "rate")).compareTo(core.getRate(date, "USD").getValue()), is(0));
        assertThat(new BigDecimal(decimal(converted, "amount")).movePointRight(2).longValue(),
                        is(core.convert(date, Money.of("USD", 100_000)).getAmount()));

        // exactly 7 days before the day needed is not stale yet
        assertThat(warnings(run, "stale-exchange-rate"), hasSize(0));
    }

    @Test
    public void anExchangeRateInstrumentIsPreferredAndIsNotCalledEcb() throws IOException
    {
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.5000"));
        var rate = new SecurityBuilder("USD").addPrice("2026-10-11", 92_000_000L).addTo(client);
        rate.setName("USD/EUR");
        rate.setTargetCurrencyCode("EUR");
        deposit(dollars, "2026-10-12", 10_000);

        var run = run("--currency", "EUR");

        assertThat(run.stdout, run.exitCode, is(0));
        var converted = converted(run, 0);
        assertThat(decimal(converted, "rate"), is("0.92"));
        assertThat(decimal(converted, "amount"), is("92.00"));
        assertThat(text(converted, "rateDate"), is("2026-10-11"));
        assertThat(text(converted, "rateSource"), is("instrument"));
    }

    @Test
    public void aMissingRateLeavesConvertedOutAndWarnsOncePerPairWithACount() throws IOException
    {
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.0850"));
        var beforeFirst = deposit(euro, "2026-09-30", 100);
        var alsoBefore = deposit(euro, "2026-09-29", 200);
        var covered = deposit(euro, "2026-10-12", 300);
        var noPath = deposit(dollars, "2026-10-12", 400);

        var run = run("--currency", "CHF");
        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(warnings(run, "exchange-rate-missing"), hasSize(2));

        var usdRun = run("--currency", "USD");
        assertThat(usdRun.stdout, usdRun.exitCode, is(0));

        var byId = new HashMap<String, JsonObject>();
        items(usdRun).forEach(i -> byId.put(text(i.getAsJsonObject(), "id"), i.getAsJsonObject()));
        assertThat(byId.get(beforeFirst.getUUID()).has("converted"), is(false));
        assertThat(byId.get(alsoBefore.getUUID()).has("converted"), is(false));
        assertThat(byId.get(covered.getUUID()).has("converted"), is(true));
        assertThat(text(byId.get(noPath.getUUID()).getAsJsonObject("converted"), "rateSource"), is("identity"));

        var missing = warnings(usdRun, "exchange-rate-missing");
        assertThat(missing, hasSize(1));
        assertThat(text(missing.get(0), "baseCurrency"), is("EUR"));
        assertThat(text(missing.get(0), "termCurrency"), is("USD"));
        assertThat(missing.get(0).get("count").getAsInt(), is(2));
        assertThat(missing.get(0).has("detail"), is(true));
    }

    @Test
    public void aStaleRateIsUsedWithOneWarningPerPairWithCountAndMaxAge() throws IOException
    {
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.0850"));
        deposit(euro, "2026-10-12", 100_000);
        deposit(euro, "2026-10-10", 100_000);
        deposit(euro, "2026-10-08", 100_000);

        var run = run("--currency", "USD");

        assertThat(run.stdout, run.exitCode, is(0));
        for (var i = 0; i < 3; i++)
            assertThat(text(converted(run, i), "rateDate"), is("2026-10-01"));

        var stale = warnings(run, "stale-exchange-rate");
        assertThat(stale, hasSize(1));
        assertThat(text(stale.get(0), "baseCurrency"), is("EUR"));
        assertThat(text(stale.get(0), "termCurrency"), is("USD"));
        // 10-08 is exactly 7 days after 10-01 and not stale
        assertThat(stale.get(0).get("count").getAsInt(), is(2));
        assertThat(stale.get(0).get("maxAgeDays").getAsInt(), is(11));
        assertThat(warnings(run, "exchange-rate-missing"), hasSize(0));
    }

    @Test
    public void onlyTheRecordsOfTheReturnedPageAreConvertedAndWarnedAbout() throws IOException
    {
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.0850"));
        deposit(euro, "2026-01-01", 100);
        deposit(euro, "2026-10-12", 200);

        var run = run("--currency", "USD", "--limit", "1", "--offset", "1");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(warnings(run, "stale-exchange-rate").get(0).get("count").getAsInt(), is(1));
    }

    @Test
    public void anUnavailableCacheWarnsOnceAndNeverFailsTheCommandOrFallsBackToBuiltInRates() throws IOException
    {
        deposit(euro, "2026-10-12", 100_000);
        deposit(euro, "2026-10-13", 100_000);

        var garbage = folder.newFile("garbage.pb").toPath();
        Files.write(garbage, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        var directory = folder.newFolder("directory").toPath();

        var cases = new LinkedHashMap<String, Map<String, String>>();
        cases.put("not-found", Map.of("PP_ECB_RATES", cache.toString()));
        cases.put("unparsable", Map.of("PP_ECB_RATES", garbage.toString()));
        cases.put("unreadable", Map.of("PP_ECB_RATES", directory.toString()));
        cases.put("not-configured", Map.of());

        for (var entry : cases.entrySet())
        {
            var run = run(entry.getValue(), "--currency", "USD");

            assertThat(entry.getKey() + ": " + run.stdout, run.exitCode, is(0));
            for (var i = 0; i < 2; i++)
                assertThat(entry.getKey(), items(run).get(i).getAsJsonObject().has("converted"), is(false));

            var unavailable = warnings(run, "exchange-rate-cache-unavailable");
            assertThat(entry.getKey(), unavailable, hasSize(1));
            assertThat(text(unavailable.get(0), "reason"), is(entry.getKey()));
            assertThat(unavailable.get(0).has("detail"), is(true));
            assertThat(entry.getKey(), unavailable.get(0).has("path"), is(!entry.getKey().equals("not-configured")));

            var missing = warnings(run, "exchange-rate-missing");
            assertThat(entry.getKey(), missing, hasSize(1));
            assertThat(missing.get(0).get("count").getAsInt(), is(2));
            assertThat(entry.getKey(), meta(run).has("exchangeRates"), is(false));
        }
    }

    @Test
    public void exchangeRateInstrumentsStillWorkWithoutTheCache() throws IOException
    {
        var rate = new SecurityBuilder("USD").addPrice("2026-10-11", 92_000_000L).addTo(client);
        rate.setTargetCurrencyCode("EUR");
        deposit(dollars, "2026-10-12", 10_000);

        var run = run(Map.of(), "--currency", "EUR");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(text(converted(run, 0), "rateSource"), is("instrument"));
        assertThat(warnings(run, "exchange-rate-cache-unavailable"), hasSize(1));
    }

    @Test
    public void theDefaultLocationIsBuiltFromTheInjectedEnvironmentAndTheOverrideWins() throws IOException
    {
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.0850"));
        deposit(euro, "2026-10-12", 100_000);

        var home = folder.newFolder("home").toPath();
        var defaultFile = defaultCacheFile(home.resolve(".PortfolioPerformance").resolve("workspace"));
        EcbCacheFiles.write(defaultFile, EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "2.0000"));

        var viaDefault = run(Map.of("HOME", home.toString()), "--currency", "USD");
        assertThat(viaDefault.stdout, viaDefault.exitCode, is(0));
        assertThat(decimal(converted(viaDefault, 0), "rate"), is("2.0000"));
        assertThat(text(meta(viaDefault).getAsJsonObject("exchangeRates"), "path"), is(defaultFile.toString()));

        var overridden = run(Map.of("HOME", home.toString(), "PP_ECB_RATES", cache.toString()), "--currency", "USD");
        assertThat(decimal(converted(overridden, 0), "rate"), is("1.0850"));
        assertThat(text(meta(overridden).getAsJsonObject("exchangeRates"), "path"), is(cache.toString()));

        var blankOverride = run(Map.of("HOME", home.toString(), "PP_ECB_RATES", "  "), "--currency", "USD");
        assertThat(decimal(converted(blankOverride, 0), "rate"), is("2.0000"));
    }

    @Test
    public void theDefaultWorkspaceOfEachOperatingSystem() throws IOException
    {
        deposit(euro, "2026-10-12", 100_000);

        var home = folder.newFolder("home").toPath();
        var workspaces = Map.of( //
                        "Windows 11", Map.entry("LOCALAPPDATA", home.resolve("PortfolioPerformance").resolve("workspace")),
                        "Mac OS X", Map.entry("HOME", home.resolve("Library").resolve("Application Support")
                                        .resolve("name.abuchen.portfolio.product").resolve("workspace")),
                        "Linux", Map.entry("HOME", home.resolve(".PortfolioPerformance").resolve("workspace")));

        workspaces.forEach((osName, location) -> {
            try
            {
                var file = defaultCacheFile(location.getValue());
                EcbCacheFiles.write(file, EcbCacheFiles.series("EUR", "USD").rate("2026-10-01", "1.0850"));

                var run = runOn(osName, Map.of(location.getKey(), home.toString()), "--currency", "USD");

                assertThat(osName + " " + run.stdout, run.exitCode, is(0));
                assertThat(osName, text(meta(run).getAsJsonObject("exchangeRates"), "path"), is(file.toString()));
                Files.delete(file);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static Path defaultCacheFile(Path workspace)
    {
        return workspace.resolve(".metadata").resolve(".plugins").resolve("name.abuchen.portfolio")
                        .resolve("ecb_exchange_rates.pb");
    }

    @Test
    public void anUnknownCurrencyIsAUsageErrorBeforeAnythingIsRead() throws IOException
    {
        var run = run("--currency", "NOPE");

        assertThat(run.exitCode, is(2));
        var problem = run.json().getAsJsonObject();
        assertThat(text(problem, "code"), is("invalid-option-value"));
        assertThat(text(problem, "option"), is("currency"));
        assertThat(text(problem, "value"), is("NOPE"));
    }

    @Test
    public void coreFixedPairsConvertAndAreNamedAsFixed() throws IOException
    {
        var pound = new AccountBuilder("GBP").addTo(client);
        TransactionFixtures.cash(pound, AccountTransaction.Type.DEPOSIT, "2026-10-12", 100_00);

        var run = run("--currency", "GBX");

        assertThat(run.stdout, run.exitCode, is(0));
        var converted = converted(run, 0);
        assertThat(text(converted, "rateSource"), is("fixed"));
        assertThat(decimal(converted, "amount"), is("10000.00"));
        assertThat(text(converted, "rateDate"), is("2026-10-12"));
    }
}
