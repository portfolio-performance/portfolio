package name.abuchen.portfolio.cli.performance;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.currency.ReportingCurrency;
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
 * back to 1.0 without a word where it finds no rate, so the figures are
 * reported only through {@link ReportingCurrency.CoreFigures#trusted}, which
 * checks every rate core asks for. If one is missing, no figure is reported
 * (ADR 0005); a stale rate is used and warned about.
 */
public final class PerformanceCommand
{
    private static final Facet<ReportingCurrency> CURRENCY = ReportingCurrency.facet(
                    "ISO currency of the figures. Defaults to the base currency of the file.",
                    ReportingCurrency.Counting.DAYS);

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

    public static final CommandSpec SPEC = CommandSpec.of("performance",
                    "IRR, TTWROR and gains of the whole client, one investment account (optionally with its reference cash account), one cash account or one instrument over a period (--from and --to, or --period), by FIFO or moving-average cost, in the base currency or --currency; or with --by instrument, one record per instrument active in the period.",
                    DATA, PerformanceCommand::execute, ClientFile.READ, PerformancePeriod.FACET,
                    PerformanceScope.FACET, CostMethod.FACET, CURRENCY);

    private PerformanceCommand()
    {
    }

    private static Map<String, Object> execute(CommandContext run) throws CliException
    {
        var client = run.client();
        var period = run.get(PerformancePeriod.FACET);
        var scope = run.get(PerformanceScope.FACET);
        var costMethod = run.get(CostMethod.FACET);
        var reporting = run.get(CURRENCY);
        var core = reporting.coreFigures();

        var data = new LinkedHashMap<String, Object>();
        data.put("currencyCode", reporting.code());

        var breakdown = scope.breakdown();
        if (breakdown.isPresent())
        {
            data.put("items", PerformanceByInstrument.items(breakdown.get(), core, period, costMethod));
            return data;
        }

        var quiet = scope.quietInstrument(client, core.unreported(), period.interval());
        if (quiet.isPresent())
        {
            run.warn(PerformanceScope.noActivity(quiet.get()));
            return data;
        }

        core.trusted(() -> {
            var index = scope.index(client, core.converter(), period.interval());
            var snapshot = index.getClientPerformanceSnapshot(costMethod == CostMethod.FIFO).orElseThrow();
            return figures(index, snapshot);
        }).ifPresent(data::putAll);
        return data;
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
