package name.abuchen.portfolio.cli.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.cli.command.WarningSpec;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * The one currency a command states totals and converted amounts in (ADR
 * 0005): the one given with <code>--currency</code>, otherwise the base
 * currency of the file where the command has that default. It turns amounts
 * into figures with the rate behind them, says which totals can be stated, and
 * reports the <code>exchangeRates</code> meta member and the rate warnings.
 * The rates themselves come from {@link ExchangeRates}.
 */
public final class ReportingCurrency
{
    /** What the counts of a command's rate warnings count. */
    public enum Counting
    {
        /** The figures the command converts itself: records or holdings. */
        ITEMS,
        /** The days core needed a rate on, for figures core calculates itself. */
        DAYS
    }

    private static final List<WarningSpec> WARNINGS_COUNTING_ITEMS = List.of(ExchangeRates.MISSING,
                    ExchangeRates.STALE, ExchangeRates.CACHE_UNAVAILABLE);

    private static final List<WarningSpec> WARNINGS_COUNTING_DAYS = List.of(
                    new WarningSpec(ExchangeRates.MISSING.code(),
                                    "No usable exchange rate was found for a currency pair on a day core needed it, so the figures are left out. Carries baseCurrency, termCurrency and the count of days without a rate."),
                    new WarningSpec(ExchangeRates.STALE.code(),
                                    "An exchange rate more than " + Prices.STALE_AFTER_DAYS
                                                    + " days before the day needed was used. Carries baseCurrency, termCurrency, the count of days and the maxAgeDays."),
                    ExchangeRates.CACHE_UNAVAILABLE);

    private static final String META_CURRENCY = "currency";
    private static final String META_RATES = "exchangeRates";

    /**
     * <code>--currency</code> with the base currency of the file as its
     * default. <code>meta</code> gets the currency and, if the cache was read,
     * <code>exchangeRates</code>; the rate warnings follow the handler.
     *
     * @param description
     *            the option as the command uses it
     */
    public static Facet<ReportingCurrency> facet(String description, Counting counting)
    {
        var option = OptionSpec.currency("currency", description);
        var declaration = declaration(option, counting)
                        .meta(META_CURRENCY, Schema.string("The reporting currency: as given, otherwise the base currency of the file."))
                        .optionalMeta(META_RATES, ratesMetaSchema());
        return Facet.of(declaration, run -> {
            var client = run.client();
            return of(run.currency(option).orElse(client.getBaseCurrency()), client, run);
        }, (reporting, out) -> reporting.report(out));
    }

    /**
     * <code>--currency</code> without a default, for a command that converts
     * only if asked to. If it is given, <code>meta</code> and the warnings are
     * those of {@link #facet}, counting items.
     */
    public static Facet<Optional<ReportingCurrency>> ifGiven(String description)
    {
        var option = OptionSpec.currency("currency", description);
        var declaration = declaration(option, Counting.ITEMS)
                        .optionalMeta(META_CURRENCY, Schema.string("The currency asked for, as given."))
                        .optionalMeta(META_RATES, ratesMetaSchema());
        return Facet.of(declaration, run -> {
            var code = run.currency(option);
            if (code.isEmpty())
                return Optional.<ReportingCurrency>empty();
            return Optional.of(of(code.get(), run.client(), run));
        }, (reporting, out) -> reporting.ifPresent(currency -> currency.report(out)));
    }

    private static Declaration declaration(OptionSpec option, Counting counting)
    {
        var warnings = counting == Counting.ITEMS ? WARNINGS_COUNTING_ITEMS : WARNINGS_COUNTING_DAYS;
        return Declaration.empty().options(option).warnings(warnings.toArray(WarningSpec[]::new)).needingClient();
    }

    private static ReportingCurrency of(String code, Client client, CommandContext run)
    {
        return new ReportingCurrency(code, client, RateCache.desktop(run::env, run.osName()),
                        LocalDate.now(run.clock()));
    }

