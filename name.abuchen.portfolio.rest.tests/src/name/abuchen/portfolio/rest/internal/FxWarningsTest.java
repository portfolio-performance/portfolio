package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

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

/**
 * How {@link FxWarnings} puts what a {@link ConversionRecorder} saw into a
 * report. Which pairs fall back, and when, is the recorder's test.
 */
@SuppressWarnings("nls")
public class FxWarningsTest
{
    /**
     * A report that converted nothing at 1:1 still carries the field, because its
     * absence would read as "this report did not look" rather than "nothing fell
     * back".
     */
    @Test
    public void testAttachAddsAnEmptyArrayWhenNothingFellBack()
    {
        var report = new JsonObject();
        FxWarnings.attach(report, new ConversionRecorder(new ExchangeRateProviderFactory(new Client())));

        assertThat(report.get("fxWarnings").getAsJsonArray().size(), is(0));
        assertThat(report.get("fxStaleRates").getAsJsonArray().size(), is(0));
    }

    /** a conversion at an old rate is described by its pair, the rate's date and its age */
    @Test
    public void testAttachDescribesAStaleRate()
    {
        var client = new Client();
        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        client.addSecurity(rates);

        var factory = new ExchangeRateProviderFactory(client);
        var conversions = new ConversionRecorder(factory, LocalDate.of(2026, 10, 28));
        conversions.wrap(new CurrencyConverterImpl(factory, CurrencyUnit.EUR)).convert(LocalDate.of(2026, 3, 1),
                        Money.of("ARS", Values.Amount.factorize(10)));

        var report = new JsonObject();
        FxWarnings.attach(report, conversions);

        var stale = report.get("fxStaleRates").getAsJsonArray();
        assertThat(stale.size(), is(1));
        var entry = stale.get(0).getAsJsonObject();
        assertThat(entry.get("currency").getAsString(), is("ARS"));
        assertThat(entry.get("reportingCurrency").getAsString(), is("EUR"));
        assertThat(entry.get("rateDate").getAsString(), is("2026-01-02"));
        assertThat(entry.get("daysOld").getAsLong(), is(58L));
    }

    /** what a converter fell back on is what the warning describes */
    @Test
    public void testAttachNamesThePairTheCalculationConvertedAtOneToOne()
    {
        var factory = new ExchangeRateProviderFactory(new Client());
        var conversions = new ConversionRecorder(factory);
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, "ARS"));
        converter.convert(LocalDate.of(2026, 1, 2), Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));

        var report = new JsonObject();
        FxWarnings.attach(report, conversions);

        var warnings = report.get("fxWarnings").getAsJsonArray();
        assertThat(warnings.size(), is(1));

        var warning = warnings.get(0).getAsJsonObject();
        assertThat(warning.get("currency").getAsString(), is("EUR"));
        assertThat(warning.get("reportingCurrency").getAsString(), is("ARS"));
        assertThat(warning.get("reason").getAsString(), is("no-series"));
    }
}
