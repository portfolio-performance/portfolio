package name.abuchen.portfolio.cli.currency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.cli.EcbCacheFiles;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;

/**
 * The reporting currency through its interface, over rates held in memory
 * ({@link RateCache#of}): which figures need a rate, which rate wins, when a
 * total can be stated, and when the figures core calculates can be trusted.
 * The client has no exchange-rate instruments unless a test adds one.
 */
public class ReportingCurrencyTest
{
    private static final LocalDate DAY = LocalDate.parse("2026-03-02");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-16");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Client client;

    @Before
    public void setUp()
    {
        client = new Client();
        client.setBaseCurrency("EUR");
    }

    /** One currency pair of the cache: alternating ISO dates and values. */
    private static EcbCache.Series series(String base, String term, String... datesAndValues)
    {
        var rates = new ArrayList<ExchangeRate>();
        for (var i = 0; i < datesAndValues.length; i += 2)
            rates.add(new ExchangeRate(LocalDate.parse(datesAndValues[i]), new BigDecimal(datesAndValues[i + 1])));
        return new EcbCache.Series(base, term, rates);
    }

    private ReportingCurrency reporting(String code, EcbCache.Series... series)
    {
        return new ReportingCurrency(code, client, RateCache.of(series), TODAY);
    }

    private ReportingCurrency reporting(String code, RateCache cache)
    {
        return new ReportingCurrency(code, client, cache, TODAY);
    }

    private static Map<String, Object> converted(ReportingCurrency reporting, ReportingCurrency.Figure figure)
    {
        var json = new LinkedHashMap<String, Object>();
        reporting.putConvertedInto(json, figure);
        @SuppressWarnings("unchecked")
        var converted = (Map<String, Object>) json.get("converted");
        return converted;
    }

    private static String decimal(Map<String, Object> json, String member)
    {
        return ((JsonPrimitive) json.get(member)).getAsBigDecimal().toPlainString();
    }

    private static List<String> codes(ReportingCurrency reporting)
    {
        return reporting.warnings().stream().map(Warning::code).toList();
    }

    private static List<Map<String, Object>> warnings(ReportingCurrency reporting, String code)
    {
        return reporting.warnings().stream().filter(w -> w.code().equals(code)).map(Warning::toJson).toList();
    }

    private static Map<String, Object> meta(ReportingCurrency reporting)
    {
        var meta = new HashMap<String, Object>();
        reporting.putRatesInto(meta);
        return meta;
    }

    // --- sources

    @Test
    public void theSameCurrencyIsTheIdentityAndReadsNoCache()
    {
        var reporting = reporting("EUR", () -> {
            throw new AssertionError("the cache must not be read");
        });

        var converted = converted(reporting, reporting.figure(DAY, Money.of("EUR", 100_00)));

        assertThat(converted.get("currencyCode"), is("EUR"));
        assertThat(decimal(converted, "amount"), is("100.00"));
        assertThat(decimal(converted, "rate"), is("1"));
        assertThat(converted.get("rateDate"), is("2026-03-02"));
        assertThat(converted.get("rateSource"), is("identity"));
        assertThat(meta(reporting).isEmpty(), is(true));
        assertThat(reporting.warnings().isEmpty(), is(true));
    }

    @Test
    public void aRecordsOwnRateBeatsTheRateSeries()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-02", "1.25"));

        var converted = converted(reporting, reporting.figure(DAY, "USD", 100_00, Optional.of(new BigDecimal("0.9"))));

