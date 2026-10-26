package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

/**
 * <code>performance</code>: how the client, or one of its investment accounts,
 * cash accounts or instruments, did over a period; with <code>--by
 * instrument</code>, how each instrument did. The
 * figures are core's performance index (IRR, TTWROR) and its performance
 * snapshot (gains), converted by core's own converter over the CLI's rate
 * source; core decides which rate it uses on which day. Core's converter falls
 * back to 1.0 without a word where it finds no rate, so a {@link StrictConverter}
 * checks every rate core asks for. If one is missing, no figure is reported
 * (ADR 0005); a stale rate is used and warned about.
 */
final class PerformanceCommand
{
    private static final OptionSpec CURRENCY = ReportingCurrency.option(
                    "ISO currency of the figures. Defaults to the base currency of the file.");

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

    private static Schema dataSchema()
    {
        var data = Schema.object(
                        "How the scope performed over the period. Every amount is in the reporting currency. Every figure but the currency is left out if an exchange rate is missing, with the warning exchange-rate-missing, and for a quiet instrument, with no-activity-in-period. With --by instrument, the currency and items instead.")
                        .property("currencyCode", Schema.string("The reporting currency of every amount."))
                        .optionalProperty("items", Schema.array(
                                        "With --by instrument only: one record per instrument active in the period, sorted by name (case-insensitive, ordinal) then UUID. Not paged, with no total.",
                                        PerformanceByInstrument.itemSchema()))
                        .optionalProperty("irr", Schema.number(
                                        "Internal rate of return over the period, annualized, as a fraction: 0.05 is 5 %. Absent if core finds none."))
                        .optionalProperty("ttwror", Schema.number(
                                        "True time-weighted rate of return over the period, not annualized, as a fraction: 0.05 is 5 %."))
                        .optionalProperty("absoluteGain", Schema.number(
                                        "Capital gains, realized capital gains, earnings and currency gains, less fees and taxes."));
        FIGURES.forEach(figure -> data.optionalProperty(figure.key(), Schema.number(figure.description())));
        return data;
    }

    private static final Schema DATA = dataSchema();

    private static final Schema META = PerformanceScope.declareIn(PerformancePeriod.declareIn(ResponseMeta.fileSchema()))
                    .property("costMethod", CostMethod.schema())
                    .property("currency", Schema.string("The reporting currency: as given, otherwise the base currency."))
                    .optionalProperty("exchangeRates", ReportingCurrency.ratesMetaSchema());

    private static final WarningSpec NO_ACTIVITY = new WarningSpec("no-activity-in-period",
                    "The instrument of --instrument was not held at the start or end of the period and had no transaction in it, so no figure is reported. Carries the uuid and name of the instrument.");

    private static final List<WarningSpec> WARNINGS = Stream
                    .concat(Stream.of(NO_ACTIVITY), ReportingCurrency.WARNINGS_COUNTING_DAYS.stream()).toList();

    static final CommandSpec SPEC = new CommandSpec("performance",
                    "IRR, TTWROR and gains of the whole client, one investment account (optionally with its reference cash account), one cash account or one instrument over a period (--from and --to, or --period), by FIFO or moving-average cost, in the base currency or --currency; or with --by instrument, one record per instrument active in the period.",
                    true, options(), DATA, META, WARNINGS, PerformanceCommand::execute);

    private PerformanceCommand()
    {
    }

    private static List<OptionSpec> options()
    {
        var options = new ArrayList<>(PerformancePeriod.OPTIONS);
        options.addAll(PerformanceScope.OPTIONS);
        options.add(CostMethod.OPTION);
        options.add(CURRENCY);
        options.add(PerformanceByInstrument.BY);
        return options;
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var period = PerformancePeriod.resolve(context);
        var costMethod = CostMethod.parse(context);
        var requested = ReportingCurrency.requested(context, CURRENCY);
        var client = context.loadClient().client();
        var scope = PerformanceScope.resolve(context, client);
        Optional<Client> breakdown = Optional.empty();
        if (PerformanceByInstrument.requested(context))
            breakdown = Optional.of(scope.narrowed(client).orElseThrow(PerformanceByInstrument::unsupportedScope));

        var reporting = requested.orBase(context, client);
        var converter = reporting.strictConverter();

        var data = new LinkedHashMap<String, Object>();
        data.put("currencyCode", reporting.code());

        Optional<Security> quiet = Optional.empty();
        if (breakdown.isPresent())
        {
            data.put("items", PerformanceByInstrument.items(breakdown.get(), converter, period, costMethod));
        }
        else
        {
            quiet = scope.quietInstrument(client, reporting.coreConverter(), period.interval());
            if (quiet.isEmpty())
            {
                var index = scope.index(client, converter, period.interval());
                var snapshot = index.getClientPerformanceSnapshot(costMethod == CostMethod.FIFO).orElseThrow();
                var figures = figures(index, snapshot);

                // core calculates lazily: only after all figures are read is every rate it needed known
                if (converter.isComplete())
                    data.putAll(figures);
            }
        }

        var meta = ResponseMeta.of(context, client);
        period.putInto(meta);
        scope.putInto(meta);
        meta.put("costMethod", costMethod.id());
        meta.put("currency", reporting.code());
        reporting.putRatesInto(meta);

        var response = Response.of(data, meta);
        quiet.ifPresent(security -> response.warn(new Warning(NO_ACTIVITY.code(),
                        "The instrument '" + MasterData.orEmpty(security.getName())
                                        + "' was not held at the start or end of the period and had no transaction in it.")
                                                        .with("uuid", security.getUUID())
                                                        .with("name", MasterData.orEmpty(security.getName()))));
        reporting.warnings().forEach(response::warn);
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

    static Optional<Object> finiteFraction(double value)
    {
        if (!Double.isFinite(value))
            return Optional.empty();
        return Optional.of(Decimals.of(BigDecimal.valueOf(value)));
    }
}
