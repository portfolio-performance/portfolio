package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.internal.ConversionRecorder.Pair;

/**
 * The recorder sees exactly the conversions a calculation performed - no more,
 * so a report does not warn about money it never contained, and no less, so a
 * fallback cannot hide.
 */
@SuppressWarnings("nls")
public class ConversionRecorderTest
{
    private static final LocalDate DATE = LocalDate.of(2026, 1, 2);

    private final ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(new Client());

    @Test
    public void testAConversionIsRecordedAsItsPair()
    {
        var conversions = new ConversionRecorder();
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, CurrencyUnit.EUR));

        converter.convert(DATE, Money.of("ARS", Values.Amount.factorize(10)));

        assertThat(conversions.pairs(), contains(new Pair("ARS", "EUR")));
    }

    /** an amount already in the term currency is not a conversion */
    @Test
    public void testAnAmountInTheTermCurrencyIsNotRecorded()
    {
        var conversions = new ConversionRecorder();
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, CurrencyUnit.EUR));

        converter.convert(DATE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)));
        converter.getRate(DATE, CurrencyUnit.EUR);

        assertThat(conversions.pairs(), is(empty()));
    }

    /** a zero amount is never looked up - an empty account in another currency converts nothing */
    @Test
    public void testAZeroAmountIsNotRecorded()
    {
        var conversions = new ConversionRecorder();
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, CurrencyUnit.EUR));

        converter.convert(DATE, Money.of("ARS", 0));

        assertThat(conversions.pairs(), is(empty()));
    }

    /** an instrument's local figures are converted with a derived converter, which records too */
    @Test
    public void testADerivedConverterRecordsIntoTheSameSet()
    {
        var conversions = new ConversionRecorder();
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, CurrencyUnit.EUR));

        converter.with("ARS").convert(DATE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)));

        assertThat(conversions.pairs(), contains(new Pair("EUR", "ARS")));
        assertThat(converter.with(CurrencyUnit.EUR) == converter, is(true));
    }

    /** each pair once, in a stable order, however often and in whatever order it was converted */
    @Test
    public void testPairsAreDistinctAndOrdered()
    {
        var conversions = new ConversionRecorder();
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, CurrencyUnit.EUR));

        converter.convert(DATE, Money.of("USD", 100));
        converter.convert(DATE, Money.of("ARS", 100));
        converter.convert(DATE, Money.of("USD", 200));

        assertThat(conversions.pairs(), contains(new Pair("ARS", "EUR"), new Pair("USD", "EUR")));
    }

    /** the wrapper converts exactly as the converter it wraps */
    @Test
    public void testTheWrapperConvertsLikeTheOriginal()
    {
        var original = new CurrencyConverterImpl(factory, CurrencyUnit.EUR);
        var wrapped = new ConversionRecorder().wrap(original);
        var amount = Money.of("USD", Values.Amount.factorize(100));

        assertThat(wrapped.convert(DATE, amount), is(original.convert(DATE, amount)));
        assertThat(wrapped.getTermCurrency(), is(CurrencyUnit.EUR));
    }
}
