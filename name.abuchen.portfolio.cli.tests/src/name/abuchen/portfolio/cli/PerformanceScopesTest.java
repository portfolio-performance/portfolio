package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

/**
 * <code>performance</code> at the scope of an investment account, a cash
 * account or an instrument, and per instrument. The expected figures come from
 * core's own performance index over the same client. Today is 2026-10-16 (see
 * {@link CliRun#CLOCK}).
 */
public class PerformanceScopesTest
{
    private static final double EPSILON = 1e-12;

    private static final String FROM = "2026-01-01";
    private static final String TO = "2026-09-30";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Client client;
    private Security alpha;
    private Security beta;
    private Security quiet;
    private Account cash;
    private Account cashTwo;
    private Portfolio depot;
    private Portfolio depotTwo;

    /**
     * Alpha is bought and partly sold in the first depot, Beta is bought in the
     * second depot, and Quiet was bought and sold before the period.
     */
    @Before
    public void setUp()
    {
        client = new Client();

        alpha = new SecurityBuilder().addTo(client);
        alpha.setName("Alpha");
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2025-12-31"), PortfolioBuilder.quoteOf(100)));
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-02-01"), PortfolioBuilder.quoteOf(120)));
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"), PortfolioBuilder.quoteOf(150)));
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-09-30"), PortfolioBuilder.quoteOf(130)));

        beta = new SecurityBuilder().addTo(client);
        beta.setName("Beta");
        beta.addPrice(new SecurityPrice(LocalDate.parse("2026-04-01"), PortfolioBuilder.quoteOf(50)));
        beta.addPrice(new SecurityPrice(LocalDate.parse("2026-09-30"), PortfolioBuilder.quoteOf(60)));

        quiet = new SecurityBuilder().addTo(client);
        quiet.setName("Quiet");
        quiet.addPrice(new SecurityPrice(LocalDate.parse("2025-03-01"), PortfolioBuilder.quoteOf(10)));
        quiet.addPrice(new SecurityPrice(LocalDate.parse("2025-06-01"), PortfolioBuilder.quoteOf(12)));
        quiet.addPrice(new SecurityPrice(LocalDate.parse("2026-09-30"), PortfolioBuilder.quoteOf(12)));

        cash = new AccountBuilder().deposit_("2025-01-01", PortfolioBuilder.amountOf(100_000))
                        .dividend("2026-02-15", PortfolioBuilder.amountOf(50), alpha).addTo(client);
        cash.setName("Cash");

        depot = new PortfolioBuilder(cash).buyPrice(alpha, "2026-01-02", 10, 100)
                        .buyPrice(alpha, "2026-02-01", 10, 120).sellPrice(alpha, "2026-03-01", 5, 150)
                        .buyPrice(quiet, "2025-03-01", 10, 10).sellPrice(quiet, "2025-06-01", 10, 12).addTo(client);
        depot.setName("Depot");

        cashTwo = new AccountBuilder().deposit_("2026-03-01", PortfolioBuilder.amountOf(10_000)).addTo(client);
        cashTwo.setName("Cash Two");

        depotTwo = new PortfolioBuilder(cashTwo).buyPrice(beta, "2026-04-01", 10, 50).addTo(client);
        depotTwo.setName("Depot Two");
    }

    private CliRun run(String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>(List.of("performance", "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.runOn("Linux", Map.of(), args.toArray(String[]::new));
    }

    private CliRun runOverTheYear(String... options) throws IOException
    {
        var args = new ArrayList<String>(List.of("--from", FROM, "--to", TO));
        args.addAll(List.of(options));
        return run(args.toArray(String[]::new));
    }

    private static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    private static JsonObject meta(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("meta");
    }

    private static Interval interval()
    {
        return Interval.of(LocalDate.parse(FROM).minusDays(1), LocalDate.parse(TO));
    }

    private CurrencyConverterImpl converter()
    {
        return new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), "EUR");
    }

