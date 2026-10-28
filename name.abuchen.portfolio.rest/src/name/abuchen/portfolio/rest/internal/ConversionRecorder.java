package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
 * <p/>
 * The same lookup also says how old the rate was that a conversion used. A
 * series that stopped being published - a currency whose rates are no longer
 * fixed, a feed that was switched off - goes on answering with its last rate,
 * and a report converts at it without anything looking wrong: the currency
 * effect simply comes out as nothing. A rate more than
 * {@value #STALE_AFTER_DAYS} days older than the date it converted at - or than
 * today, for a date after it - is recorded as stale, with the largest gap the
 * calculation met for each pair.
 */
public final class ConversionRecorder
{
    /** one conversion: amounts in {@code base} were expressed in {@code term} */
    private record Pair(String base, String term)
    {
    }

    private static final Comparator<FxWarnings.Warning> ORDER = Comparator.comparing(FxWarnings.Warning::currency)
                    .thenComparing(FxWarnings.Warning::reportingCurrency);

    private static final Comparator<FxWarnings.StaleRate> STALE_ORDER = Comparator
                    .comparing(FxWarnings.StaleRate::currency).thenComparing(FxWarnings.StaleRate::reportingCurrency);

    /**
     * A week: longer than any weekend with holidays around it, which is the
     * gap a daily series has every week without being out of date.
     */
    /* package */ static final long STALE_AFTER_DAYS = 7;

    private final ExchangeRateProviderFactory factory;

    // a calculation may convert from several threads
    private final Map<Pair, FxWarnings.Warning> fallbacks = new ConcurrentHashMap<>();
    private final Map<Pair, FxWarnings.StaleRate> stale = new ConcurrentHashMap<>();

    /** no rate can be younger than today's, so a later date is measured from today */
    private final LocalDate today;

    /**
     * @param factory
     *            the factory the wrapped converters look their rates up in
     */
    public ConversionRecorder(ExchangeRateProviderFactory factory)
    {
        this(factory, LocalDate.now());
    }

    /* package */ ConversionRecorder(ExchangeRateProviderFactory factory, LocalDate today)
    {
        this.factory = factory;
        this.today = today;
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

    /** the pairs converted at a stale rate at least once, each with its oldest, in a stable order */
    public List<FxWarnings.StaleRate> staleRates()
    {
        return stale.values().stream().sorted(STALE_ORDER).toList();
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

        var rate = factory.getTimeSeries(base, term).lookupRate(date);
        if (rate.isEmpty())
        {
            fallbacks.putIfAbsent(pair, new FxWarnings.Warning(base, term, FxWarnings.NO_RATE));
            return;
        }

        // a rate dated after the date - the first quote answering an earlier
        // day - is the other inaccuracy, deliberately not reported
        var asOf = date.isAfter(today) ? today : date;
        var rateDate = rate.get().getTime();
        var days = ChronoUnit.DAYS.between(rateDate, asOf);
        if (days > STALE_AFTER_DAYS)
            stale.merge(pair, new FxWarnings.StaleRate(base, term, rateDate.toString(), days),
                            (kept, other) -> kept.daysOld() >= other.daysOld() ? kept : other);
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
