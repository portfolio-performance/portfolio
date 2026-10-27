package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;

/**
 * Remembers which currency pairs a calculation actually converted.
 * <p/>
 * Every conversion the core performs goes through
 * {@link CurrencyConverter#getRate}: the default {@code convert} methods, a
 * transaction's {@code getMonetaryAmount(converter)}, a unit sum, a valuation
 * at the market price. A converter wrapped here records each pair it is asked
 * for, so the set is exactly the money that reached the report - cash accounts,
 * dividends, fees and taxes included - without restating the core's rules about
 * when a stored exchange rate is used instead. A zero amount is never looked up
 * and is therefore never recorded.
 * <p/>
 * {@link CurrencyConverter#with} is followed: an instrument's local figures are
 * computed by a converter into the instrument's currency, and a pair that falls
 * back there distorts those figures just the same.
 */
public final class ConversionRecorder
{
    /** one conversion: amounts in {@code base} were expressed in {@code term} */
    public record Pair(String base, String term)
    {
    }

    private static final Comparator<Pair> ORDER = Comparator.comparing(Pair::base).thenComparing(Pair::term);

    // a calculation may convert from several threads
    private final Set<Pair> pairs = ConcurrentHashMap.newKeySet();

    /** a converter that converts as the given one does, and records each pair here */
    public CurrencyConverter wrap(CurrencyConverter converter)
    {
        return new Recording(converter);
    }

    /** the pairs converted so far, in a stable order */
    public List<Pair> pairs()
    {
        return pairs.stream().sorted(ORDER).toList();
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
                pairs.add(new Pair(currencyCode, term));
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
