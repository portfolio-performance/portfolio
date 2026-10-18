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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.util.Interval;

/**
 * <code>performance</code> for the client. The expected figures come from
 * core's own performance index and snapshot over the same client. Today is
 * 2026-10-16 (see {@link CliRun#CLOCK}).
 */
public class PerformanceCommandTest
{
    private static final double EPSILON = 1e-12;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Client client;

    /**
     * Two purchases at different prices and a partial sale, so that FIFO and
     * moving average disagree, plus a dividend.
     */
    @Before
    public void setUp()
    {
        client = new Client();

        var alpha = new SecurityBuilder().addTo(client);
        alpha.setName("Alpha");
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2025-12-31"),
                        PortfolioBuilder.quoteOf(100)));
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-02-01"),
                        PortfolioBuilder.quoteOf(120)));
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"),
                        PortfolioBuilder.quoteOf(150)));
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-09-30"),
                        PortfolioBuilder.quoteOf(130)));

        var cash = new AccountBuilder().deposit_("2025-12-01", PortfolioBuilder.amountOf(100_000))
                        .dividend("2026-02-15", PortfolioBuilder.amountOf(50), alpha).addTo(client);
        cash.setName("Cash");

        new PortfolioBuilder(cash).buyPrice(alpha, "2026-01-02", 10, 100).buyPrice(alpha, "2026-02-01", 10, 120)
                        .sellPrice(alpha, "2026-03-01", 5, 150).addTo(client).setName("Depot");
    }

    private CliRun run(String... options) throws IOException
    {
        return run(Map.of(), options);
    }

    private CliRun run(Map<String, String> env, String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>(List.of("performance", "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.runOn("Linux", env, args.toArray(String[]::new));
    }

    private static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    private static JsonObject meta(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("meta");
    }

    /**
     * Core's performance index over the days from the first to the last day,
     * both counted. Core's interval is open at its start, so it begins on the
     * day before the first day.
     */
    private PerformanceIndex coreOverDays(String firstDay, String lastDay)
    {
        return core(Interval.of(LocalDate.parse(firstDay).minusDays(1), LocalDate.parse(lastDay)));
    }

    private PerformanceIndex core(Interval interval)
    {
        return PerformanceIndex.forClient(client,
                        new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), "EUR"), interval,
                        new ArrayList<>());
    }

    private static BigDecimal amount(long stored)
    {
        return BigDecimal.valueOf(stored, Values.Amount.precision());
    }

    private static void assertMatchesCore(CliRun run, PerformanceIndex index, boolean fifo)
    {
        assertThat(run.stdout, run.exitCode, is(0));

        var data = data(run);
        var snapshot = index.getClientPerformanceSnapshot(fifo).orElseThrow();

        assertThat(data.get("irr").getAsDouble(), closeTo(snapshot.getPerformanceIRR(), EPSILON));
        assertThat(data.get("ttwror").getAsDouble(), closeTo(index.getFinalAccumulatedPercentage(), EPSILON));
        assertThat(data.get("absoluteGain").getAsBigDecimal().compareTo(amount(snapshot.getAbsoluteDelta().getAmount())),
                        is(0));
        assertThat(data.get("capitalGains").getAsBigDecimal()
                        .compareTo(amount(snapshot.getValue(CategoryType.CAPITAL_GAINS).getAmount())), is(0));
        assertThat(data.get("realizedCapitalGains").getAsBigDecimal()
                        .compareTo(amount(snapshot.getValue(CategoryType.REALIZED_CAPITAL_GAINS).getAmount())),
                        is(0));
    }

    @Test
    public void irrTtwrorAndGainsMatchCoresPerformanceIndexForFromAndTo() throws IOException
    {
        var run = run("--from", "2026-01-01", "--to", "2026-09-30");

        assertMatchesCore(run, coreOverDays("2026-01-01", "2026-09-30"), true);
    }

    @Test
    public void fromIsTheFirstDayOfThePeriodSoActivityOnItCounts() throws IOException
    {
        // the first purchase is on 2026-01-02: it is the opening of the period, not a part of the value before it
        var run = run("--from", "2026-01-02", "--to", "2026-09-30");

        assertMatchesCore(run, coreOverDays("2026-01-02", "2026-09-30"), true);
        assertThat(data(run).get("initialValue").getAsBigDecimal().signum(), is(1));
        assertThat(meta(run).get("from").getAsString(), is("2026-01-02"));
    }

    @Test
    public void aSingleDayPeriodIsValid() throws IOException
    {
        var run = run("--from", "2026-09-30", "--to", "2026-09-30");

        assertMatchesCore(run, coreOverDays("2026-09-30", "2026-09-30"), true);
        assertThat(meta(run).get("from").getAsString(), is("2026-09-30"));
        assertThat(meta(run).get("to").getAsString(), is("2026-09-30"));
    }

    @Test
    public void toDefaultsToTodayFromTheClockAndTheResolvedDatesAreEchoed() throws IOException
    {
        var run = run("--from", "2026-01-01");

        assertMatchesCore(run, coreOverDays("2026-01-01", "2026-10-16"), true);
        assertThat(meta(run).get("from").getAsString(), is("2026-01-01"));
        assertThat(meta(run).get("to").getAsString(), is("2026-10-16"));
        assertThat(meta(run).has("period"), is(false));
    }

    @Test
    public void periodCodesResolveThroughCoreAndTheResolvedDatesAreEchoedAsFirstAndLastDay() throws IOException
    {
        var today = LocalDate.parse("2026-10-16");

        for (var code : List.of("L1Y0", "L0Y6", "Y2025", "S2026-01-15"))
        {
            var interval = ReportingPeriod.from(code).toInterval(today);

            var run = run("--period", code);

            assertMatchesCore(run, core(interval), true);
            assertThat(code, meta(run).get("from").getAsString(), is(interval.getStart().plusDays(1).toString()));
            assertThat(code, meta(run).get("to").getAsString(), is(interval.getEnd().toString()));
            assertThat(code, meta(run).get("period").getAsString(), is(code));
        }
    }

    @Test
    public void aCalendarYearIsEchoedFromItsFirstToItsLastDay() throws IOException
    {
        var run = run("--period", "Y2025");

        assertThat(meta(run).get("from").getAsString(), is("2025-01-01"));
        assertThat(meta(run).get("to").getAsString(), is("2025-12-31"));
    }

    @Test
    public void aPeriodCodeAndTheEquivalentFromAndToGiveTheSameFiguresAndEcho() throws IOException
    {
        var byCode = run("--period", "Y2025");
        var byDates = run("--from", "2025-01-01", "--to", "2025-12-31");

        assertThat(data(byCode), is(data(byDates)));
        assertThat(meta(byCode).get("from"), is(meta(byDates).get("from")));
        assertThat(meta(byCode).get("to"), is(meta(byDates).get("to")));
    }

    @Test
    public void currentMonthResolvesAgainstTheClock() throws IOException
    {
        var run = run("--period", "M");

        assertMatchesCore(run, coreOverDays("2026-10-01", "2026-10-16"), true);
        assertThat(meta(run).get("from").getAsString(), is("2026-10-01"));
    }

    @Test
    public void yearToDateIsAcceptedAsYTD() throws IOException
    {
        var run = run("--period", "YTD");

        assertMatchesCore(run, coreOverDays("2026-01-01", "2026-10-16"), true);
        assertThat(meta(run).get("from").getAsString(), is("2026-01-01"));
        assertThat(meta(run).get("period").getAsString(), is("YTD"));
    }

    @Test
    public void anUnknownPeriodCodeIsAUsageError() throws IOException
    {
        for (var code : List.of("nonsense", "Y20x5", "9X9Y", "L1Y"))
        {
            var run = run("--period", code);

            assertThat(code + ": " + run.stdout, run.exitCode, is(2));
            assertThat(code, run.json().getAsJsonObject().get("code").getAsString(), is("invalid-option-value"));
        }
    }

    @Test
    public void aPeriodTogetherWithFromOrToIsAUsageError() throws IOException
    {
        for (var other : List.of(List.of("--from", "2026-01-01"), List.of("--to", "2026-06-30")))
        {
            var args = new ArrayList<>(List.of("--period", "L1Y0"));
            args.addAll(other);

            var run = run(args.toArray(String[]::new));

            assertThat(run.stdout, run.exitCode, is(2));
            assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("conflicting-options"));
        }
    }

    @Test
    public void withoutAPeriodOrFromIsAUsageError() throws IOException
    {
        for (var args : List.of(new String[0], new String[] { "--to", "2026-06-30" }))
        {
            var run = run(args);

            assertThat(run.stdout, run.exitCode, is(2));
            assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("period-required"));
        }
    }

    @Test
    public void aFromAfterToIsAUsageError() throws IOException
    {
        var run = run("--from", "2026-06-30", "--to", "2026-06-29");

        assertThat(run.stdout, run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("invalid-date-range"));
    }

    @Test
    public void aPeriodCodeThatStartsAfterItEndsIsAUsageError() throws IOException
    {
        var run = run("--period", "S2026-10-16");

        assertThat(run.stdout, run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("invalid-date-range"));
    }

    @Test
    public void costMethodDefaultsToFifoAndIsEchoed() throws IOException
    {
        var run = run("--from", "2026-01-01", "--to", "2026-09-30");

        assertThat(meta(run).get("costMethod").getAsString(), is("fifo"));
        assertMatchesCore(run, coreOverDays("2026-01-01", "2026-09-30"), true);
    }

    @Test
    public void movingAverageChangesTheGainsAsCoreDoesAndIsEchoed() throws IOException
    {
        var fifo = run("--from", "2026-01-01", "--to", "2026-09-30", "--cost-method", "fifo");
        var movingAverage = run("--from", "2026-01-01", "--to", "2026-09-30", "--cost-method", "moving-average");

        var index = coreOverDays("2026-01-01", "2026-09-30");
        assertMatchesCore(fifo, index, true);
        assertMatchesCore(movingAverage, index, false);

        assertThat(meta(movingAverage).get("costMethod").getAsString(), is("moving-average"));
        assertThat(data(movingAverage).get("realizedCapitalGains").getAsBigDecimal()
                        .compareTo(data(fifo).get("realizedCapitalGains").getAsBigDecimal()) != 0, is(true));
        assertThat(data(movingAverage).get("irr").getAsBigDecimal().compareTo(data(fifo).get("irr").getAsBigDecimal()),
                        is(0));
    }

    @Test
    public void anUnknownCostMethodIsAUsageError() throws IOException
    {
        var run = run("--from", "2026-01-01", "--cost-method", "lifo");

        assertThat(run.stdout, run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("invalid-option-value"));
    }

    private static List<JsonObject> warnings(CliRun run, String code)
    {
        var all = run.json().getAsJsonObject().getAsJsonArray("warnings");
        var found = new ArrayList<JsonObject>();
        if (all != null)
            all.forEach(w -> {
                if (w.getAsJsonObject().get("code").getAsString().equals(code))
                    found.add(w.getAsJsonObject());
            });
        return found;
    }

    private void addDollarAccount()
    {
        new AccountBuilder("USD").deposit_("2026-02-01", PortfolioBuilder.amountOf(1_000)).addTo(client)
                        .setName("Dollars");
    }

    @Test
    public void aClientInItsBaseCurrencyRaisesNoWarnings() throws IOException
    {
        var run = run("--from", "2026-01-01", "--to", "2026-09-30");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.json().getAsJsonObject().getAsJsonArray("warnings").size(), is(0));
    }

    @Test
    public void aForeignCurrencyWithoutARateIsWarnedAboutAndTheFiguresAreStillReported() throws IOException
    {
        addDollarAccount();
        var missingCache = folder.getRoot().toPath().resolve("missing").resolve("ecb_exchange_rates.pb");

        var run = run(Map.of("PP_ECB_RATES", missingCache.toString()), "--from", "2026-01-01", "--to",
                        "2026-09-30");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(data(run).has("absoluteGain"), is(true));
        var missing = warnings(run, "exchange-rate-missing");
        assertThat(missing.size(), is(1));
        assertThat(missing.get(0).get("baseCurrency").getAsString(), is("USD"));
        assertThat(missing.get(0).get("termCurrency").getAsString(), is("EUR"));
        assertThat(warnings(run, "exchange-rate-cache-unavailable").size(), is(1));
    }

    @Test
    public void aStaleRateIsWarnedAbout() throws IOException
    {
        addDollarAccount();
        var cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-03-01", "1.2000"));

        var run = run(Map.of("PP_ECB_RATES", cache.toString()), "--from", "2026-01-01", "--to", "2026-09-30");

        assertThat(run.stdout, run.exitCode, is(0));
        var stale = warnings(run, "stale-exchange-rate");
        assertThat(stale.size(), is(1));
        assertThat(stale.get(0).get("baseCurrency").getAsString(), is("USD"));
        assertThat(warnings(run, "exchange-rate-missing").size(), is(0));
    }

    @Test
    public void aFreshRateRaisesNoWarning() throws IOException
    {
        addDollarAccount();
        var cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-09-30", "1.2000"));

        var run = run(Map.of("PP_ECB_RATES", cache.toString()), "--from", "2026-01-01", "--to", "2026-09-30");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.json().getAsJsonObject().getAsJsonArray("warnings").size(), is(0));
    }

    @Test
    public void aForeignCurrencyThatWasClosedBeforeThePeriodRaisesNoWarning() throws IOException
    {
        new AccountBuilder("USD").deposit_("2025-01-01", PortfolioBuilder.amountOf(1_000))
                        .withdraw("2025-02-01", PortfolioBuilder.amountOf(1_000)).addTo(client).setName("Old Dollars");
        var missingCache = folder.getRoot().toPath().resolve("missing").resolve("ecb_exchange_rates.pb");

        var run = run(Map.of("PP_ECB_RATES", missingCache.toString()), "--from", "2026-01-01", "--to",
                        "2026-09-30");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(warnings(run, "exchange-rate-missing").size(), is(0));
    }

    @Test
    public void aPeriodEndingInTheFutureChecksTheRateAgainstTodayNotItsLastDay() throws IOException
    {
        addDollarAccount();
        var cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-10-16", "1.2000"));

        var run = run(Map.of("PP_ECB_RATES", cache.toString()), "--period", "Y2026");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).get("to").getAsString(), is("2026-12-31"));
        assertThat(warnings(run, "stale-exchange-rate").size(), is(0));
        assertThat(warnings(run, "exchange-rate-missing").size(), is(0));
    }

    @Test
    public void aForeignCurrencyHeldAtTheStartIsCheckedAtTheStartToo() throws IOException
    {
        new AccountBuilder("USD").deposit_("2025-06-01", PortfolioBuilder.amountOf(1_000)).addTo(client)
                        .setName("Dollars");
        var cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-03-01", "1.2000")
                        .rate("2026-09-30", "1.3000"));

        var run = run(Map.of("PP_ECB_RATES", cache.toString()), "--from", "2026-01-01", "--to", "2026-09-30");

        assertThat(run.stdout, run.exitCode, is(0));
        var missing = warnings(run, "exchange-rate-missing");
        assertThat(missing.size(), is(1));
        assertThat(missing.get(0).get("baseCurrency").getAsString(), is("USD"));
        assertThat(warnings(run, "stale-exchange-rate").size(), is(0));
    }

    @Test
    public void figuresAreInTheBaseCurrencyOrTheRequestedOneAsCoreConvertsThem() throws IOException
    {
        var cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
        var usd = EcbCacheFiles.series("EUR", "USD").rate("2025-12-31", "1.1000").rate("2026-03-01", "1.2000")
                        .rate("2026-09-30", "1.3000");
        EcbCacheFiles.write(cache, usd);

        var run = run(Map.of("PP_ECB_RATES", cache.toString()), "--from", "2026-01-01", "--to", "2026-09-30",
                        "--currency", "USD");
        assertThat(run.stdout, run.exitCode, is(0));

        var snapshot = new ClientPerformanceSnapshot(client, CoreRates.converter("USD", usd),
                        Interval.of(LocalDate.parse("2025-12-31"), LocalDate.parse("2026-09-30")));
        assertThat(meta(run).get("currency").getAsString(), is("USD"));
        assertThat(data(run).get("currencyCode").getAsString(), is("USD"));
        assertThat(data(run).get("finalValue").getAsBigDecimal()
                        .compareTo(amount(snapshot.getValue(CategoryType.FINAL_VALUE).getAmount())), is(0));
        assertThat(data(run).get("absoluteGain").getAsBigDecimal()
                        .compareTo(amount(snapshot.getAbsoluteDelta().getAmount())), is(0));
    }
}
