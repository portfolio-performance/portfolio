package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

/**
 * <code>performance</code>: how the whole client did over a period. The
 * figures are core's performance index (IRR, TTWROR) and its performance
 * snapshot (gains), converted by core's own converter over the CLI's rate
 * source; core decides which rate it uses on which day. Core's converter falls
 * back to 1.0 without a word where it finds no rate, so a {@link StrictConverter}
 * checks every rate core asks for. If one is missing, no figure is reported
 * (ADR 0005); a stale rate is used and warned about.
 */
final class PerformanceCommand
{
    private static final OptionSpec CURRENCY = new OptionSpec("currency", "code",
                    "ISO currency of the figures. Defaults to the base currency of the file.");

    private static final String LEFT_OUT = " Absent if an exchange rate is missing.";

    /** A value or gain of the performance snapshot, as the output names it. */
    private record Figure(String key, CategoryType category, String description)
    {
    }

    private static final List<Figure> FIGURES = List.of(
                    new Figure("initialValue", CategoryType.INITIAL_VALUE,
                                    "Value of the client at the start of the period."),
                    new Figure("finalValue", CategoryType.FINAL_VALUE,
                                    "Value of the client at the end of the period."),
                    new Figure("capitalGains", CategoryType.CAPITAL_GAINS,
                                    "Unrealized capital gains of the period, by the cost method."),
                    new Figure("realizedCapitalGains", CategoryType.REALIZED_CAPITAL_GAINS,
                                    "Realized capital gains of the period, by the cost method."),
                    new Figure("earnings", CategoryType.EARNINGS, "Dividends and interest, net of interest charges."),
                    new Figure("fees", CategoryType.FEES, "Fees paid."),
                    new Figure("taxes", CategoryType.TAXES, "Taxes paid."),
                    new Figure("currencyGains", CategoryType.CURRENCY_GAINS,
                                    "Gains and losses from changes of exchange rates."),
                    new Figure("transfers", CategoryType.TRANSFERS,
                                    "Deposits less removals, and deliveries in less deliveries out."));

    private static Schema reportingAmount(String description)
    {
        return Schema.number(description + " In the reporting currency." + LEFT_OUT);
    }

    private static Schema dataSchema()
    {
        var data = Schema.object(
                        "How the client performed over the period. Every figure but the currency is left out if an exchange rate is missing, with the warning exchange-rate-missing.")
                        .property("currencyCode", Schema.string("The reporting currency of every amount."))
                        .optionalProperty("irr", Schema.number(
                                        "Internal rate of return over the period, annualized, as a fraction: 0.05 is 5 %. Absent if core finds none."
                                                        + LEFT_OUT))
                        .optionalProperty("ttwror", Schema.number(
                                        "True time-weighted rate of return over the period, not annualized, as a fraction: 0.05 is 5 %."
                                                        + LEFT_OUT))
                        .optionalProperty("absoluteGain", reportingAmount(
                                        "Capital gains, realized capital gains, earnings and currency gains, less fees and taxes."));
        FIGURES.forEach(figure -> data.optionalProperty(figure.key(), reportingAmount(figure.description())));
        return data;
    }

    private static final Schema DATA = dataSchema();

    private static final Schema META = PerformancePeriod.declareIn(ResponseMeta.fileSchema())
                    .property("costMethod", CostMethod.schema())
                    .property("currency", Schema.string("The reporting currency: as given, otherwise the base currency."))
                    .optionalProperty("exchangeRates", ExchangeRates.metaSchema());

    /** The warnings of <code>performance</code>: those of {@link ExchangeRates}, with a count of days. */
    private static final List<WarningSpec> WARNINGS = List.of(
                    new WarningSpec(ExchangeRates.MISSING.code(),
                                    "No usable exchange rate was found for a currency pair on a day core needed it, so the figures are left out. Carries baseCurrency, termCurrency and the count of days without a rate."),
                    new WarningSpec(ExchangeRates.STALE.code(),
                                    "An exchange rate more than " + Prices.STALE_AFTER_DAYS
                                                    + " days before the day needed was used. Carries baseCurrency, termCurrency, the count of days and the maxAgeDays."),
                    ExchangeRates.CACHE_UNAVAILABLE);

    static final CommandSpec SPEC = new CommandSpec("performance",
                    "IRR, TTWROR and gains of the whole client over a period, given as --from and --to or as a reporting-period --period code, by FIFO or moving-average cost, in the base currency or --currency. The figures are left out if an exchange rate is missing.",
                    true, options(), DATA, META, WARNINGS, PerformanceCommand::execute);

    private PerformanceCommand()
    {
    }

    private static List<OptionSpec> options()
    {
        var options = new ArrayList<>(PerformancePeriod.OPTIONS);
        options.add(CostMethod.OPTION);
        options.add(CURRENCY);
        return options;
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var period = PerformancePeriod.resolve(context);
        var costMethod = CostMethod.parse(context);
        var requested = ExchangeRates.parseCurrency(CURRENCY, context.option(CURRENCY.name()));
        var client = context.loadClient().client();

        var currency = requested.orElse(client.getBaseCurrency());
        var rates = new ExchangeRates(context, client);
        var converter = new StrictConverter(rates, rates.converter(currency), LocalDate.now(context.clock()));

        var index = PerformanceIndex.forClient(client, converter, period.interval(), new ArrayList<>());
        var snapshot = index.getClientPerformanceSnapshot(costMethod == CostMethod.FIFO).orElseThrow();
        var figures = figures(index, snapshot);

        var data = new LinkedHashMap<String, Object>();
        data.put("currencyCode", currency);
        // core calculates lazily: only after all figures are read is every rate it needed known
        if (converter.isComplete())
            data.putAll(figures);

        var meta = ResponseMeta.of(context, client);
        period.putInto(meta);
        meta.put("costMethod", costMethod.id());
        meta.put("currency", currency);
        rates.putCacheInto(meta);

        var response = Response.of(data, meta);
        rates.warnings().forEach(response::warn);
        return response;
    }

    private static Map<String, Object> figures(PerformanceIndex index, ClientPerformanceSnapshot snapshot)
    {
        var figures = new LinkedHashMap<String, Object>();
        finiteFraction(snapshot.getPerformanceIRR()).ifPresent(irr -> figures.put("irr", irr));
        finiteFraction(index.getFinalAccumulatedPercentage()).ifPresent(ttwror -> figures.put("ttwror", ttwror));
        figures.put("absoluteGain", Decimals.amount(snapshot.getAbsoluteDelta()));
        FIGURES.forEach(figure -> figures.put(figure.key(), Decimals.amount(snapshot.getValue(figure.category()))));
        return figures;
    }

    private static Optional<Object> finiteFraction(double value)
    {
        if (!Double.isFinite(value))
            return Optional.empty();
        return Optional.of(Decimals.of(BigDecimal.valueOf(value)));
    }
}
