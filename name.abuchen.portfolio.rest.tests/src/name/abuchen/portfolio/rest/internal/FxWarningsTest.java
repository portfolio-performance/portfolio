package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.internal.ConversionRecorder.Pair;

/**
 * What {@link FxWarnings} itself decides. The route tests assert that a report
 * carries {@code fxWarnings}; this asserts which pairs land in it, because a
 * silently converted pair is the one thing a report must never present as
 * converted.
 * <p/>
 * The currencies are chosen on purpose: USD, GBP and CHF all have built-in ECB
 * series, so they cannot exercise the fallback. ARS and BBD are in the currency
 * bundle but reachable by no provider, which is the state a real file ends up in
 * when a user adds an account in a currency nothing quotes.
 */
@SuppressWarnings("nls")
public class FxWarningsTest
{
    private static ExchangeRateProviderFactory factory(Client client)
    {
        return new ExchangeRateProviderFactory(client);
    }

    @Test
    public void testPairWithoutAnySeriesIsReportedAsNoSeries()
    {
        var client = new Client();

        assertThat(FxWarnings.list(List.of(new Pair("ARS", CurrencyUnit.EUR)), factory(client)),
                        contains(new FxWarnings.Warning("ARS", "EUR", "no-series")));
    }

    /** an exchange-rate security without prices: the series exists, answers no date */
    @Test
    public void testSeriesWithoutRatesIsReportedAsNoRate()
    {
        var client = new Client();
        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(rates);

        assertThat(FxWarnings.list(List.of(new Pair("ARS", CurrencyUnit.EUR)), factory(client)),
                        contains(new FxWarnings.Warning("ARS", "EUR", "no-rate")));
    }

    /** a pair with rates converts, so it is never reported */
    @Test
    public void testPairWithRatesIsSilent()
    {
        var client = new Client();
        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        client.addSecurity(rates);

        assertThat(FxWarnings.list(List.of(new Pair("ARS", CurrencyUnit.EUR)), factory(client)), is(empty()));
    }

    /**
     * An inverse or a chained series must not be mistaken for the placeholder:
     * USD and GBP reach EUR through the built-in ECB series, in one step for
     * USD and in two for GBP, and neither is a fallback.
     */
    @Test
    public void testInverseAndChainedSeriesAreSilent()
    {
        var pairs = List.of(new Pair("USD", "EUR"), new Pair("GBP", "EUR"), new Pair("EUR", "USD"),
                        new Pair("GBP", "USD"));

        assertThat(FxWarnings.list(pairs, factory(new Client())), is(empty()));
    }

    /** the warnings keep the order of the pairs, which the recorder makes stable */
    @Test
    public void testWarningsKeepTheOrderOfThePairs()
    {
        var warnings = FxWarnings.list(List.of(new Pair("ARS", "EUR"), new Pair("BBD", "EUR")),
                        factory(new Client()));

        assertThat(warnings.size(), is(2));
        assertThat(warnings.get(0).currency(), is("ARS"));
        assertThat(warnings.get(1).currency(), is("BBD"));
    }

    /**
     * A report that converted nothing still carries the field, because its
     * absence would read as "this report did not look" rather than "nothing fell
     * back".
     */
    @Test
    public void testAttachAddsAnEmptyArrayWhenNothingWasConverted()
    {
        var report = new JsonObject();
        FxWarnings.attach(report, new ConversionRecorder(), factory(new Client()));

        assertThat(report.get("fxWarnings").getAsJsonArray().size(), is(0));
    }

    /** what a converter was asked to do is what the warning describes */
    @Test
    public void testAttachNamesThePairTheCalculationConverted()
    {
        var client = new Client();
        var factory = factory(client);
        var conversions = new ConversionRecorder();
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, "ARS"));
        converter.convert(LocalDate.of(2026, 1, 2), Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));

        var report = new JsonObject();
        FxWarnings.attach(report, conversions, factory);

        var warnings = report.get("fxWarnings").getAsJsonArray();
        assertThat(warnings.size(), is(1));

        var warning = warnings.get(0).getAsJsonObject();
        assertThat(warning.get("currency").getAsString(), is("EUR"));
        assertThat(warning.get("reportingCurrency").getAsString(), is("ARS"));
        assertThat(warning.get("reason").getAsString(), is("no-series"));
    }
}
