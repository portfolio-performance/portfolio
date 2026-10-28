package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * The recorder sees exactly the conversions that fell back to 1:1, at the date
 * they were made - no more, so a report does not warn about money it never
 * contained or converted properly, and no less, so a fallback cannot hide.
 * <p/>
 * ARS is in the currency bundle but quoted by no built-in provider; USD and GBP
 * reach EUR through the ECB series.
 */
@SuppressWarnings("nls")
public class ConversionRecorderTest
{
    private static final LocalDate DATE = LocalDate.of(2026, 1, 2);

    private static ConversionRecorder recorder(Client client)
    {
        return new ConversionRecorder(new ExchangeRateProviderFactory(client));
    }

    private static CurrencyConverter converter(ConversionRecorder conversions, Client client, String term)
    {
        return conversions.wrap(new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), term));
    }

    @Test
    public void testAPairWithoutAnySeriesIsRecordedAsNoSeries()
    {
        var client = new Client();
        var conversions = recorder(client);

        converter(conversions, client, CurrencyUnit.EUR).convert(DATE, Money.of("ARS", Values.Amount.factorize(10)));

        assertThat(conversions.fallbacks(), contains(new FxWarnings.Warning("ARS", "EUR", "no-series")));
    }

    /** direct, inverted and chained series convert properly, so nothing is recorded */
    @Test
    public void testAPairWithRatesIsNotRecorded()
    {
        var client = new Client();
        var conversions = recorder(client);

        var converter = converter(conversions, client, CurrencyUnit.EUR);
        converter.convert(DATE, Money.of("USD", Values.Amount.factorize(10)));
        converter.convert(DATE, Money.of("GBP", Values.Amount.factorize(10)));
        converter.with("USD").convert(DATE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)));

        assertThat(conversions.fallbacks(), is(empty()));
    }

    /** an exchange-rate security without any price: the series exists, but answers no date */
    @Test
    public void testASeriesWithoutRatesIsRecordedAsNoRate()
    {
        var client = new Client();
        client.addSecurity(rates());
        var conversions = recorder(client);

        converter(conversions, client, CurrencyUnit.EUR).convert(DATE, Money.of("ARS", Values.Amount.factorize(10)));

        assertThat(conversions.fallbacks(), contains(new FxWarnings.Warning("ARS", "EUR", "no-rate")));
    }

    /**
     * Whether a series answers depends on the date: a security-backed series has
     * no rate on a day whose quote is zero. A conversion on that day falls back,
     * one on any other day does not - so the decision has to be made per
     * conversion, not once for some other date.
     */
    @Test
    public void testTheDateOfTheConversionDecides()
    {
        var client = new Client();
        var rates = rates();
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 7, 15), 0));
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 7, 25), Values.Quote.factorize(970)));
        client.addSecurity(rates);

        var onAValidQuote = recorder(client);
        converter(onAValidQuote, client, CurrencyUnit.EUR).convert(LocalDate.of(2026, 7, 30),
                        Money.of("ARS", Values.Amount.factorize(10)));
        assertThat(onAValidQuote.fallbacks(), is(empty()));

        var onAZeroQuote = recorder(client);
        converter(onAZeroQuote, client, CurrencyUnit.EUR).convert(LocalDate.of(2026, 7, 20),
                        Money.of("ARS", Values.Amount.factorize(10)));
        assertThat(onAZeroQuote.fallbacks(), contains(new FxWarnings.Warning("ARS", "EUR", "no-rate")));
    }

    /** an amount already in the term currency is not a conversion */
    @Test
    public void testAnAmountInTheTermCurrencyIsNotRecorded()
    {
        var client = new Client();
        var conversions = recorder(client);
        var converter = converter(conversions, client, "ARS");

        converter.convert(DATE, Money.of("ARS", Values.Amount.factorize(10)));
        converter.getRate(DATE, "ARS");

        assertThat(conversions.fallbacks(), is(empty()));
    }

    /** a zero amount is never looked up - an empty account in another currency converts nothing */
    @Test
    public void testAZeroAmountIsNotRecorded()
    {
        var client = new Client();
        var conversions = recorder(client);

        converter(conversions, client, CurrencyUnit.EUR).convert(DATE, Money.of("ARS", 0));

        assertThat(conversions.fallbacks(), is(empty()));
    }

    /** an instrument's local figures are converted with a derived converter, which records too */
    @Test
    public void testADerivedConverterRecordsIntoTheSameSet()
    {
        var client = new Client();
        var conversions = recorder(client);
        var converter = converter(conversions, client, CurrencyUnit.EUR);

        converter.with("ARS").convert(DATE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)));

        assertThat(conversions.fallbacks(), contains(new FxWarnings.Warning("EUR", "ARS", "no-series")));
        assertThat(converter.with(CurrencyUnit.EUR) == converter, is(true));
    }

    /** each pair once, in a stable order, however often and in whatever order it fell back */
    @Test
    public void testFallbacksAreDistinctAndOrdered()
    {
        var client = new Client();
        var conversions = recorder(client);
        var converter = converter(conversions, client, CurrencyUnit.EUR);

        converter.convert(DATE, Money.of("BBD", 100));
        converter.convert(DATE, Money.of("ARS", 100));
        converter.convert(DATE, Money.of("BBD", 200));

        assertThat(conversions.fallbacks(), contains(new FxWarnings.Warning("ARS", "EUR", "no-series"),
                        new FxWarnings.Warning("BBD", "EUR", "no-series")));
    }

    /** the wrapper converts exactly as the converter it wraps */
    @Test
    public void testTheWrapperConvertsLikeTheOriginal()
    {
        var client = new Client();
        var original = new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), CurrencyUnit.EUR);
        var wrapped = recorder(client).wrap(original);
        var amount = Money.of("USD", Values.Amount.factorize(100));

        assertThat(wrapped.convert(DATE, amount), is(original.convert(DATE, amount)));
        assertThat(wrapped.getTermCurrency(), is(CurrencyUnit.EUR));
    }

    /** an exchange-rate security ARS/EUR, without prices */
    /**
     * A series that stopped answering with new rates is used with its last
     * one: recorded as stale with the largest gap met, not as a fallback.
     */
    @Test
    public void testAConversionAtAnOldRateIsRecordedAsStale()
    {
        var client = new Client();
        var rates = rates();
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        client.addSecurity(rates);
        var conversions = new ConversionRecorder(new ExchangeRateProviderFactory(client), LocalDate.of(2026, 10, 28));

        var converter = converter(conversions, client, CurrencyUnit.EUR);
        converter.convert(LocalDate.of(2026, 3, 1), Money.of("ARS", Values.Amount.factorize(10)));
        converter.convert(LocalDate.of(2026, 7, 30), Money.of("ARS", Values.Amount.factorize(10)));
        converter.convert(LocalDate.of(2026, 5, 1), Money.of("ARS", Values.Amount.factorize(10)));

        assertThat(conversions.fallbacks(), is(empty()));
        assertThat(conversions.staleRates(),
                        contains(new FxWarnings.StaleRate("ARS", "EUR", "2026-01-02", 209)));
    }

    /** a week-old rate is a weekend with holidays, not a stale one */
    @Test
    public void testARecentRateIsNotStale()
    {
        var client = new Client();
        var rates = rates();
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        client.addSecurity(rates);
        var conversions = new ConversionRecorder(new ExchangeRateProviderFactory(client), LocalDate.of(2026, 10, 28));

        converter(conversions, client, CurrencyUnit.EUR).convert(LocalDate.of(2026, 1, 9),
                        Money.of("ARS", Values.Amount.factorize(10)));

        assertThat(conversions.staleRates(), is(empty()));
    }

    /** no rate can be younger than today's: a later date is measured from today */
    @Test
    public void testAFutureDateIsMeasuredFromToday()
    {
        var client = new Client();
        var rates = rates();
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        client.addSecurity(rates);

        var fresh = new ConversionRecorder(new ExchangeRateProviderFactory(client), LocalDate.of(2026, 1, 5));
        converter(fresh, client, CurrencyUnit.EUR).convert(LocalDate.of(2030, 12, 31),
                        Money.of("ARS", Values.Amount.factorize(10)));
        assertThat(fresh.staleRates(), is(empty()));

        var old = new ConversionRecorder(new ExchangeRateProviderFactory(client), LocalDate.of(2026, 2, 2));
        converter(old, client, CurrencyUnit.EUR).convert(LocalDate.of(2030, 12, 31),
                        Money.of("ARS", Values.Amount.factorize(10)));
        assertThat(old.staleRates(), contains(new FxWarnings.StaleRate("ARS", "EUR", "2026-01-02", 31)));
    }

    private static Security rates()
    {
        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        return rates;
    }
}
