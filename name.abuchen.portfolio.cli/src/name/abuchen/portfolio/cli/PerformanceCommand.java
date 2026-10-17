package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.util.Interval;

/**
 * <code>performance</code>: how the whole client did over a period. The
 * figures are core's performance index (IRR, TTWROR) and its performance
 * snapshot (gains), converted by core's own converter over the CLI's rate
 * source; core decides which rate it uses on which day. Where the source has
 * no rate for a pair, core's converter falls back to 1.0 and no warning is
 * raised: the spec has <code>performance</code> follow the engine's own
 * behavior, unlike <code>holdings</code>.
 */
final class PerformanceCommand
{
    /** Core's code for "year to date"; agents also say <code>YTD</code>. */
    private static final String YEAR_TO_DATE_CODE = "X";
    private static final String YEAR_TO_DATE_ALIAS = "YTD";

    private static final OptionSpec FROM = OptionSpec.date("from",
                    "Day the opening value is taken at, the end of that day: activity after it counts. With --to; not with --period.");

    private static final OptionSpec TO = OptionSpec.date("to",
                    "Last day of the period, inclusive. Defaults to today. Not with --period.");

    private static final OptionSpec PERIOD = new OptionSpec("period", "code",
                    "A reporting-period code of the desktop app, resolved by core relative to today: for example L1Y0 (last year), X or YTD (year to date), Y2025 (the year 2025), L0Y6 (last six months), S2025-01-15 (since a day). Instead of --from and --to.");

    private static final OptionSpec COST_METHOD = new OptionSpec("cost-method", "method",
                    "How capital gains are costed: fifo (the default) or moving-average. Echoed in meta.");

    private static final OptionSpec CURRENCY = new OptionSpec("currency", "code",
                    "ISO currency of the figures. Defaults to the base currency of the file.");

    /** A cost method of the <code>--cost-method</code> option, by the name it has there. */
    private enum CostMethodOption
    {
        FIFO("fifo"), MOVING_AVERAGE("moving-average");

        private final String id;

        CostMethodOption(String id)
        {
            this.id = id;
        }

        static List<String> ids()
        {
            return Arrays.stream(values()).map(m -> m.id).toList();
        }
    }

    private static Schema reportingAmount(String description)
    {
        return Schema.number(description + " In the reporting currency.");
    }

    private static final Schema DATA = Schema.object("How the client performed over the period.")
                    .property("currencyCode", Schema.string("The reporting currency of every amount."))
                    .optionalProperty("irr", Schema.number(
                                    "Internal rate of return over the period, annualized, as a fraction: 0.05 is 5 %. Absent if core finds none."))
                    .optionalProperty("ttwror", Schema.number(
                                    "True time-weighted rate of return over the period, not annualized, as a fraction: 0.05 is 5 %."))
                    .property("absoluteGain", reportingAmount(
                                    "Capital gains, realized capital gains, earnings and currency gains, less fees and taxes."))
                    .property("initialValue", reportingAmount("Value of the client at the start of the period."))
                    .property("finalValue", reportingAmount("Value of the client at the end of the period."))
                    .property("capitalGains",
                                    reportingAmount("Unrealized capital gains of the period, by the cost method."))
                    .property("realizedCapitalGains",
                                    reportingAmount("Realized capital gains of the period, by the cost method."))
                    .property("earnings", reportingAmount("Dividends and interest, net of interest charges."))
                    .property("fees", reportingAmount("Fees paid."))
                    .property("taxes", reportingAmount("Taxes paid."))
                    .property("currencyGains", reportingAmount("Gains and losses from changes of exchange rates."))
                    .property("transfers",
                                    reportingAmount("Deposits less removals, and deliveries in less deliveries out."));

    private static final Schema META = ResponseMeta.fileSchema()
                    .property("from", Schema.string(
                                    "The day the opening value is taken at, as resolved: as given with --from, or the start of the --period code."))
                    .property("to", Schema.string("The last day of the period, as resolved."))
                    .optionalProperty("period", Schema.string("The reporting-period code, as given. Absent with --from."))
                    .property("costMethod", Schema.enumOf("The cost method used: as given, otherwise fifo.",
                                    CostMethodOption.ids()))
                    .property("currency", Schema.string("The reporting currency: as given, otherwise the base currency."))
                    .optionalProperty("exchangeRates", ExchangeRates.metaSchema());

    static final CommandSpec SPEC = new CommandSpec("performance",
                    "IRR, TTWROR and gains of the whole client over a period, given as --from and --to or as a reporting-period --period code, by FIFO or moving-average cost, in the base currency or --currency.",
                    true, List.of(FROM, TO, PERIOD, COST_METHOD, CURRENCY), DATA, META, ExchangeRates.WARNINGS,
                    PerformanceCommand::execute);

    private PerformanceCommand()
    {
    }

    /** The resolved interval of a run, and the code it came from, if any. */
    private record Period(Interval interval, Optional<String> code)
    {
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var period = resolvePeriod(context);
        var costMethod = parseCostMethod(context);
        var requested = ExchangeRates.parseCurrency(CURRENCY, context.option(CURRENCY.name()));
        var client = context.loadClient().client();

        var currency = requested.orElse(client.getBaseCurrency());
        var rates = new ExchangeRates(context, client);

        var index = PerformanceIndex.forClient(client, rates.converter(currency), period.interval(), new ArrayList<>());
        var snapshot = index.getClientPerformanceSnapshot(costMethod == CostMethodOption.FIFO).orElseThrow();