    private void report(Facet.Report out)
    {
        out.meta(META_CURRENCY, code);
        var meta = new LinkedHashMap<String, Object>();
        rates.putCacheInto(meta);
        if (meta.containsKey(META_RATES))
            out.meta(META_RATES, meta.get(META_RATES));
        rates.warnings().forEach(out::warn);
    }

    /**
     * The figures core calculates with its own converter. Core falls back to a
     * rate of 1.0 where it finds none (ADR 0002), so a figure is reported only
     * through a trust call, which checks every rate core asked for while
     * calculating it. The supplier of a trust call must return finished data:
     * core calculates lazily, so a figure read later would escape the check.
     */
    public final class CoreFigures
    {
        private final StrictConverter strict = new StrictConverter(rates, rates.converter(code), today);

        private CoreFigures()
        {
        }

        /** The converter to hand core for the figures of a trust call. */
        public CurrencyConverter converter()
        {
            return strict;
        }

        /**
         * The figures, unless core missed a rate while the supplier ran. The
         * missing and stale rates are warned about, counting days.
         */
        public <T> Optional<T> trusted(Supplier<T> figures)
        {
            return strict.trusted(false, figures, new HashSet<>());
        }

        /**
         * The figures, if every item has its rate on the day and core found
         * every rate it asked for while the supplier ran. Every item is looked
         * up, so the warnings count items; the rates core asks for stay out of
         * them, except a pair only core missed, which is warned about once.
         */
        public <T> Optional<T> trusted(LocalDate day, List<Money> items, Supplier<T> figures)
        {
            var itemMissing = new HashSet<List<String>>();
            for (var item : items)
            {
                if (figure(day, item).spoilsTotal())
                    itemMissing.add(List.of(item.getCurrencyCode(), code));
            }

            var unrated = new HashSet<StrictConverter.Need>();
            var result = strict.trusted(true, figures, unrated);

            unrated.stream().map(need -> List.of(need.fromCurrency(), need.toCurrency())).distinct()
                            .filter(pair -> !itemMissing.contains(pair)).sorted(BY_PAIR)
                            .forEach(pair -> rates.noteMissing(pair.get(0), pair.get(1)));

            return itemMissing.isEmpty() ? result : Optional.empty();
        }

        /**
         * Core's own converter for core runs whose figures are never reported.
         * It tolerates core's fallback rate of 1.0 and warns about nothing:
         * this is the one place that does.
         */
        public CurrencyConverter unreported()
        {
            return rates.converter(code);
        }
    }

    /** An amount and, if one was found, the rate into the reporting currency. */
    public static final class Figure
    {
        private final long amount;
        private final boolean needsRate;
        private final Optional<ExchangeRates.Rate> rate;

        private Figure(long amount, boolean needsRate, Optional<ExchangeRates.Rate> rate)
        {
            this.amount = amount;
            this.needsRate = needsRate;
            this.rate = rate;
        }

        /** Nothing to convert needs no rate, so a missing one does not spoil a total. */
        private boolean spoilsTotal()
        {
            return needsRate && rate.isEmpty();
        }

        private long converted()
        {
            return rate.map(r -> r.convert(amount)).orElse(0L);
        }
    }

    private static final Comparator<List<String>> BY_PAIR = Comparator.comparing((List<String> pair) -> pair.get(0))
                    .thenComparing(pair -> pair.get(1));

    private final String code;
    private final ExchangeRates rates;
    private final LocalDate today;

    /**
     * @param code
     *            the ISO code of the reporting currency
     * @param cache
     *            the ECB rates, read on the first lookup that needs them
     * @param today
     *            no rate is dated later, so a later day is checked as today
     */
    public ReportingCurrency(String code, Client client, RateCache cache, LocalDate today)
    {
        this.code = code;
        this.rates = new ExchangeRates(client, cache);
        this.today = today;
    }

    /**
     * The meta member describing the cache. Transitional: public only for the
     * performance command, until it is composed of facets.
     */
    public static Schema ratesMetaSchema()
    {
        return Schema.object("The ECB cache of the desktop app. Present only if the cache was read.")
                        .property("path", Schema.string("The cache file."))
                        .optionalProperty("latestRateDate",
                                        Schema.string("ISO date of the newest rate in the cache; open the desktop app to refresh it."));
    }

