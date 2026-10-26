package name.abuchen.portfolio.cli.currency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.EcbCacheFiles;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;

/**
 * The reporting currency on its own, in process: which figures need a rate,
 * which rate wins, and when a total can be stated. The client has no
 * exchange-rate instruments, so every rate other than core's fixed pairs comes
 * from the ECB cache file of the test, if one is written.
 */
public class ReportingCurrencyTest
{
    private static final OptionSpec CURRENCY = ReportingCurrency.option("The currency.");

    private static final LocalDate DAY = LocalDate.parse("2026-03-02");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Client client;
    private Path cache;

    @Before
    public void setUp()
    {
        client = new Client();
        client.setBaseCurrency("EUR");
        cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
    }

    private CommandContext context(Map<String, String> options)
    {
        return new CommandContext(options, Map.of(EcbCache.VARIABLE, cache.toString()),
                        new ByteArrayInputStream(new byte[0]), new PrintStream(new ByteArrayOutputStream()),
                        CliRun.CLOCK, "Linux", null);
    }

    /** The reporting currency of a run without <code>--currency</code>: the base currency. */
    private ReportingCurrency base() throws CliException
    {
        var context = context(Map.of());
        return ReportingCurrency.requested(context, CURRENCY).orBase(context, client);
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

    @Test
    public void aZeroAmountInAnotherCurrencyNeedsNoRateAndDoesNotSpoilTheTotal() throws CliException
    {
        var reporting = base();

        var zero = reporting.figure(DAY, Money.of("USD", 0));
        var euros = reporting.figure(DAY, Money.of("EUR", 100_00));

        assertThat(converted(reporting, zero) == null, is(true));
        var total = reporting.total(List.of(zero, euros)).orElseThrow();
        assertThat(total.get("currencyCode"), is("EUR"));
        assertThat(decimal(total, "amount"), is("100.00"));
        assertThat(reporting.warnings().isEmpty(), is(true));
    }

    @Test
    public void oneFigureWithoutItsRateSpoilsTheTotal() throws CliException
    {
        var reporting = base();

        var euros = reporting.figure(DAY, Money.of("EUR", 100_00));
        var dollars = reporting.figure(DAY, Money.of("USD", 50_00));

        assertThat(converted(reporting, dollars) == null, is(true));
        assertThat(reporting.total(List.of(euros, dollars)).isPresent(), is(false));
        assertThat(codes(reporting), contains("exchange-rate-cache-unavailable", "exchange-rate-missing"));
    }

    @Test
    public void theSameCurrencyIsTheIdentityAndReadsNoCache() throws CliException
    {
        var reporting = base();

        var converted = converted(reporting, reporting.figure(DAY, Money.of("EUR", 100_00)));

        assertThat(converted.get("currencyCode"), is("EUR"));
        assertThat(decimal(converted, "amount"), is("100.00"));
        assertThat(decimal(converted, "rate"), is("1"));
        assertThat(converted.get("rateDate"), is("2026-03-02"));
        assertThat(converted.get("rateSource"), is("identity"));

        var meta = new HashMap<String, Object>();
        reporting.putRatesInto(meta);
        assertThat(meta.isEmpty(), is(true));
        assertThat(reporting.warnings().isEmpty(), is(true));
    }

    @Test
    public void aRecordsOwnRateBeatsTheRateSeries() throws CliException, IOException
    {
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-03-02", "1.25"));
        var reporting = base();

        var figure = reporting.figure(DAY, "USD", 100_00, Optional.of(new BigDecimal("0.9")));
        var converted = converted(reporting, figure);

        assertThat(decimal(converted, "amount"), is("90.00"));
        assertThat(decimal(converted, "rate"), is("0.9"));
        assertThat(converted.get("rateSource"), is("transaction"));

        // without its own rate, the series of the cache
        var fromCache = converted(reporting, reporting.figure(DAY, "USD", 100_00, Optional.empty()));
        assertThat(decimal(fromCache, "amount"), is("80.00"));
        assertThat(fromCache.get("rateSource"), is("ecb"));
    }

    @Test
    public void allRatedLooksUpEveryValueAndSoReportsEveryMissingPair() throws CliException
    {
        var reporting = base();

        var rated = reporting.allRated(DAY, List.of(Money.of("USD", 1_00), Money.of("CHF", 1_00),
                        Money.of("JPY", 0), Money.of("EUR", 1_00)));

        assertThat(rated, is(false));
        var missing = reporting.warnings().stream().filter(w -> w.code().equals("exchange-rate-missing"))
                        .map(w -> w.toJson().get("baseCurrency")).toList();
        assertThat(missing, contains("USD", "CHF"));
    }

    @Test
    public void givenNoCurrencyThereIsNoReportingCurrencyWithoutADefault() throws CliException
    {
        var context = context(Map.of());

        assertThat(ReportingCurrency.requested(context, CURRENCY).ifGiven(context, client).isPresent(), is(false));
    }

    @Test
    public void theCurrencyGivenWinsOverTheBaseCurrency() throws CliException
    {
        var context = context(Map.of("currency", "USD"));
        var requested = ReportingCurrency.requested(context, CURRENCY);

        assertThat(requested.orBase(context, client).code(), is("USD"));
        assertThat(requested.ifGiven(context, client).orElseThrow().code(), is("USD"));
    }

    @Test
    public void aCurrencyCoreDoesNotKnowIsRejected()
    {
        var context = context(Map.of("currency", "XYZ"));

        try
        {
            ReportingCurrency.requested(context, CURRENCY);
            throw new AssertionError("expected invalid-option-value");
        }
        catch (CliException e)
        {
            assertThat(e.getCode(), is("invalid-option-value"));
        }
    }
}
