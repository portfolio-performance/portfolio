package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

/**
 * Records which currency pairs a calculation converted at 1:1 because there was
 * nothing to convert with - decided for each conversion, at the date it was
 * made.
 * <p/>
 * Every conversion the core performs goes through
 * {@link CurrencyConverter#getRate}: the default {@code convert} methods, a
 * transaction's {@code getMonetaryAmount(converter)}, a unit sum, a valuation
 * at the market price. A converter wrapped here asks, for exactly the pair and
 * the date it is asked for, whether the converter will fall back: the pair has
 * no series at all (the factory substitutes a placeholder that answers 1), or
 * the series answers nothing for that date. Whether a series answers depends on
 * the date - a series built from an exchange-rate security has no rate on a day
 * whose quote is zero - so the question cannot be asked once, afterwards, for
 * another date.
 * <p/>
 * What is recorded is therefore exactly the money that reached the report at
 * face value - cash accounts, dividends, fees and taxes included - and nothing
 * else. A zero amount is never looked up and is never recorded.
 * {@link CurrencyConverter#with} is followed: an instrument's local figures are
 * computed by a converter into the instrument's currency, and a pair that falls
 * back there distorts those figures just the same.
 */
public final class ConversionRecorder
{
    /** one conversion: amounts in {@code base} were expressed in {@code term} */
    private record Pair(String base, String term)
    {
    }

    private static final Comparator<FxWarnings.Warning> ORDER = Comparator.comparing(FxWarnings.Warning::currency)
                    .thenComparing(FxWarnings.Warning::reportingCurrency);

    private final ExchangeRateProviderFactory factory;

    // a calculation may convert from several threads
    private final Map<Pair, FxWarnings.Warning> fallbacks = new ConcurrentHashMap<>();

    /**
     * @param factory
     *            the factory the wrapped converters look their rates up in
     */
    public ConversionRecorder(ExchangeRateProviderFactory factory)
    {
        this.factory = factory;
    }

    /** a converter that converts as the given one does, and records each fallback here */
    public CurrencyConverter wrap(CurrencyConverter converter)
    {
        return new Recording(converter);
    }

    /** the pairs that fell back at least once, each once, in a stable order */
    public List<FxWarnings.Warning> fallbacks()
    {
        return fallbacks.values().stream().sorted(ORDER).toList();
    }

    private void check(String base, String term, LocalDate date)
    {
        var pair = new Pair(base, term);
        if (fallbacks.containsKey(pair))
            return;

        // the same two cases in which CurrencyConverterImpl substitutes a rate of 1
        if (!factory.hasTimeSeries(base, term))
        {
            fallbacks.putIfAbsent(pair, new FxWarnings.Warning(base, term, FxWarnings.NO_SERIES));
            return;
        }

        if (factory.getTimeSeries(base, term).lookupRate(date).isEmpty())
            fallbacks.putIfAbsent(pair, new FxWarnings.Warning(base, term, FxWarnings.NO_RATE));
    }

    private final class Recording implements CurrencyConverter
    {
        private final CurrencyConverter delegate;

        private Recording(CurrencyConverter delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public String getTermCurrency()
        {
            return delegate.getTermCurrency();
        }

        @Override
        public ExchangeRate getRate(LocalDate date, String currencyCode)
        {
            var term = delegate.getTermCurrency();
            if (!term.equals(currencyCode))
                check(currencyCode, term, date);
            return delegate.getRate(date, currencyCode);
        }

        @Override
        public CurrencyConverter with(String currencyCode)
        {
            var other = delegate.with(currencyCode);
            return other == delegate ? this : new Recording(other);
        }
    }
}
