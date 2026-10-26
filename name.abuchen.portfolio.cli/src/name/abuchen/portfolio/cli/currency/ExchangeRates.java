package name.abuchen.portfolio.cli.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.cli.command.WarningSpec;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.FixedExchangeRateTimeSeries;

/**
 * The CLI's exchange-rate source for one command run (ADR 0005), behind
 * {@link ReportingCurrency}. Beside the rates a transaction carries itself,
 * which the command reads, it offers the rates core knows from exchange-rate
 * instruments in the client file and the desktop app's ECB cache, read-only.
 * Core's built-in ECB snapshot and its silent fallback rate of 1.0 are never
 * used.
 * <p>
 * Core labels a chained rate with the requested date and answers a date before
 * the first rate with that later rate. This helper therefore looks up every step
 * of a chain itself: a step counts only if it is dated on or before the day
 * needed, and the rate date is the oldest step's date.
 * <p>
 * The cache is read lazily, on the first lookup that needs a rate series. A
 * command collects the missing and stale rates it meets and reports them, once
 * per currency pair, through {@link #warnings()}.
 */
final class ExchangeRates
{
    /** Where a rate comes from. */
    enum Source
    {
        IDENTITY, TRANSACTION, INSTRUMENT, ECB, FIXED;

        /** The name published in the output. */
        String id()
        {
            return name().toLowerCase(Locale.ROOT);
        }

        static List<String> ids()
        {
            return Arrays.stream(values()).map(Source::id).toList();
        }
    }

    static final WarningSpec MISSING = new WarningSpec("exchange-rate-missing",
                    "No usable exchange rate for a currency pair; its figures are left out. Carries baseCurrency, termCurrency and the count of affected items.");

    static final WarningSpec STALE = new WarningSpec("stale-exchange-rate",
                    "An exchange rate over " + Prices.STALE_AFTER_DAYS
                                    + " days older than the day needed was used. Carries baseCurrency, termCurrency, the count of records and maxAgeDays.");

    static final WarningSpec CACHE_UNAVAILABLE = new WarningSpec("exchange-rate-cache-unavailable",
                    "The desktop app's ECB rate cache is missing, unreadable or invalid, so it is not used. Carries the path if known and a reason: not-configured, not-found, unreadable or unparsable.");

    private static final String CORE_ECB_PROVIDER = "name.abuchen.portfolio.money.impl.ECBExchangeRateProvider";

    /**
     * A rate: the amount in the base currency times the value gives the amount
     * in the term currency.
     *
     * @param date
     *            the oldest date of the rates behind the value
     * @param source
     *            where the rate comes from
     */
    record Rate(BigDecimal value, LocalDate date, Source source)
    {
        /** Whether the rate is more than the stale threshold older than the day it was needed. */
        boolean isStale(LocalDate day)
        {
            return date.isBefore(day.minusDays(Prices.STALE_AFTER_DAYS));
        }

        long ageDays(LocalDate day)
        {
            return ChronoUnit.DAYS.between(date, day);
        }

        /** The amount in minor units in the term currency, rounded like core's currency converter. */
        long convert(long amount)
        {
            return BigDecimal.valueOf(amount).multiply(value).setScale(0, RoundingMode.HALF_DOWN).longValue();
        }
    }

    private record Step(ExchangeRateTimeSeries series, boolean inverted)
    {
    }

    private record Pair(String base, String term)
    {
    }

    /** The records of one currency pair that a warning is about, and the greatest age of the rate used for them. */
    private static final class Affected
    {
        private int count;
        private long maxAgeDays;
    }

    private final class Factory extends ExchangeRateProviderFactory
    {
        private Factory(Client client)
        {
            super(client);
        }

        /** Core's series, but with the cache of the desktop app in place of its built-in ECB data. */
        @Override
        public List<ExchangeRateTimeSeries> getAvailableTimeSeries()
        {
            var series = new ArrayList<ExchangeRateTimeSeries>();
            for (var provider : ExchangeRateProviderFactory.getProviders())
            {
                if (CORE_ECB_PROVIDER.equals(provider.getClass().getName()))
                    series.addAll(cacheSeries());
                else
                    series.addAll(provider.getAvailableTimeSeries(client));
            }
            return series;
        }
    }

    private final CommandContext context;
    private final Client client;
    private ExchangeRateProviderFactory factory;

    private boolean cacheTried;
    private EcbCache.Loaded cache;
    private EcbCache.Unavailable cacheFailure;

    private final Map<Pair, Affected> missing = new LinkedHashMap<>();
    private final Map<Pair, Affected> stale = new LinkedHashMap<>();

    ExchangeRates(CommandContext context, Client client)
    {
        this.context = context;
        this.client = client;
    }

    /**
     * Core's own converter over the CLI's rate source, for the figures core
     * calculates itself. Core falls back to a rate of 1.0 where it finds none,
     * so callers use it only where the currencies match or a rate is certain,
     * or wrap it in a {@link StrictConverter}.
     */
    CurrencyConverter converter(String termCurrency)
    {
        if (factory == null)
            factory = new Factory(client);
        return new CurrencyConverterImpl(factory, termCurrency);
    }

    /**
     * The rate for a day: the identity for the same currency, otherwise the
     * rate series. Notes a missing rate, or a stale one, for the warnings.
     */
    Optional<Rate> lookup(LocalDate day, String fromCurrency, String toCurrency)
    {
        if (fromCurrency.equals(toCurrency))
            return Optional.of(new Rate(BigDecimal.ONE, day, Source.IDENTITY));

        return series(day, fromCurrency, toCurrency);
    }