        var data = new LinkedHashMap<String, Object>();
        data.put("currencyCode", currency);
        rate(snapshot.getPerformanceIRR()).ifPresent(irr -> data.put("irr", irr));
        rate(index.getFinalAccumulatedPercentage()).ifPresent(ttwror -> data.put("ttwror", ttwror));
        data.put("absoluteGain", amount(snapshot.getAbsoluteDelta()));
        data.put("initialValue", amount(snapshot.getValue(CategoryType.INITIAL_VALUE)));
        data.put("finalValue", amount(snapshot.getValue(CategoryType.FINAL_VALUE)));
        data.put("capitalGains", amount(snapshot.getValue(CategoryType.CAPITAL_GAINS)));
        data.put("realizedCapitalGains", amount(snapshot.getValue(CategoryType.REALIZED_CAPITAL_GAINS)));
        data.put("earnings", amount(snapshot.getValue(CategoryType.EARNINGS)));
        data.put("fees", amount(snapshot.getValue(CategoryType.FEES)));
        data.put("taxes", amount(snapshot.getValue(CategoryType.TAXES)));
        data.put("currencyGains", amount(snapshot.getValue(CategoryType.CURRENCY_GAINS)));
        data.put("transfers", amount(snapshot.getValue(CategoryType.TRANSFERS)));

        var meta = ResponseMeta.of(context, client);
        meta.put("from", period.interval().getStart().toString());
        meta.put("to", period.interval().getEnd().toString());
        period.code().ifPresent(code -> meta.put("period", code));
        meta.put("costMethod", costMethod.id);
        meta.put("currency", currency);
        rates.cacheMeta().ifPresent(cache -> meta.put("exchangeRates", cache));

        var response = Response.of(data, meta);
        rates.warnings().forEach(response::warn);
        return response;
    }

    /**
     * The period of the run: <code>--from</code> and <code>--to</code>, or a
     * reporting-period code.
     *
     * @throws CliException
     *             a usage error if the options are missing, conflict or are
     *             invalid
     */
    private static Period resolvePeriod(CommandContext context) throws CliException
    {
        var code = context.option(PERIOD.name());

        Interval interval;
        if (code.isPresent())
        {
            if (context.option(FROM.name()).isPresent() || context.option(TO.name()).isPresent())
                throw CliException.usage("conflicting-options", "Conflicting options",
                                "Option '--" + PERIOD.name() + "' cannot be combined with '--" + FROM.name()
                                                + "' or '--" + TO.name() + "'.")
                                .with("option", PERIOD.name());

            var today = LocalDate.now(context.clock());
            interval = parseReportingPeriod(code.get(), today).toInterval(today);
        }
        else
        {
            var range = DateRange.parse(context, FROM, TO);
            var from = range.from().orElseThrow(() -> CliException.usage("period-required", "Period required",
                            "Pass --" + FROM.name() + " <date> (with an optional --" + TO.name() + "), or --"
                                            + PERIOD.name() + " <code>."));
            interval = Interval.of(from, range.to());
        }

        // core answers a period that ends on its start day (e.g. "since" a future day) with an empty interval
        if (!interval.getStart().isBefore(interval.getEnd()))
            throw CliException.usage("invalid-date-range", "Invalid date range",
                            "The period must start before it ends, but it runs from " + interval.getStart() + " to "
                                            + interval.getEnd() + ".")
                            .with("from", interval.getStart().toString()).with("to", interval.getEnd().toString());

        return new Period(interval, code);
    }

    private static ReportingPeriod parseReportingPeriod(String code, LocalDate today) throws CliException
    {
        var normalized = YEAR_TO_DATE_ALIAS.equals(code) ? YEAR_TO_DATE_CODE : code;

        Optional<ReportingPeriod> period = Optional.empty();
        try
        {
            if (!normalized.isBlank())
                period = ReportingPeriod.tryFrom(normalized);
        }
        catch (RuntimeException e)
        {
            // core does not guard every malformed code: reported below like any other
        }

        return period.map(p -> independentOfTheWallClock(p, today))
                        .orElseThrow(() -> PERIOD.invalid(code,
                                        "a reporting-period code such as L1Y0, YTD or Y2025"));
    }

    /**
     * Core's year-to-date and current-month periods take their start from the
     * system clock, not from the day they are resolved for. The same periods
     * are expressed as "since" the day before the start, from the injected
     * clock, so that the period is deterministic.
     */
    private static ReportingPeriod independentOfTheWallClock(ReportingPeriod period, LocalDate today)
    {
        if (period instanceof ReportingPeriod.YearToDate)
            return new ReportingPeriod.SinceX(today.withDayOfMonth(1).withMonth(1).minusDays(1));
        if (period instanceof ReportingPeriod.CurrentMonth)
            return new ReportingPeriod.SinceX(today.withDayOfMonth(1).minusDays(1));
        return period;
    }

    private static CostMethodOption parseCostMethod(CommandContext context) throws CliException
    {
        var raw = context.option(COST_METHOD.name());
        if (raw.isEmpty())
            return CostMethodOption.FIFO;

        return Arrays.stream(CostMethodOption.values()).filter(m -> m.id.equals(raw.get())).findFirst()
                        .orElseThrow(() -> COST_METHOD.invalid(raw.get(),
                                        "one of: " + String.join(", ", CostMethodOption.ids())));
    }

    private static Optional<Object> rate(double value)
    {
        if (!Double.isFinite(value))
            return Optional.empty();
        return Optional.of(Decimals.of(BigDecimal.valueOf(value)));
    }

    private static Object amount(Money money)
    {
        return Decimals.of(money.getAmount(), Values.Amount);
    }
}
