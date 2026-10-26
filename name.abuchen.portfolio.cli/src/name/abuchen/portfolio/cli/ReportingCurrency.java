package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
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
final class ReportingCurrency
{
    /** The warnings of a command that converts. */
    static final List<WarningSpec> WARNINGS = List.of(ExchangeRates.MISSING, ExchangeRates.STALE,
                    ExchangeRates.CACHE_UNAVAILABLE);

    /** The warnings of a command whose converter core drives, which counts days rather than records. */
    static final List<WarningSpec> WARNINGS_COUNTING_DAYS = List.of(
                    new WarningSpec(ExchangeRates.MISSING.code(),
                                    "No usable exchange rate was found for a currency pair on a day core needed it, so the figures are left out. Carries baseCurrency, termCurrency and the count of days without a rate."),
                    new WarningSpec(ExchangeRates.STALE.code(),
                                    "An exchange rate more than " + Prices.STALE_AFTER_DAYS
                                                    + " days before the day needed was used. Carries baseCurrency, termCurrency, the count of days and the maxAgeDays."),
                    ExchangeRates.CACHE_UNAVAILABLE);

    /** The currency code of <code>--currency</code>, validated before the client is loaded. */
    record Requested(Optional<String> code)
    {
        /** The currency given, otherwise the base currency of the client. */
        ReportingCurrency orBase(CommandContext context, Client client)
        {
            return new ReportingCurrency(code.orElse(client.getBaseCurrency()), context, client);
        }

        /** The currency given, if any; for a command without a default. */
        Optional<ReportingCurrency> ifGiven(CommandContext context, Client client)
        {
            return code.map(c -> new ReportingCurrency(c, context, client));
        }
    }

    /** An amount and, if one was found, the rate into the reporting currency. */
    static final class Figure
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

    private final String code;
    private final ExchangeRates rates;
    private final LocalDate today;

    private ReportingCurrency(String code, CommandContext context, Client client)
    {
        this.code = code;
        this.rates = new ExchangeRates(context, client);
        this.today = LocalDate.now(context.clock());
    }

    /** The <code>--currency</code> option, described as the command uses it. */
    static OptionSpec option(String description)
    {
        return new OptionSpec("currency", "code", description);
    }

    /**
     * The value of the option, if given.
     *
     * @throws CliException
     *             <code>invalid-option-value</code> if core doesn't know the currency
     */
    static Requested requested(CommandContext context, OptionSpec option) throws CliException
    {
        var raw = context.option(option.name());
        if (raw.isPresent() && CurrencyUnit.getInstance(raw.get()) == null)
            throw CliException.usage("invalid-option-value", "Invalid option value",
                            "Option '--" + option.name() + "' must be an ISO currency code known to core.")
                            .with("option", option.name()).with("value", raw.get());
        return new Requested(raw);
    }

    /** The meta member describing the cache, to add to a converting command's meta schema. */
    static Schema ratesMetaSchema()
    {
        return Schema.object("The ECB cache of the desktop app. Present only if the cache was read.")
                        .property("path", Schema.string("The cache file."))
                        .optionalProperty("latestRateDate",
                                        Schema.string("ISO date of the newest rate in the cache; open the desktop app to refresh it."));
    }

    /** The schema of the <code>converted</code> member of a converting command's figures. */
    static Schema convertedSchema(String description)
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
    String code()
    {
        return code;
    }

    /**
     * A value valued on a day: the identity for the same currency, otherwise
     * the rate series. Zero in another currency needs no rate, so none is
     * looked up and none is warned about.
     */
    Figure figure(LocalDate day, Money value)
    {
        if (value.isZero() && !code.equals(value.getCurrencyCode()))
            return new Figure(0, false, Optional.empty());

        return new Figure(value.getAmount(), true, rates.lookup(day, value.getCurrencyCode(), code));
    }

    /**
     * An amount of a record that may carry its own rate. Rates are tried in
     * order: the identity for the same currency, the record's own rate, then
     * the rate series.
     *
     * @param own
     *            the record's own rate from its currency into the reporting
     *            currency, if it has one
     */
    Figure figure(LocalDate day, String currencyCode, long amount, Optional<BigDecimal> own)
    {
        if (currencyCode.equals(code))
            return new Figure(amount, true, Optional.of(new ExchangeRates.Rate(BigDecimal.ONE, day, ExchangeRates.Source.IDENTITY)));

        var rate = own.map(value -> new ExchangeRates.Rate(value, day, ExchangeRates.Source.TRANSACTION))
                        .or(() -> rates.series(day, currencyCode, code));
        return new Figure(amount, true, rate);
    }

    /**
     * Whether every value has its rate, or needs none. Every value is looked
     * up, so that every missing rate is reported.
     */
    boolean allRated(LocalDate day, List<Money> values)
    {
        var figures = values.stream().map(value -> figure(day, value)).toList();
        return figures.stream().noneMatch(Figure::spoilsTotal);
    }

    /** The total of the figures, unless one of them lacks its rate. */
    Optional<Map<String, Object>> total(List<Figure> figures)
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
    void putConvertedInto(Map<String, Object> json, Figure figure)
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

    /**
     * Core's own converter into the reporting currency. It falls back to a
     * rate of 1.0 where it finds none, so it serves only figures whose rates
     * are checked another way, or none are needed.
     */
    CurrencyConverter coreConverter()
    {
        return rates.converter(code);
    }

    /** Core's converter, checked rate by rate; see {@link StrictConverter}. Days after today are checked as today. */
    StrictConverter strictConverter()
    {
        return new StrictConverter(rates, coreConverter(), today);
    }

    /** Adds the meta member <code>exchangeRates</code>, if the cache was read. */
    void putRatesInto(Map<String, Object> meta)
    {
        rates.putCacheInto(meta);
    }

    /** The warnings about the rates looked up so far; see {@link ExchangeRates#warnings()}. */
    List<Warning> warnings()
    {
        return rates.warnings();
    }
}
