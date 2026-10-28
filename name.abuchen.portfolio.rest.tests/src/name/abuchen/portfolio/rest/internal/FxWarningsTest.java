package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
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