    /** The rate from the rate series alone, noting a missing or stale one. */
    Optional<Rate> series(LocalDate day, String fromCurrency, String toCurrency)
    {
        var found = find(day, fromCurrency, toCurrency);
        found.ifPresentOrElse(rate -> noteUsed(fromCurrency, toCurrency, rate, day),
                        () -> noteMissing(fromCurrency, toCurrency));
        return found;
    }

    private List<ExchangeRateTimeSeries> cacheSeries()
    {
        loadCache();
        return cache == null ? List.of() : List.copyOf(cache.series());
    }

    private void loadCache()
    {
        if (cacheTried)
            return;
        cacheTried = true;

        try
        {
            var path = EcbCache.locate(context::env, context.osName());
            cache = EcbCache.read(path);
        }
        catch (EcbCache.Unavailable e)
        {
            cacheFailure = e;
        }
    }

    /**
     * The rate from one currency to another on a day, from exchange-rate
     * instruments and the ECB cache. Empty if some step of the chain has no rate
     * dated on or before the day, or if there is no path at all.
     */
    Optional<Rate> find(LocalDate day, String fromCurrency, String toCurrency)
    {
        var steps = new ArrayList<Step>();
        if (factory == null)
            factory = new Factory(client);

        if (!flatten(factory.getTimeSeries(fromCurrency, toCurrency), false, steps))
            return Optional.empty();

        var value = BigDecimal.ONE;
        LocalDate oldest = null;
        var fromCache = false;
        var fromInstrument = false;

        for (var step : steps)
        {
            var found = step.series().lookupRate(day);
            if (found.isEmpty() || found.get().getTime().isAfter(day))
                return Optional.empty();

            var stepValue = found.get().getValue();
            if (step.inverted())
            {
                if (stepValue.signum() == 0)
                    return Optional.empty();
                stepValue = BigDecimal.ONE.divide(stepValue, 10, RoundingMode.HALF_DOWN);
            }

            value = value.multiply(stepValue);

            var date = found.get().getTime();
            if (oldest == null || date.isBefore(oldest))
                oldest = date;

            if (step.series() instanceof EcbCache.Series)
                fromCache = true;
            else if (!(step.series() instanceof FixedExchangeRateTimeSeries))
                fromInstrument = true;
        }

        return Optional.of(new Rate(value, oldest, fromCache ? Source.ECB : fromInstrument ? Source.INSTRUMENT : Source.FIXED));
    }

    /**
     * Unwraps the composition core builds: an inverse wraps one series, a
     * chain several, and an original series wraps none. A series without a
     * provider is core's placeholder for a currency pair with no path.
     */
    private static boolean flatten(ExchangeRateTimeSeries series, boolean inverted, List<Step> steps)
    {
        var parts = series.getComposition();

        if (parts.isEmpty())
        {
            if (series.getProvider().isEmpty())
                return false;
            steps.add(new Step(series, inverted));
            return true;
        }

        if (parts.size() == 1)
            return flatten(parts.get(0), !inverted, steps);

        var ordered = new ArrayList<>(parts);
        if (inverted)
            Collections.reverse(ordered);

        for (var part : ordered)
        {
            if (!flatten(part, inverted, steps))
                return false;
        }
        return true;
    }

    /** Records that no rate was found for a record of the given pair. */
    private void noteMissing(String baseCurrency, String termCurrency)
    {
        missing.computeIfAbsent(new Pair(baseCurrency, termCurrency), p -> new Affected()).count++;
    }

    /** Records that a rate was used, and warns if it is stale for the day it was needed. */
    private void noteUsed(String baseCurrency, String termCurrency, Rate rate, LocalDate day)
    {
        if (!rate.isStale(day))
            return;

        var affected = stale.computeIfAbsent(new Pair(baseCurrency, termCurrency), p -> new Affected());
        affected.count++;
        affected.maxAgeDays = Math.max(affected.maxAgeDays, rate.ageDays(day));
    }

    /** Adds the meta member <code>exchangeRates</code>, if the cache was read. */
    void putCacheInto(Map<String, Object> meta)
    {
        if (cache == null)
            return;

        var json = new LinkedHashMap<String, Object>();
        json.put("path", cache.path().toString());
        cache.latestRateDate().ifPresent(date -> json.put("latestRateDate", date.toString()));
        meta.put("exchangeRates", json);
    }

    /**
     * The warnings for everything noted so far: the unavailable cache if it was
     * needed, then each pair with missing rates, then each with stale ones.
     */
    List<Warning> warnings()
    {
        var warnings = new ArrayList<Warning>();

        if (cacheFailure != null)
        {
            var warning = new Warning(CACHE_UNAVAILABLE.code(),
                            "The ECB exchange-rate cache of the desktop app is not used: " + cacheFailure.getMessage());
            cacheFailure.path().ifPresent(path -> warning.with("path", path.toString()));
            warnings.add(warning.with("reason", cacheFailure.reason()));
        }

        missing.forEach((pair, affected) -> warnings.add(new Warning(MISSING.code(),
                        "No exchange rate " + pair.base() + " to " + pair.term() + " on or before the day needed.")
                                        .with("baseCurrency", pair.base()).with("termCurrency", pair.term())
                                        .with("count", affected.count)));

        stale.forEach((pair, affected) -> warnings.add(new Warning(STALE.code(),
                        "The exchange rate " + pair.base() + " to " + pair.term() + " used is up to "
                                        + affected.maxAgeDays + " days old.").with("baseCurrency", pair.base())
                                                        .with("termCurrency", pair.term()).with("count", affected.count)
                                                        .with("maxAgeDays", affected.maxAgeDays)));

        return warnings;
    }
}
