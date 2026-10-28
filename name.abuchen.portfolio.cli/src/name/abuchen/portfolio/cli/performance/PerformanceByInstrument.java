package name.abuchen.portfolio.cli.performance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.currency.ReportingCurrency;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.snapshot.security.CalculationLineItem;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;

/**
 * <code>performance --by instrument</code>: one record per instrument that was
 * active in the period, from core's per-security performance snapshot over the
 * client as the scope narrows it. Each record is judged on its own: one that
 * needs a missing exchange rate loses its figures, the others are reported.
 */
final class PerformanceByInstrument
{
    static final String INSTRUMENT = "instrument";

    static final OptionSpec BY = new OptionSpec("by", "dimension",
                    "Break the figures down: instrument is the only dimension. Returns one record per instrument active in the period, sorted by name, instead of the figures of the scope. With the whole client or an investment account.");

    private PerformanceByInstrument()
    {
    }

    /**
     * @return whether the figures are to be broken down by instrument
     * @throws CliException
     *             <code>invalid-option-value</code> for any other dimension
     */
    static boolean requested(CommandContext context) throws CliException
    {
        var value = context.text(BY);
        if (value.isPresent() && !INSTRUMENT.equals(value.get()))
            throw BY.invalid(value.get(), "'" + INSTRUMENT + "'");
        return value.isPresent();
    }

    /**
     * @return a usage error: the scope cannot be broken down by instrument
     */
    static CliException unsupportedScope()
    {
        return CliException.usage("conflicting-options", "Conflicting options",
                        "Option '--" + BY.name()
                                        + "' works with the whole client or an investment account, not with '--cash-account' or '--instrument'.")
                        .with("option", BY.name());
    }

    static Schema itemSchema()
    {
        return Schema.object("How one instrument performed over the period, as core's per-security snapshot has it. Amounts are in the reporting currency; the figures are absent if an exchange rate is missing.")
                        .property("uuid", Schema.string("UUID of the instrument."))
                        .property("name", Schema.string("Name of the instrument."))
                        .property("retired", Schema.bool("Whether the instrument is retired."))
                        .optionalProperty("isin", Schema.string("ISIN, if set."))
                        .optionalProperty("tickerSymbol", Schema.string("Ticker symbol, if set."))
                        .optionalProperty("irr", Schema.number(
                                        "Internal rate of return of the instrument over the period, annualized, as a fraction: 0.05 is 5 %. It can differ slightly from irr at --instrument scope, which core calculates differently. Absent if core finds none."))
                        .optionalProperty("ttwror", Schema.number(
                                        "True time-weighted rate of return over the period, not annualized, as a fraction: 0.05 is 5 %."))
                        .optionalProperty("initialValue", Schema.number("Value of the holding at the start of the period."))
                        .optionalProperty("finalValue", Schema.number("Market value of the holding at the end of the period."))
                        .optionalProperty("dividends", Schema.number("Dividends received in the period."))
                        .optionalProperty("fees", Schema.number("Fees paid in the period."))
                        .optionalProperty("taxes", Schema.number("Taxes paid in the period."))
                        .optionalProperty("absoluteGain", Schema.number(
                                        "Market value at the end and sales and dividends, less the value at the start, purchases, fees and taxes. Core's delta: it does not depend on the cost method."))
                        .optionalProperty("realizedCapitalGains",
                                        Schema.number("Realized capital gains of the period, by the cost method."))
                        .optionalProperty("unrealizedCapitalGains",
                                        Schema.number("Unrealized capital gains on the holding at the end, by the cost method."));
    }

    /**
     * The records of the instruments active in the period, sorted by name then
     * UUID.
     *
     * @param narrowed
     *            the client as far as the scope covers it
     */
    static List<Map<String, Object>> items(Client narrowed, ReportingCurrency.CoreFigures core,
                    PerformancePeriod period, CostMethod costMethod)
    {
        var converter = core.converter();
        var records = new ArrayList<>(
                        LazySecurityPerformanceSnapshot.create(narrowed, converter, period.interval()).getRecords());
        records.sort(MasterData.byNameThenUuid((LazySecurityPerformanceRecord r) -> r.getSecurity().getName(),
                        r -> r.getSecurity().getUUID()));

        var items = new ArrayList<Map<String, Object>>();
        for (var record : records)
        {
            // judged record by record: the figures of one do not depend on the rates another lacked
            var item = identity(record);
            core.trusted(() -> figures(record, converter, period, costMethod)).ifPresent(item::putAll);
            items.add(item);
        }
        return items;
    }

    private static Map<String, Object> identity(LazySecurityPerformanceRecord record)
    {
        var security = record.getSecurity();

        var json = new LinkedHashMap<String, Object>();
        MasterData.putIdentity(json, security.getUUID(), security.getName());
        json.put("retired", security.isRetired());
        MasterData.putIfSet(json, "isin", security.getIsin());
        MasterData.putIfSet(json, "tickerSymbol", security.getTickerSymbol());
        return json;
    }

    private static Map<String, Object> figures(LazySecurityPerformanceRecord record, CurrencyConverter converter,
                    PerformancePeriod period, CostMethod costMethod)
    {
        var figures = new LinkedHashMap<String, Object>();
        PerformanceCommand.finiteFraction(record.getIrr()).ifPresent(irr -> figures.put("irr", irr));
        PerformanceCommand.finiteFraction(record.getTrueTimeWeightedRateOfReturn())
                        .ifPresent(ttwror -> figures.put("ttwror", ttwror));

        // like the market value at the end: summed in the currency of the instrument, converted once
        var initialValue = record.getLineItems().stream().filter(CalculationLineItem.ValuationAtStart.class::isInstance)
                        .map(CalculationLineItem::getValue)
                        .collect(MoneyCollectors.sum(record.getSecurity().getCurrencyCode()))
                        .with(converter.at(period.firstDay().minusDays(1)));

        figures.put("initialValue", Decimals.amount(initialValue));
        figures.put("finalValue", Decimals.amount(record.getMarketValue()));
        figures.put("dividends", Decimals.amount(record.getSumOfDividends()));
        figures.put("fees", Decimals.amount(record.getFees()));
        figures.put("taxes", Decimals.amount(record.getTaxes()));
        figures.put("absoluteGain", Decimals.amount(record.getDelta()));
        figures.put("realizedCapitalGains",
                        Decimals.amount(record.getRealizedCapitalGains(costMethod.core()).getCapitalGains()));
        figures.put("unrealizedCapitalGains",
                        Decimals.amount(record.getUnrealizedCapitalGains(costMethod.core()).getCapitalGains()));
        return figures;
    }
}