    private static BigDecimal amount(long stored)
    {
        return BigDecimal.valueOf(stored, Values.Amount.precision());
    }

    private static void assertMatchesCore(CliRun run, PerformanceIndex index)
    {
        assertThat(run.stdout, run.exitCode, is(0));

        var data = data(run);
        var snapshot = index.getClientPerformanceSnapshot(true).orElseThrow();

        assertThat(data.get("irr").getAsDouble(), closeTo(snapshot.getPerformanceIRR(), EPSILON));
        assertThat(data.get("ttwror").getAsDouble(), closeTo(index.getFinalAccumulatedPercentage(), EPSILON));
        assertThat(data.get("absoluteGain").getAsBigDecimal()
                        .compareTo(amount(snapshot.getAbsoluteDelta().getAmount())), is(0));
        assertThat(data.get("finalValue").getAsBigDecimal().signum(), is(1));
    }

    @Test
    public void theClientScopeIsEchoedInMeta() throws IOException
    {
        var run = runOverTheYear();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).getAsJsonObject("scope").keySet(), is(Set.of("kind")));
        assertThat(meta(run).getAsJsonObject("scope").get("kind").getAsString(), is("client"));
    }

    @Test
    public void anInvestmentAccountsFiguresMatchCoresPerformanceOfThePortfolio() throws IOException
    {
        var run = runOverTheYear("--investment-account", depot.getUUID());

        assertMatchesCore(run, PerformanceIndex.        forPortfolio(client, converter(), depot, interval(), new ArrayList<>()));

        var scope = meta(run).getAsJsonObject("scope");
        assertThat(scope.get("kind").getAsString(), is("investment-account"));
        assertThat(scope.get("uuid").getAsString(), is(depot.getUUID()));
        assertThat(scope.get("name").getAsString(), is("Depot"));
        assertThat(scope.has("referenceCashAccount"), is(false));
    }

    @Test
    public void anInvestmentAccountIsResolvedByName() throws IOException
    {
        var byName = runOverTheYear("--investment-account", "Depot Two");
        var byUuid = runOverTheYear("--investment-account", depotTwo.getUUID());

        assertThat(data(byName), is(data(byUuid)));
    }

    @Test
    public void theReferenceCashAccountIsAddedOnRequest() throws IOException
    {
        var run = runOverTheYear("--investment-account", depot.getUUID(), "--with-reference-cash-account");

        var index = PerformanceIndex.forPortfolioPlusAccount(client, converter(), depot, interval(),
                        new ArrayList<>());
        assertMatchesCore(run, index);
        assertThat(PerformanceIndex.forPortfolio(client, converter(), depot, interval(), new ArrayList<>())
                        .getFinalAccumulatedPercentage() != index.getFinalAccumulatedPercentage(), is(true));

        var scope = meta(run).getAsJsonObject("scope");
        assertThat(scope.get("uuid").getAsString(), is(depot.getUUID()));
        assertThat(scope.getAsJsonObject("referenceCashAccount").get("uuid").getAsString(), is(cash.getUUID()));
        assertThat(scope.getAsJsonObject("referenceCashAccount").get("name").getAsString(), is("Cash"));
    }

    @Test
    public void anInvestmentAccountWithoutAReferenceCashAccountFailsWhenItIsAskedFor() throws IOException
    {
        var orphan = new Portfolio();
        orphan.setName("Orphan");
        client.addPortfolio(orphan);

        var run = runOverTheYear("--investment-account", "Orphan", "--with-reference-cash-account");

        assertThat(run.stdout, run.exitCode, is(1));
        var error = run.json().getAsJsonObject();
        assertThat(error.get("code").getAsString(), is("no-reference-cash-account"));
        assertThat(error.get("status").getAsInt(), is(422));
        assertThat(error.get("uuid").getAsString(), is(orphan.getUUID()));
    }

    @Test
    public void theReferenceCashAccountFlagWithoutAnInvestmentAccountIsAUsageError() throws IOException
    {
        var run = runOverTheYear("--with-reference-cash-account");

        assertThat(run.stdout, run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("option-required"));
    }

    @Test
    public void aCashAccountsFiguresMatchCoresPerformanceOfTheAccount() throws IOException
    {
        var run = runOverTheYear("--cash-account", "Cash Two");

        assertMatchesCore(run, PerformanceIndex.forAccount(client, converter(), cashTwo, interval(),
                        new ArrayList<>()));

        var scope = meta(run).getAsJsonObject("scope");
        assertThat(scope.get("kind").getAsString(), is("cash-account"));
        assertThat(scope.get("uuid").getAsString(), is(cashTwo.getUUID()));
        assertThat(scope.get("name").getAsString(), is("Cash Two"));
    }

    @Test
    public void anInstrumentsFiguresMatchCoresPerformanceOfTheInvestment() throws IOException
    {
        var run = runOverTheYear("--instrument", "Alpha");

        assertMatchesCore(run, PerformanceIndex.forInvestment(client, converter(), alpha, interval(),
                        new ArrayList<>()));

        var scope = meta(run).getAsJsonObject("scope");
        assertThat(scope.get("kind").getAsString(), is("instrument"));
        assertThat(scope.get("uuid").getAsString(), is(alpha.getUUID()));
        assertThat(scope.get("name").getAsString(), is("Alpha"));
    }

    @Test
    public void aQuietInstrumentWarnsInsteadOfFailingAndLeavesItsFiguresOut() throws IOException
    {
        var run = runOverTheYear("--instrument", quiet.getUUID());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(data(run).keySet(), is(Set.of("currencyCode")));
        assertThat(data(run).get("currencyCode").getAsString(), is("EUR"));
        assertThat(meta(run).getAsJsonObject("scope").get("kind").getAsString(), is("instrument"));

        var warnings = warnings(run, "no-activity-in-period");
        assertThat(warnings.size(), is(1));
        assertThat(warnings.get(0).get("uuid").getAsString(), is(quiet.getUUID()));
        assertThat(warnings.get(0).get("name").getAsString(), is("Quiet"));
    }

    @Test
    public void anInstrumentHeldThroughThePeriodIsNotQuiet() throws IOException
    {
        var before = run("--from", "2025-06-01", "--to", "2025-12-31", "--instrument", "Alpha");
        assertThat(before.stdout, before.exitCode, is(0));
        assertThat(warnings(before, "no-activity-in-period").size(), is(1));

        // no transaction in the period, but 15 shares are held at its start and end
        var held = run("--from", "2026-03-02", "--to", "2026-06-30", "--instrument", "Alpha");
        assertThat(held.stdout, held.exitCode, is(0));
        assertThat(warnings(held, "no-activity-in-period").size(), is(0));
        assertThat(data(held).get("finalValue").getAsBigDecimal().signum(), is(1));
    }

    @Test
    public void atMostOneScopeOptionIsAllowed() throws IOException
    {
        var options = List.of(List.of("--investment-account", "Depot"), List.of("--cash-account", "Cash"),
                        List.of("--instrument", "Alpha"));

        for (var first = 0; first < options.size(); first++)
        {
            for (var second = first + 1; second < options.size(); second++)
            {
                var args = new ArrayList<>(options.get(first));
                args.addAll(options.get(second));

                var run = runOverTheYear(args.toArray(String[]::new));

                assertThat(args + ": " + run.stdout, run.exitCode, is(2));
                assertThat(args.toString(), run.json().getAsJsonObject().get("code").getAsString(),
                                is("conflicting-options"));
            }
        }
    }

    private static List<JsonObject> warnings(CliRun run, String code)
    {
        var found = new ArrayList<JsonObject>();
        run.json().getAsJsonObject().getAsJsonArray("warnings").forEach(w -> {
            if (w.getAsJsonObject().get("code").getAsString().equals(code))
                found.add(w.getAsJsonObject());
        });
        return found;
    }

    private static List<JsonObject> items(CliRun run)
    {
        var found = new ArrayList<JsonObject>();
        data(run).getAsJsonArray("items").forEach(item -> found.add(item.getAsJsonObject()));
        return found;
    }

    private static JsonObject item(CliRun run, Security security)
    {
        return items(run).stream().filter(item -> item.get("uuid").getAsString().equals(security.getUUID()))
                        .findFirst().orElseThrow();
    }

    private static void assertSameAmount(String what, JsonObject item, String member, long stored)
    {
        assertThat(what + " " + member, item.get(member).getAsBigDecimal().compareTo(amount(stored)), is(0));
    }

    /** The per-instrument figures of core over the client as it is narrowed, with the FIFO cost method. */
    private void assertItemMatchesCore(JsonObject item, LazySecurityPerformanceRecord record)
    {
        var name = record.getSecurity().getName();

        assertThat(name, item.get("irr").getAsDouble(), closeTo(record.getIrr(), EPSILON));
        assertThat(name, item.get("ttwror").getAsDouble(),
                        closeTo(record.getTrueTimeWeightedRateOfReturn(), EPSILON));
        assertSameAmount(name, item, "finalValue", record.getMarketValue().getAmount());
        assertSameAmount(name, item, "dividends", record.getSumOfDividends().getAmount());
        assertSameAmount(name, item, "fees", record.getFees().getAmount());
        assertSameAmount(name, item, "taxes", record.getTaxes().getAmount());
        assertSameAmount(name, item, "absoluteGain", record.getDelta().getAmount());
        assertSameAmount(name, item, "realizedCapitalGains",
                        record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains().getAmount());
        assertSameAmount(name, item, "unrealizedCapitalGains",
                        record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains().getAmount());
    }

    private LazySecurityPerformanceSnapshot coreSnapshot(Client narrowed, Interval interval)
    {
        return LazySecurityPerformanceSnapshot.create(narrowed, converter(), interval);
    }

    @Test
    public void byInstrumentReportsOneRecordPerActiveInstrumentAsCoreDoes() throws IOException
    {
        var run = run("--from", "2026-03-02", "--to", TO, "--by", "instrument");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(data(run).keySet(), is(Set.of("currencyCode", "items")));
        assertThat(data(run).get("currencyCode").getAsString(), is("EUR"));
        assertThat(meta(run).getAsJsonObject("scope").get("kind").getAsString(), is("client"));

        var interval = Interval.of(LocalDate.parse("2026-03-01"), LocalDate.parse(TO));
        var core = coreSnapshot(client, interval);
        assertThat(core.getRecords().size(), is(2));
        assertThat(items(run).size(), is(2));
        for (var record : core.getRecords())
            assertItemMatchesCore(item(run, record.getSecurity()), record);

        // 15 shares of Alpha at 150 at the start, at 130 at the end
        var alphaItem = item(run, alpha);
        assertThat(alphaItem.get("name").getAsString(), is("Alpha"));
        assertThat(alphaItem.get("retired").getAsBoolean(), is(false));
        assertThat(alphaItem.get("initialValue").getAsBigDecimal().compareTo(new BigDecimal("2250")), is(0));
        assertThat(alphaItem.get("finalValue").getAsBigDecimal().compareTo(new BigDecimal("1950")), is(0));
        assertThat(alphaItem.get("absoluteGain").getAsBigDecimal().compareTo(new BigDecimal("-300")), is(0));
    }

    @Test
    public void byInstrumentReportsDividendsOfThePeriodAndOmitsQuietInstrumentsWithoutAWarning() throws IOException
    {
        var run = runOverTheYear("--by", "instrument");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(items(run).stream().map(item -> item.get("name").getAsString()).toList(),
                        is(List.of("Alpha", "Beta")));
        assertThat(item(run, alpha).get("dividends").getAsBigDecimal().compareTo(new BigDecimal("50")), is(0));
        assertThat(warnings(run, "no-activity-in-period").size(), is(0));
    }

    @Test
    public void byInstrumentIsSortedByNameIgnoringCaseThenUuid() throws IOException
    {
        var aardvark = new SecurityBuilder().addTo(client);
        aardvark.setName("aardvark");
        aardvark.addPrice(new SecurityPrice(LocalDate.parse("2026-09-30"), PortfolioBuilder.quoteOf(5)));
        new PortfolioBuilder(cash).buyPrice(aardvark, "2026-05-01", 1, 5).addTo(client).setName("Depot Three");

        var twinOne = new SecurityBuilder().addTo(client);
        var twinTwo = new SecurityBuilder().addTo(client);
        twinOne.setName("Twin");
        twinTwo.setName("twin");
        for (var twin : List.of(twinOne, twinTwo))
        {
            twin.addPrice(new SecurityPrice(LocalDate.parse("2026-09-30"), PortfolioBuilder.quoteOf(5)));
            new PortfolioBuilder(cash).buyPrice(twin, "2026-05-01", 1, 5).addTo(client).setName("Depot " + twin.getUUID());
        }

        var run = runOverTheYear("--by", "instrument");

        var names = items(run).stream().map(item -> item.get("name").getAsString()).toList();
        assertThat(names.subList(0, 3), is(List.of("aardvark", "Alpha", "Beta")));

        var twins = items(run).stream().filter(item -> item.get("name").getAsString().equalsIgnoreCase("twin"))
                        .map(item -> item.get("uuid").getAsString()).toList();
        assertThat(twins, is(twins.stream().sorted().toList()));
        assertThat(twins.size(), is(2));
    }

    @Test
    public void byInstrumentAtInvestmentAccountScopeRunsOnThatAccountsClient() throws IOException
    {
        var run = runOverTheYear("--investment-account", "Depot Two", "--by", "instrument");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).getAsJsonObject("scope").get("uuid").getAsString(), is(depotTwo.getUUID()));

        var narrowed = new PortfolioClientFilter(depotTwo).filter(client);
        var core = coreSnapshot(narrowed, interval());
        assertThat(items(run).size(), is(1));
        assertItemMatchesCore(item(run, beta), core.getRecord(beta).orElseThrow());
    }

    @Test
    public void byInstrumentAtInvestmentAccountScopeWithItsReferenceCashAccountIncludesItsDividends()
                    throws IOException
    {
        var with = runOverTheYear("--investment-account", "Depot", "--with-reference-cash-account", "--by",
                        "instrument");

        assertThat(with.stdout, with.exitCode, is(0));
        assertThat(items(with).size(), is(1));

        var narrowed = new PortfolioClientFilter(depot, cash).filter(client);
        assertItemMatchesCore(item(with, alpha), coreSnapshot(narrowed, interval()).getRecord(alpha).orElseThrow());
        assertThat(meta(with).getAsJsonObject("scope").has("referenceCashAccount"), is(true));
    }

    @Test
    public void byAnythingButInstrumentIsAUsageError() throws IOException
    {
        var run = runOverTheYear("--by", "account");

        assertThat(run.stdout, run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("invalid-option-value"));
    }

    @Test
    public void severalRecordsNeedingTheSameMissingRateDoNotMultiplyTheWarningCount() throws IOException
    {
        var first = addDollarInstrument("Aardvark");
        var one = runWithoutRates();
        var oneCount = pairCount(one, "USD", "EUR");
        assertThat(oneCount > 0, is(true));

        var second = addDollarInstrument("Zebra");
        var two = runWithoutRates();

        assertThat(item(two, first).has("absoluteGain"), is(false));
        assertThat(item(two, second).has("absoluteGain"), is(false));
        assertThat(pairCount(two, "USD", "EUR"), is(oneCount));
    }

    /** A dollar instrument held in a euro depot: its value needs a rate. */
    private Security addDollarInstrument(String name)
    {
        var dollar = new SecurityBuilder("USD").addTo(client);
        dollar.setName(name);
        dollar.addPrice(new SecurityPrice(LocalDate.parse("2026-09-30"), PortfolioBuilder.quoteOf(5)));
        new PortfolioBuilder(cash).buyPrice(dollar, "2026-05-01", 10, 5).addTo(client).setName("Depot " + name);
        return dollar;
    }

    private CliRun runWithoutRates() throws IOException
    {
        var missingCache = folder.getRoot().toPath().resolve("missing").resolve("ecb_exchange_rates.pb");
        return CliRun.runOn("Linux", Map.of("PP_ECB_RATES", missingCache.toString()), "performance", "--file",
                        ClientFiles.xml(folder.getRoot().toPath(), client).getPath(), "--from", FROM, "--to", TO,
                        "--by", "instrument");
    }

    private static int pairCount(CliRun run, String base, String term)
    {
        return warnings(run, "exchange-rate-missing").stream()
                        .filter(w -> w.get("baseCurrency").getAsString().equals(base)
                                        && w.get("termCurrency").getAsString().equals(term))
                        .mapToInt(w -> w.get("count").getAsInt()).sum();
    }

    @Test
    public void aRecordThatNeedsAMissingExchangeRateLosesOnlyItsOwnFigures() throws IOException
    {
        // sorts before Alpha, so that Alpha is read after a record whose rates were missing
        var dollar = addDollarInstrument("Aardvark");

        var run = runWithoutRates();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(items(run).stream().map(item -> item.get("name").getAsString()).toList(),
                        is(List.of("Aardvark", "Alpha", "Beta")));

        var lost = item(run, dollar);
        assertThat(lost.has("uuid") && lost.has("name") && lost.has("retired"), is(true));
        for (var figure : List.of("irr", "ttwror", "initialValue", "finalValue", "dividends", "fees", "taxes",
                        "absoluteGain", "realizedCapitalGains", "unrealizedCapitalGains"))
            assertThat(figure, lost.has(figure), is(false));
        assertThat(item(run, alpha).has("absoluteGain"), is(true));
        assertThat(item(run, alpha).has("ttwror"), is(true));
        assertThat(item(run, beta).has("absoluteGain"), is(true));

        var missing = warnings(run, "exchange-rate-missing");
        // the instrument costs euros and is valued in dollars: both directions are needed
        assertThat(missing.toString(), missing.isEmpty(), is(false));
        for (var warning : missing)
            assertThat(Set.of(warning.get("baseCurrency").getAsString(), warning.get("termCurrency").getAsString()),
                            is(Set.of("USD", "EUR")));
    }

    @Test
    public void byInstrumentWithoutAMissingRateReportsEveryRecordsFigures() throws IOException
    {
        var run = runOverTheYear("--by", "instrument");

        for (var item : items(run))
            assertThat(item.get("name").getAsString(), item.has("absoluteGain"), is(true));
        assertThat(run.json().getAsJsonObject().getAsJsonArray("warnings").size(), is(0));
    }

    @Test
    public void byWithACashAccountOrAnInstrumentIsAUsageError() throws IOException
    {
        for (var scope : List.of(List.of("--cash-account", "Cash"), List.of("--instrument", "Alpha")))
        {
            var args = new ArrayList<>(scope);
            args.addAll(List.of("--by", "instrument"));

            var run = runOverTheYear(args.toArray(String[]::new));

            assertThat(scope + ": " + run.stdout, run.exitCode, is(2));
            assertThat(scope.toString(), run.json().getAsJsonObject().get("code").getAsString(),
                            is("conflicting-options"));
        }
    }
}
