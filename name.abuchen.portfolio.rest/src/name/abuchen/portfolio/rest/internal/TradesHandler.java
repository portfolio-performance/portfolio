package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.security.SnapshotCache;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.snapshot.trades.TradeGrouping;
import name.abuchen.portfolio.util.TextUtil;

/**
 * Matched trades for a file or one instrument.
 * <p/>
 * Trades are computed, not stored. Per-security collector failures are reported
 * as {@code warnings} so one bad security does not hide the rest.
 */
public final class TradesHandler
{
    /**
     * @param from
     *            the first day of the period a trade must touch, or null for no
     *            lower bound
     * @param to
     *            the last day of that period, or null for no upper bound
     */
    /* package */ record Params(Set<TradeStatus> status, TradeGrouping grouping, CostMethod costMethod,
                    TaxesAndFees taxesAndFees, String currency, LocalDate valuationDate, LocalDate from,
                    LocalDate to)
    {
        /**
         * Whether the trade was held at some point of the period: it started on
         * or before its last day and was still open, or closed, on or after its
         * first. A trade closed within the period is in it, and so is one still
         * held through it.
         */
        boolean touches(Trade trade)
        {
            if (to != null && trade.getStart().toLocalDate().isAfter(to))
                return false;
            if (from != null)
            {
                var end = trade.getEnd();
                if (end.isPresent() && end.get().toLocalDate().isBefore(from))
                    return false;
            }
            return true;
        }
    }

    /**
     * A security the collector could not reconcile. The message is generated
     * here rather than taken from the exception: the collector's text is
     * localized to the running application, and every other string this API
     * emits is English.
     */
    /* package */ record Warning(Security instrument, String code, String message)
    {
    }

    private TradesHandler()
    {
    }

    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String statusParam,
                    String groupingParam, String costMethodParam, String taxesAndFeesParam, String currencyParam)
    {
        return list(client, factory, statusParam, groupingParam, costMethodParam, taxesAndFeesParam, currencyParam,
                        null, null);
    }

    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String statusParam,
                    String groupingParam, String costMethodParam, String taxesAndFeesParam, String currencyParam,
                    String fromParam, String toParam)
    {
        var params = parse(client, statusParam, groupingParam, costMethodParam, taxesAndFeesParam, currencyParam,
                        fromParam, toParam);
        return respond(collect(client, factory, client.getSecurities(), params), params);
    }

    public static JsonElement forInstrument(Client client, ExchangeRateProviderFactory factory, String uuid,
                    String statusParam, String groupingParam, String costMethodParam, String taxesAndFeesParam,
                    String currencyParam)
    {
        return forInstrument(client, factory, uuid, statusParam, groupingParam, costMethodParam, taxesAndFeesParam,
                        currencyParam, null, null);
    }

    public static JsonElement forInstrument(Client client, ExchangeRateProviderFactory factory, String uuid,
                    String statusParam, String groupingParam, String costMethodParam, String taxesAndFeesParam,
                    String currencyParam, String fromParam, String toParam)
    {
        // resolve the resource before validating query parameters
        var security = SecuritiesHandler.find(client, uuid);

        var params = parse(client, statusParam, groupingParam, costMethodParam, taxesAndFeesParam, currencyParam,
                        fromParam, toParam);

        // sub-collection: an untraded instrument has an empty list, not a 404
        return respond(collect(client, factory, List.of(security), params), params);
    }

    private static Params parse(Client client, String statusParam, String groupingParam, String costMethodParam,
                    String taxesAndFeesParam, String currencyParam, String fromParam, String toParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var status = TradeStatus.parse(statusParam, errors);
        var grouping = CalcParams.grouping(groupingParam, errors);
        var costMethod = CalcParams.costMethod(costMethodParam, errors);
        var taxesAndFees = CalcParams.taxesAndFees(taxesAndFeesParam, errors);
        var currency = CalcParams.reportingCurrency(client, currencyParam, errors);
        var from = fromParam == null ? null : CalcParams.date("from", fromParam, errors); //$NON-NLS-1$
        var to = toParam == null ? null : CalcParams.date("to", toParam, errors); //$NON-NLS-1$

        if (from != null && to != null && to.isBefore(from))
            errors.add(new ApiException.FieldError("to", "invalid-range", "to must not be before from")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // one valuation date for every open trade in the response
        return new Params(status, grouping, costMethod, taxesAndFees, currency, LocalDate.now(), from, to);
    }

    /* package */ record Collected(List<Trade> trades, List<Warning> warnings, ConversionRecorder conversions)
    {
    }

    private static Collected collect(Client client, ExchangeRateProviderFactory factory, List<Security> securities,
                    Params params)
    {
        // records what computing the trades converted at 1:1 - for every trade of these
        // instruments, including those a status filter leaves out of the answer,
        // because the collector has to match all of them
        var conversions = new ConversionRecorder(factory);
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, params.currency()));

        // moving-average costs share one snapshot cache per request
        var snapshotCache = new SnapshotCache();

        var trades = new ArrayList<Trade>();
        var warnings = new ArrayList<Warning>();

        // the collector keeps no state between securities
        var collector = new TradeCollector(client, converter, params.grouping(), snapshotCache);

        for (var security : securities)
        {
            try
            {
                for (var trade : collector.collect(security))
                {
                    if (params.status().contains(TradeStatus.of(trade)) && params.touches(trade))
                        trades.add(trade);
                }
            }
            catch (TradeCollectorException e)
            {
                warnings.add(warning(security, e));
            }
        }

        // stable sort keeps the collector's order within one security
        trades.sort(Comparator.comparing(t -> t.getSecurity().getName(), TextUtil::compare));

        return new Collected(trades, warnings, conversions);
    }

    private static Warning warning(Security security, TradeCollectorException e)
    {
        return switch (e.getReason())
        {
            case NO_HOLDINGS_FOR_SELL -> new Warning(security, "no-holdings-for-sell", //$NON-NLS-1$
                            "a sale had to be matched against a securities account holding no open lots"); //$NON-NLS-1$
            case MISSING_HOLDINGS_FOR_SELL -> new Warning(security, "missing-holdings-for-sell", //$NON-NLS-1$
                            "a sale covered more shares than the securities account held"); //$NON-NLS-1$
            case NO_HOLDINGS_FOR_TRANSFER -> new Warning(security, "no-holdings-for-transfer", //$NON-NLS-1$
                            "shares were transferred out of a securities account holding none of them"); //$NON-NLS-1$
            case MISSING_HOLDINGS_FOR_TRANSFER -> new Warning(security, "missing-holdings-for-transfer", //$NON-NLS-1$
                            "more shares were transferred out of a securities account than it held"); //$NON-NLS-1$
        };
    }

    private static JsonElement respond(Collected collected, Params params)
    {
        var items = new JsonArray();
        for (var trade : collected.trades())
            items.add(EntityJson.toJson(trade, params));

        var warnings = new JsonArray();
        for (var warning : collected.warnings())
            warnings.add(EntityJson.toJson(warning));

        var json = EntityJson.tradesContext(params);
        // after the items: a trade's figures may be computed as it is rendered,
        // and every fallback on the way has to be seen first
        FxWarnings.attach(json, collected.conversions());
        // always present so clients can detect partial success
        json.add("warnings", warnings); //$NON-NLS-1$
        json.add("items", items); //$NON-NLS-1$
        return json;
    }
}
