package name.abuchen.portfolio.cli.currency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.EcbCacheFiles;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.FacetHarness;
import name.abuchen.portfolio.cli.command.WarningSpec;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.online.QuoteFeed;

/**
 * The facets of the commands that convert, each at its own interface through
 * {@link FacetHarness}: the reporting currency with and without a default,
 * and the stale-price collector. Today is 2026-10-16; the environment names
 * no ECB cache unless a test gives one.
 */
public class CurrencyFacetsTest
{
    private static final LocalDate DAY = LocalDate.parse("2026-03-20");

    /** 19 days before {@link #DAY}. */
    private static final SecurityPrice OLD = new SecurityPrice(LocalDate.parse("2026-03-01"),
                    PortfolioBuilder.quoteOf(10));

    private static final Facet<ReportingCurrency> REPORTING = ReportingCurrency.facet("The currency.",
                    ReportingCurrency.Counting.ITEMS);

    private static final Facet<Optional<ReportingCurrency>> IF_GIVEN = ReportingCurrency.ifGiven("The currency.");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static Client client()
    {
        var client = new Client();
        client.setBaseCurrency("EUR");
        return client;
    }

    private static Security security(String name)
    {
        var security = new Security(name, "EUR");
        security.addPrice(OLD);
        return security;
    }

    private static FacetHarness.Result<StalePrices> noting(FacetHarness.Body<StalePrices> handler)
    {
        return FacetHarness.run(StalePrices.FACET, List.of(), null, Map.of(), handler);
    }

    private static List<String> instruments(FacetHarness.Result<?> result)
    {
        return result.warnings().stream().map(warning -> (String) warning.toJson().get("instrument")).toList();
    }

    private static Set<String> codes(FacetHarness.Result<?> result)
    {
        return result.warnings().stream().map(warning -> warning.code()).collect(Collectors.toSet());
    }

    @Test
    public void anInstrumentMetTwiceWarnsOnce()
    {
        var alpha = security("Alpha");

        var result = noting(stalePrices -> {
            stalePrices.note(alpha, OLD, DAY);
            stalePrices.note(alpha, OLD, DAY);
        });

        assertThat(instruments(result), is(List.of(alpha.getUUID())));
        assertThat(result.warningCodes(), is(List.of("stale-price")));
    }

    @Test
    public void stalePricesComeInNameThenUuidOrder()
    {
        var charlie = security("Charlie");
        var alpha = security("Alpha");
        var bravo1 = security("Bravo");
        var bravo2 = security("Bravo");
        var bravos = bravo1.getUUID().compareTo(bravo2.getUUID()) < 0 ? List.of(bravo1, bravo2)
                        : List.of(bravo2, bravo1);

        var result = noting(stalePrices -> {
            for (var security : List.of(charlie, bravos.get(1), alpha, bravos.get(0)))
                stalePrices.note(security, OLD, DAY);
        });

        assertThat(instruments(result), is(List.of(alpha.getUUID(), bravos.get(0).getUUID(),
                        bravos.get(1).getUUID(), charlie.getUUID())));
    }

    @Test
    public void freshRetiredAndManualPricesDoNotWarn()
    {
        var fresh = new Security("Fresh", "EUR");
        var current = new SecurityPrice(LocalDate.parse("2026-03-15"), PortfolioBuilder.quoteOf(10));
        fresh.addPrice(current);

        var retired = security("Retired");
        retired.setRetired(true);

        var manual = security("Manual");
        manual.setFeed(QuoteFeed.MANUAL);

        var result = noting(stalePrices -> {
            stalePrices.note(fresh, current, DAY);
            stalePrices.note(retired, OLD, DAY);
            stalePrices.note(manual, OLD, DAY);
        });

        assertThat(result.warnings().isEmpty(), is(true));
    }

    @Test
    public void theReportingCurrencyDefaultsToTheBaseCurrencyAndReadsNoCacheForTheIdentity()
    {
        var result = FacetHarness.run(REPORTING, List.of(), client(), Map.of(),
                        reporting -> reporting.figure(DAY, Money.of("EUR", 100)));

        assertThat(result.value().code(), is("EUR"));
        assertThat(result.meta().get("currency").getAsString(), is("EUR"));
        assertThat(result.meta().has("exchangeRates"), is(false));
        assertThat(result.warnings().isEmpty(), is(true));
    }

