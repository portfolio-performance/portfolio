package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;

/**
 * Core's converter, made to say when it found no rate. Core's own converter
 * falls back to a rate of 1.0 without a word, so the figures core calculates
 * with it look plausible but are wrong. This one passes on core's rates, and
 * checks each rate core asks for against the CLI's own rate source, which notes
 * a missing or a stale one for the warnings of {@link ExchangeRates}. After
 * the calculation, {@link #isComplete()} says whether every rate was found; if
 * not, the figures must not be reported. A day after today is checked as
 * today, as no rate is dated later.
 */
final class StrictConverter implements CurrencyConverter
{
    private record Need(LocalDate day, String fromCurrency, String toCurrency)
    {
    }

    private final ExchangeRates rates;
    private final CurrencyConverter core;
    private final LocalDate today;

    // shared with the converters derived by with(), so that one answer covers them all
    private final Map<Need, Boolean> found;
    private final Set<Need> unrated;

    StrictConverter(ExchangeRates rates, CurrencyConverter core, LocalDate today)
    {
        this(rates, core, today, new HashMap<>(), new HashSet<>());
    }

    private StrictConverter(ExchangeRates rates, CurrencyConverter core, LocalDate today, Map<Need, Boolean> found,
                    Set<Need> unrated)
    {
        this.rates = rates;
        this.core = core;
        this.today = today;
        this.found = found;
        this.unrated = unrated;
    }

    /** Whether core found a rate for everything it converted since the start or the last {@link #reset()}. */
    boolean isComplete()
    {
        return unrated.isEmpty();
    }

    /**
     * Starts judging a new calculation on its own: the figures of one record
     * do not depend on the rates another record lacked. A rate already looked
     * up is not looked up, and so not warned about, again.
     */
    void reset()
    {
        unrated.clear();
    }

    @Override
    public String getTermCurrency()
    {
        return core.getTermCurrency();
    }

    /** Core's rate; where there is none, core's 1.0, which the caller must not report (see {@link #isComplete()}). */
    @Override
    public ExchangeRate getRate(LocalDate date, String currencyCode)
    {
        var term = getTermCurrency();
        if (!term.equals(currencyCode))
        {
            var need = new Need(date.isAfter(today) ? today : date, currencyCode, term);
            if (!found.computeIfAbsent(need, n -> rates.series(n.day(), currencyCode, term).isPresent()))
                unrated.add(need);
        }
        return core.getRate(date, currencyCode);
    }

    @Override
    public CurrencyConverter with(String currencyCode)
    {
        if (currencyCode.equals(getTermCurrency()))
            return this;
        return new StrictConverter(rates, core.with(currencyCode), today, found, unrated);
    }
}