        assertThat(decimal(converted, "amount"), is("90.00"));
        assertThat(decimal(converted, "rate"), is("0.9"));
        assertThat(converted.get("rateSource"), is("transaction"));
    }

    @Test
    public void theEcbCacheConvertsAndIsNamedInMeta()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-02", "1.25"));

        var converted = converted(reporting, reporting.figure(DAY, "USD", 100_00, Optional.empty()));

        assertThat(decimal(converted, "amount"), is("80.00"));
        assertThat(decimal(converted, "rate"), is("0.8000000000"));
        assertThat(converted.get("rateSource"), is("ecb"));
        @SuppressWarnings("unchecked")
        var rates = (Map<String, Object>) meta(reporting).get("exchangeRates");
        assertThat(rates.get("path"), is(Path.of("in-memory").toString()));
        assertThat(rates.get("latestRateDate"), is("2026-03-02"));
    }

    @Test
    public void anExchangeRateInstrumentIsPreferredAndIsNotCalledEcb()
    {
        var instrument = new SecurityBuilder("USD").addPrice("2026-03-01", 92_000_000L).addTo(client);
        instrument.setTargetCurrencyCode("EUR");
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-02", "1.5"));

        var converted = converted(reporting, reporting.figure(DAY, Money.of("USD", 100_00)));

        assertThat(decimal(converted, "amount"), is("92.00"));
        assertThat(converted.get("rateDate"), is("2026-03-01"));
        assertThat(converted.get("rateSource"), is("instrument"));
    }

    @Test
    public void coreFixedPairsConvertAndAreNamedAsFixed()
    {
        var reporting = reporting("GBX");

        var converted = converted(reporting, reporting.figure(DAY, Money.of("GBP", 100_00)));

        assertThat(decimal(converted, "amount"), is("10000.00"));
        assertThat(converted.get("rateSource"), is("fixed"));
    }

    @Test
    public void aChainedRateIsDatedByItsOldestStep()
    {
        var reporting = reporting("CHF", series("EUR", "USD", "2026-03-01", "1.25"),
                        series("EUR", "CHF", "2026-02-27", "0.94"));

        var converted = converted(reporting, reporting.figure(DAY, Money.of("USD", 100_00)));

        assertThat(converted.get("rateDate"), is("2026-02-27"));
        assertThat(converted.get("rateSource"), is("ecb"));
        assertThat(decimal(converted, "amount"), is("75.20"));
    }

    // --- missing and stale

    @Test
    public void aRateDatedOnlyAfterTheDayIsMissing()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-05", "1.25"));

        var figure = reporting.figure(DAY, Money.of("USD", 100_00));

        assertThat(converted(reporting, figure) == null, is(true));
        assertThat(codes(reporting), contains("exchange-rate-missing"));
    }

    @Test
    public void missingRatesAreWarnedOncePerPairCountingTheItems()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-05", "1.25"));

        reporting.figure(DAY, Money.of("USD", 1_00));
        reporting.figure(DAY, Money.of("USD", 2_00));
        reporting.figure(DAY, Money.of("CHF", 3_00));

        var missing = warnings(reporting, "exchange-rate-missing");
        assertThat(missing, hasSize(2));
        assertThat(missing.get(0).get("baseCurrency"), is("USD"));
        assertThat(missing.get(0).get("termCurrency"), is("EUR"));
        assertThat(missing.get(0).get("count"), is(2));
        assertThat(missing.get(1).get("baseCurrency"), is("CHF"));
        assertThat(missing.get(1).get("count"), is(1));
    }

    @Test
    public void aStaleRateIsUsedAndWarnedAboutWithItsGreatestAge()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-02-20", "1.25"));

        var converted = converted(reporting, reporting.figure(DAY, Money.of("USD", 100_00)));
        reporting.figure(DAY.plusDays(3), Money.of("USD", 100_00));

        assertThat(decimal(converted, "amount"), is("80.00"));
        var stale = warnings(reporting, "stale-exchange-rate");
        assertThat(stale, hasSize(1));
        assertThat(stale.get(0).get("count"), is(2));
        assertThat(stale.get(0).get("maxAgeDays"), is(13L));
    }

    @Test
    public void aZeroAmountInAnotherCurrencyNeedsNoRateAndDoesNotSpoilTheTotal()
    {
        var reporting = reporting("EUR");

        var zero = reporting.figure(DAY, Money.of("USD", 0));
        var euros = reporting.figure(DAY, Money.of("EUR", 100_00));

        assertThat(converted(reporting, zero) == null, is(true));
        var total = reporting.total(List.of(zero, euros)).orElseThrow();
        assertThat(total.get("currencyCode"), is("EUR"));
        assertThat(decimal(total, "amount"), is("100.00"));
        assertThat(reporting.warnings().isEmpty(), is(true));
    }

    @Test
    public void oneFigureWithoutItsRateSpoilsTheTotal()
    {
        var reporting = reporting("EUR");

        var euros = reporting.figure(DAY, Money.of("EUR", 100_00));
        var dollars = reporting.figure(DAY, Money.of("USD", 50_00));

        assertThat(reporting.total(List.of(euros, dollars)).isPresent(), is(false));
        assertThat(codes(reporting), contains("exchange-rate-missing"));
    }

    // --- the cache

    @Test
    public void eachReasonTheCacheIsUnavailableIsWarnedAboutAndNothingFallsBack() throws IOException
    {
        var missing = folder.getRoot().toPath().resolve("missing.pb");
        var garbage = folder.newFile("garbage.pb").toPath();
        Files.write(garbage, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        var empty = folder.newFile("empty.pb").toPath();
        var directory = folder.newFolder("directory").toPath();

        var cases = new LinkedHashMap<String, Map<String, String>>();
        cases.put("not-configured", Map.of());
        cases.put("not-found", Map.of(RateCache.VARIABLE, missing.toString()));
        cases.put("unreadable", Map.of(RateCache.VARIABLE, directory.toString()));
        cases.put("unparsable", Map.of(RateCache.VARIABLE, garbage.toString()));
        cases.put("unparsable ", Map.of(RateCache.VARIABLE, empty.toString()));

        for (var entry : cases.entrySet())
        {
            var reason = entry.getKey().trim();
            var env = entry.getValue();
            var reporting = reporting("USD", RateCache.desktop(name -> Optional.ofNullable(env.get(name)), "Linux"));

            assertThat(reason, converted(reporting, reporting.figure(DAY, Money.of("EUR", 100_00))) == null, is(true));
            reporting.figure(DAY, Money.of("EUR", 1_00));

            var unavailable = warnings(reporting, "exchange-rate-cache-unavailable");
            assertThat(reason, unavailable, hasSize(1));
            assertThat(reason, unavailable.get(0).get("reason"), is(reason));
            assertThat(reason, unavailable.get(0).containsKey("path"), is(!reason.equals("not-configured")));
            assertThat(reason, warnings(reporting, "exchange-rate-missing").get(0).get("count"), is(2));
            assertThat(reason, meta(reporting).isEmpty(), is(true));
        }
    }

    @Test
    public void theDesktopCacheIsReadFromTheVariable() throws IOException
    {
        var file = EcbCacheFiles.write(folder.getRoot().toPath().resolve("ecb.pb"),
                        EcbCacheFiles.series("EUR", "USD").rate("2026-03-02", "1.25"));
        var reporting = reporting("EUR", RateCache.desktop(
                        name -> RateCache.VARIABLE.equals(name) ? Optional.of(file.toString()) : Optional.empty(),
                        "Linux"));

        var converted = converted(reporting, reporting.figure(DAY, Money.of("USD", 100_00)));

        assertThat(decimal(converted, "amount"), is("80.00"));
        @SuppressWarnings("unchecked")
        var rates = (Map<String, Object>) meta(reporting).get("exchangeRates");
        assertThat(rates.get("path"), is(file.toString()));
    }

    @Test
    public void theCacheIsReadOnceAndOnlyWhenNeeded()
    {
        var reads = new int[1];
        var loaded = RateCache.of(series("EUR", "USD", "2026-03-02", "1.25"));
        var reporting = reporting("EUR", () -> {
            reads[0]++;
            return loaded.load();
        });

        reporting.figure(DAY, Money.of("EUR", 1_00));
        assertThat(reads[0], is(0));

        reporting.figure(DAY, Money.of("USD", 1_00));
        reporting.figure(DAY, Money.of("USD", 2_00));
        assertThat(reads[0], is(1));
    }

    // --- figures core calculates

    @Test
    public void trustedGivesTheFiguresWhenCoreFoundEveryRate()
    {
        var core = reporting("EUR", series("EUR", "USD", "2026-03-02", "1.25")).coreFigures();

        var figures = core.trusted(() -> Money.of("USD", 100_00).with(core.converter().at(DAY)));

        assertThat(figures.orElseThrow(), is(Money.of("EUR", 80_00)));
    }

    @Test
    public void trustedLeavesTheFiguresOutWhenARateCoreAskedForWasMissingAndCountsTheDays()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-05", "1.25"));
        var core = reporting.coreFigures();

        var figures = core.trusted(() -> {
            var converter = core.converter();
            Money.of("USD", 1_00).with(converter.at(DAY));
            Money.of("USD", 2_00).with(converter.at(DAY));
            return Money.of("USD", 3_00).with(converter.at(DAY.minusDays(1)));
        });

        assertThat(figures.isPresent(), is(false));
        var missing = warnings(reporting, "exchange-rate-missing");
        assertThat(missing, hasSize(1));
        assertThat(missing.get(0).get("count"), is(2));
    }

    @Test
    public void trustedJudgesOnlyTheRatesAskedForWhileItsSupplierRuns()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-01", "1.25"));
        var core = reporting.coreFigures();

        // a rate before the first: missing, but asked for outside the call
        Money.of("USD", 1_00).with(core.converter().at(DAY.minusMonths(1)));

        // core calculates lazily: the supplier reads the figure, so its rate is asked for inside the call
        Supplier<Money> lazy = () -> Money.of("USD", 1_00).with(core.converter().at(DAY));
        assertThat(core.trusted(lazy).isPresent(), is(true));

        var spoiled = core.trusted(() -> Money.of("USD", 1_00).with(core.converter().at(DAY.minusMonths(2))));
        assertThat(spoiled.isPresent(), is(false));

        // the next call is judged on its own
        assertThat(core.trusted(lazy).isPresent(), is(true));
    }

    @Test
    public void aDayAfterTodayIsCheckedAsToday()
    {
        var core = reporting("EUR", series("EUR", "USD", "2026-10-16", "1.25")).coreFigures();

        var figures = core.trusted(() -> Money.of("USD", 1_00).with(core.converter().at(TODAY.plusDays(30))));

        assertThat(figures.isPresent(), is(true));
    }

    @Test
    public void trustedOverItemsCountsTheItemsNotTheRatesCoreAskedFor()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-05", "1.25"));
        var core = reporting.coreFigures();
        var items = List.of(Money.of("USD", 1_00), Money.of("USD", 2_00), Money.of("EUR", 3_00));

        var figures = core.trusted(DAY, items, () -> {
            var converter = core.converter();
            Money.of("USD", 1_00).with(converter.at(DAY));
            return Money.of("USD", 1_00).with(converter.at(DAY.minusDays(1)));
        });

        assertThat(figures.isPresent(), is(false));
        var missing = warnings(reporting, "exchange-rate-missing");
        assertThat(missing, hasSize(1));
        assertThat(missing.get(0).get("count"), is(2));
    }

    @Test
    public void trustedOverItemsGivesTheFiguresWhenEveryItemAndEveryCoreRateHasItsRate()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-02", "1.25"));
        var core = reporting.coreFigures();

        var figures = core.trusted(DAY, List.of(Money.of("USD", 100_00)),
                        () -> Money.of("USD", 100_00).with(core.converter().at(DAY)));

        assertThat(figures.orElseThrow(), is(Money.of("EUR", 80_00)));
        assertThat(reporting.warnings().isEmpty(), is(true));
    }

    @Test
    public void aRateOnlyCoreMissedLeavesTheFiguresOutAndIsWarnedAboutOnce()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-01", "1.25"));
        var core = reporting.coreFigures();

        var figures = core.trusted(DAY, List.of(Money.of("USD", 100_00)), () -> {
            var dollars = core.converter().with("USD");
            Money.of("EUR", 1_00).with(dollars.at(DAY.minusMonths(1)));
            Money.of("EUR", 1_00).with(dollars.at(DAY.minusMonths(2)));
            return Money.of("USD", 100_00).with(core.converter().at(DAY));
        });

        assertThat(figures.isPresent(), is(false));
        var missing = warnings(reporting, "exchange-rate-missing");
        assertThat(missing, hasSize(1));
        assertThat(missing.get(0).get("baseCurrency"), is("EUR"));
        assertThat(missing.get(0).get("termCurrency"), is("USD"));
        assertThat(missing.get(0).get("count"), is(1));
    }

    @Test
    public void unreportedToleratesCoresFallbackAndWarnsAboutNothing()
    {
        var reporting = reporting("EUR", series("EUR", "USD", "2026-03-05", "1.25"));

        var converted = Money.of("USD", 100_00).with(reporting.coreFigures().unreported().at(DAY));

        assertThat(converted.getCurrencyCode(), is("EUR"));
        assertThat(codes(reporting).contains("exchange-rate-missing"), is(false));
    }
}