    @Test
    public void theReportingCurrencyIsTheOneGiven()
    {
        var result = FacetHarness.run(REPORTING, client(), "--currency", "USD");

        assertThat(result.value().code(), is("USD"));
        assertThat(result.meta().get("currency").getAsString(), is("USD"));
    }

    @Test
    public void anUnknownCurrencyIsAnInvalidValueBeforeAnyFileError()
    {
        // no client and no --file: a file error would be file-required
        assertThat(FacetHarness.run(REPORTING, null, "--currency", "XYZ").code(), is("invalid-option-value"));
        assertThat(FacetHarness.run(IF_GIVEN, null, "--currency", "XYZ").code(), is("invalid-option-value"));
    }

    @Test
    public void aCacheReadIsInMetaAndTheRateWarningsFollowTheHandler() throws IOException
    {
        var cache = EcbCacheFiles.write(folder.getRoot().toPath().resolve("ecb.pb"),
                        EcbCacheFiles.series("EUR", "USD").rate("2026-03-20", "1.25"));

        var result = FacetHarness.run(REPORTING, List.of(), client(), Map.of(RateCache.VARIABLE, cache.toString()),
                        reporting -> {
                            reporting.figure(DAY, Money.of("USD", 100));
                            reporting.figure(DAY, Money.of("CHF", 100));
                        });

        var rates = result.meta().getAsJsonObject("exchangeRates");
        assertThat(rates.get("path").getAsString(), is(cache.toString()));
        assertThat(rates.get("latestRateDate").getAsString(), is("2026-03-20"));
        assertThat(codes(result), is(Set.of("exchange-rate-missing")));
        assertThat(result.warnings().get(0).toJson().get("baseCurrency"), is("CHF"));
    }

    @Test
    public void anUnavailableCacheIsWarnedAboutWithTheMissingRate()
    {
        var result = FacetHarness.run(REPORTING, List.of(), client(), Map.of(),
                        reporting -> reporting.figure(DAY, Money.of("USD", 100)));

        assertThat(result.meta().has("exchangeRates"), is(false));
        assertThat(codes(result), is(Set.of("exchange-rate-cache-unavailable", "exchange-rate-missing")));
    }

    @Test
    public void theCountingPicksWhatTheRateWarningsSayTheyCount()
    {
        var items = ReportingCurrency.facet("The currency.", ReportingCurrency.Counting.ITEMS);
        var days = ReportingCurrency.facet("The currency.", ReportingCurrency.Counting.DAYS);

        assertThat(description(items, "exchange-rate-missing"), containsString("count of affected items"));
        assertThat(description(days, "exchange-rate-missing"), containsString("count of days"));
        assertThat(description(items, "stale-exchange-rate"), containsString("count of records"));
        assertThat(description(days, "stale-exchange-rate"), containsString("count of days"));
    }

    private static String description(Facet<?> facet, String code)
    {
        return facet.declaration().warnings().stream().filter(w -> w.code().equals(code)).findFirst()
                        .map(WarningSpec::description).orElseThrow();
    }

    @Test
    public void withoutTheOptionThereIsNoReportingCurrencyAndNoFileIsNeeded()
    {
        var result = FacetHarness.run(IF_GIVEN, null);

        assertThat(result.value(), is(Optional.empty()));
        assertThat(result.meta().has("currency"), is(false));
        assertThat(result.warnings().isEmpty(), is(true));
    }

    @Test
    public void aGivenCurrencyIsReportedAndItsRateWarningsFollowTheHandler()
    {
        var result = FacetHarness.run(IF_GIVEN, List.of("--currency", "USD"), client(), Map.of(),
                        reporting -> reporting.orElseThrow().figure(DAY, Money.of("CHF", 100)));

        assertThat(result.value().orElseThrow().code(), is("USD"));
        assertThat(result.meta().get("currency").getAsString(), is("USD"));
        assertThat(codes(result), is(Set.of("exchange-rate-cache-unavailable", "exchange-rate-missing")));
    }
}
