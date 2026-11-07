package name.abuchen.portfolio.cli.currency;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;

/**
 * Core's converter, made to say when it found no rate. Core's own converter
 * falls back to a rate of 1.0 without a word, so the figures core calculates
 * with it look plausible but are wrong. This one passes on core's rates, and
 * checks each rate core asks for against the CLI's own rate source. A
 * {@link #trusted} call judges the figures one supplier calculates: if a rate
 * was missing, they must not be reported. A day after today is checked as
 * today, as no rate is dated later.
 * <p>
 * Outside a silent call, the rate source notes a missing or a stale rate for
 * the warnings of {@link ExchangeRates}, once per day and pair.
 */
final class StrictConverter implements CurrencyConverter
{
    /** A rate core asked for: from a currency into another on a day. */
    record Need(LocalDate day, String fromCurrency, String toCurrency)
    {
    }

    /** What the converters derived by with() share, so that one answer covers them all. */
    private static final class State
    {
        private final Map<Need, Boolean> found = new HashMap<>();
        private final Set<Need> unrated = new LinkedHashSet<>();
        private boolean silent;
    }

    private final ExchangeRates rates;
    private final CurrencyConverter core;
    private final LocalDate today;
    private final State state;

    StrictConverter(ExchangeRates rates, CurrencyConverter core, LocalDate today)
    {
        this(rates, core, today, new State());
    }

    private StrictConverter(ExchangeRates rates, CurrencyConverter core, LocalDate today, State state)
    {
        this.rates = rates;
        this.core = core;
        this.today = today;
        this.state = state;
    }

    /**
     * The figures of the supplier, judged on their own: only the rates asked
     * for while it runs count. Empty if one of them was missing.
     *
     * @param silent
     *            whether the rates asked for stay out of the warnings
     * @param unrated
     *            receives the rates that were missing
     */
    <T> Optional<T> trusted(boolean silent, Supplier<T> figures, Set<Need> unrated)
    {
        state.unrated.clear();
        state.silent = silent;
        try
        {
            var result = figures.get();
            unrated.addAll(state.unrated);
            return state.unrated.isEmpty() ? Optional.of(result) : Optional.empty();
        }
        finally
        {
            state.silent = false;
            state.unrated.clear();
        }
    }

    @Override
    public String getTermCurrency()
    {
        return core.getTermCurrency();
    }

    /** Core's rate; where there is none, core's 1.0, which a trusted call does not report. */
    @Override
    public ExchangeRate getRate(LocalDate date, String currencyCode)
    {
        var term = getTermCurrency();
        if (!term.equals(currencyCode))
        {
            var need = new Need(date.isAfter(today) ? today : date, currencyCode, term);
            if (!state.found.computeIfAbsent(need, this::look))
                state.unrated.add(need);
        }
        return core.getRate(date, currencyCode);
    }

    private boolean look(Need need)
    {
        return (state.silent ? rates.find(need.day(), need.fromCurrency(), need.toCurrency())
                        : rates.series(need.day(), need.fromCurrency(), need.toCurrency())).isPresent();
    }

    @Override
    public CurrencyConverter with(String currencyCode)
    {
        if (currencyCode.equals(getTermCurrency()))
            return this;
        return new StrictConverter(rates, core.with(currencyCode), today, state);
    }
}
