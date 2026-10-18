package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

/**
 * <code>performance</code>: how the whole client did over a period. The
 * figures are core's performance index (IRR, TTWROR) and its performance
 * snapshot (gains), converted by core's own converter over the CLI's rate
 * source; core decides which rate it uses on which day. Where the source has
 * no rate for a pair, core's converter falls back to 1.0: the spec has
 * <code>performance</code> follow the engine's own behavior, unlike
 * <code>holdings</code>. The command therefore looks up the rate of every
 * foreign currency the client uses for the last day of the period, and warns
 * if it is missing or stale (ADR 0005).
 */
final class PerformanceCommand
{
    private static final OptionSpec CURRENCY = new OptionSpec("currency", "code",
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

    private static Schema reportingAmount(String description)
    {
        return Schema.number(description + " In the reporting currency.");
    }

    private static Schema dataSchema()
    {
        var data = Schema.object("How the client performed over the period.")
                        .property("currencyCode", Schema.string("The reporting currency of every amount."))
                        .optionalProperty("irr", Schema.number(
                                        "Internal rate of return over the period, annualized, as a fraction: 0.05 is 5 %. Absent if core finds none."))
                        .optionalProperty("ttwror", Schema.number(
                                        "True time-weighted rate of return over the period, not annualized, as a fraction: 0.05 is 5 %."))
                        .property("absoluteGain", reportingAmount(
                                        "Capital gains, realized capital gains, earnings and currency gains, less fees and taxes."));
        FIGURES.forEach(figure -> data.property(figure.key(), reportingAmount(figure.description())));
        return data;
    }

    private static final Schema DATA = dataSchema();

    private static final Schema META = PerformancePeriod.declareIn(ResponseMeta.fileSchema())
                    .property("costMethod", CostMethod.schema())
                    .property("currency", Schema.string("The reporting currency: as given, otherwise the base currency."))
                    .optionalProperty("exchangeRates", ExchangeRates.metaSchema());

    static final CommandSpec SPEC = new CommandSpec("performance",
                    "IRR, TTWROR and gains of the whole client over a period, given as --from and --to or as a reporting-period --period code, by FIFO or moving-average cost, in the base currency or --currency.",
                    true, options(), DATA, META, ExchangeRates.WARNINGS, PerformanceCommand::execute);

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
        checkRates(client, rates, period, currency);

        var index = PerformanceIndex.forClient(client, rates.converter(currency), period.interval(), new ArrayList<>());
        var snapshot = index.getClientPerformanceSnapshot(costMethod == CostMethod.FIFO).orElseThrow();

        var data = new LinkedHashMap<String, Object>();
        data.put("currencyCode", currency);
        finiteFraction(snapshot.getPerformanceIRR()).ifPresent(irr -> data.put("irr", irr));
        finiteFraction(index.getFinalAccumulatedPercentage()).ifPresent(ttwror -> data.put("ttwror", ttwror));
        data.put("absoluteGain", Decimals.amount(snapshot.getAbsoluteDelta()));
        FIGURES.forEach(figure -> data.put(figure.key(), Decimals.amount(snapshot.getValue(figure.category()))));

        var meta = ResponseMeta.of(context, client);
        period.putInto(meta);
        meta.put("costMethod", costMethod.id());
        meta.put("currency", currency);
        rates.putCacheInto(meta);

        var response = Response.of(data, meta);
        rates.warnings().forEach(response::warn);
        return response;
    }

    /**
     * Core's converter never says that it found no rate, so the rate of each
     * foreign currency the client uses is looked up here for the last day of
     * the period. That notes a missing or stale one for the warnings.
     */
    private static void checkRates(Client client, ExchangeRates rates, PerformancePeriod period, String currency)
    {
        var foreign = new TreeSet<>(currenciesUsedBy(client));
        foreign.remove(currency);
        foreign.forEach(code -> rates.lookup(period.lastDay(), code, currency));
    }

    private static Set<String> currenciesUsedBy(Client client)
    {
        var accounts = client.getAccounts();
        var transactions = Stream.concat(client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream()),
                        accounts.stream().flatMap(a -> a.getTransactions().stream()));

        return Stream.concat(accounts.stream().map(a -> a.getCurrencyCode()),
                        transactions.map(Transaction::getSecurity).filter(Objects::nonNull)
                                        .map(Security::getCurrencyCode))
                        .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Optional<Object> finiteFraction(double value)
    {
        if (!Double.isFinite(value))
            return Optional.empty();
        return Optional.of(Decimals.of(BigDecimal.valueOf(value)));
    }
}