    /** The schema of the <code>converted</code> member of a converting command's figures. */
    public static Schema convertedSchema(String description)
    {
        return Schema.object(description)
                        .property("currencyCode", Schema.string("The ISO currency code asked for."))
                        .property("amount", Schema.number("The amount converted, rounded to minor units like core does."))
                        .property("rate", Schema.number("Unrounded factor from the native to the converted currency."))
                        .property("rateDate", Schema.string("ISO date of the rate; of the oldest if chained."))
                        .property("rateSource", Schema.enumOf(
                                        "Where the rate comes from: transaction is the record's own forex unit or transfer rate, instrument an exchange-rate instrument of the file, ecb the desktop app's ECB cache, fixed one of core's fixed pairs such as GBX to GBP.",
                                        ExchangeRates.Source.ids()));
    }

    /** The ISO code of the reporting currency. */
    public String code()
    {
        return code;
    }

    /**
     * A value valued on a day: the identity for the same currency, otherwise
     * the rate series. Zero in another currency needs no rate, so none is
     * looked up and none is warned about.
     */
    public Figure figure(LocalDate day, Money value)
    {
        if (value.isZero() && !code.equals(value.getCurrencyCode()))
            return new Figure(0, false, Optional.empty());

        return new Figure(value.getAmount(), true, rates.lookup(day, value.getCurrencyCode(), code));
    }

    /**
     * An amount of a record that may carry its own rate. Rates are tried in
     * order: the identity for the same currency, the record's own rate, then
     * the rate series. It takes a code and an amount rather than a
     * {@link Money}, which refuses the empty code a record without a currency
     * is listed with.
     *
     * @param own
     *            the record's own rate from its currency into the reporting
     *            currency, if it has one
     */
    public Figure figure(LocalDate day, String currencyCode, long amount, Optional<BigDecimal> own)
    {
        if (currencyCode.equals(code))
            return new Figure(amount, true, Optional.of(new ExchangeRates.Rate(BigDecimal.ONE, day, ExchangeRates.Source.IDENTITY)));

        var rate = own.map(value -> new ExchangeRates.Rate(value, day, ExchangeRates.Source.TRANSACTION))
                        .or(() -> rates.series(day, currencyCode, code));
        return new Figure(amount, true, rate);
    }

    /** The total of the figures, unless one of them lacks its rate. */
    public Optional<Map<String, Object>> total(List<Figure> figures)
    {
        if (figures.stream().anyMatch(Figure::spoilsTotal))
            return Optional.empty();

        var sum = figures.stream().mapToLong(Figure::converted).sum();
        var json = new LinkedHashMap<String, Object>();
        json.put("currencyCode", code);
        json.put("amount", Decimals.of(sum, Values.Amount));
        return Optional.of(json);
    }

    /** Adds the member <code>converted</code>, if the figure has its rate. */
    public void putConvertedInto(Map<String, Object> json, Figure figure)
    {
        figure.rate.ifPresent(rate -> {
            var converted = new LinkedHashMap<String, Object>();
            converted.put("currencyCode", code);
            converted.put("amount", Decimals.of(figure.converted(), Values.Amount));
            converted.put("rate", Decimals.of(rate.value()));
            converted.put("rateDate", rate.date().toString());
            converted.put("rateSource", rate.source().id());
            json.put("converted", converted);
        });
    }

    /** The converters for the figures core calculates itself; see {@link CoreFigures}. */
    public CoreFigures coreFigures()
    {
        return new CoreFigures();
    }

    /**
     * Adds the meta member <code>exchangeRates</code>, if the cache was read.
     * Transitional: for the performance command, until it is composed of
     * facets.
     */
    public void putRatesInto(Map<String, Object> meta)
    {
        rates.putCacheInto(meta);
    }

    /**
     * The warnings about the rates looked up so far; see
     * {@link ExchangeRates#warnings()}. Transitional: for the performance
     * command, until it is composed of facets.
     */
    public List<Warning> warnings()
    {
        return rates.warnings();
    }
}
