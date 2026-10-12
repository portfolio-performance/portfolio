package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

/**
 * The performance report broken down by instrument: the collection over every
 * instrument the interval touched, and the single-instrument member of it.
 * <p/>
 * The item route is genuinely cheaper than the collection, but for one reason
 * only: the snapshot build is shared fixed cost while the daily
 * {@code PerformanceIndex} behind the {@code timeWeighted} and {@code risk}
 * groups is per instrument and lazy, so serializing one instrument skips N-1
 * daily series. The saving comes entirely from that laziness - looking a record
 * up in the snapshot is a linear scan over records that were all built anyway.
 * <p/>
 * The factory is the host's per-file instance - it registers a listener on the
 * client, so this handler must not construct its own.
 */
public final class InstrumentPerformanceHandler
{
    /** the validated query parameters of one request */
    /* package */ record Params(LocalDate openingDate, LocalDate closingDate, String currency, CostMethod costMethod,
                    TaxesAndFees taxesAndFees, Set<MetricGroup> metrics)
    {
    }

    private InstrumentPerformanceHandler()
    {
    }

    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String openingDateParam,
                    String closingDateParam, String currencyParam, String costMethodParam, String taxesAndFeesParam,
                    String metricsParam)
    {
        return list(client, converters(factory), openingDateParam, closingDateParam, currencyParam, costMethodParam,
                        taxesAndFeesParam, metricsParam);
    }

    /* package */ static JsonElement list(Client client, Function<String, CurrencyConverter> converters,
                    String openingDateParam, String closingDateParam, String currencyParam, String costMethodParam,
                    String taxesAndFeesParam, String metricsParam)
    {
        var params = parse(client, openingDateParam, closingDateParam, currencyParam, costMethodParam,
                        taxesAndFeesParam, metricsParam);

        var records = snapshot(client, converters, params).getRecords();

        // the model yields the records in the iteration order of a HashMap, so
        // sorting is what makes repeated calls comparable at all
        var sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(LazySecurityPerformanceRecord::getSecurityName, TextUtil::compare));

        var items = new JsonArray();
        for (LazySecurityPerformanceRecord record : sorted)
            items.add(EntityJson.instrumentPerformance(record, params));

        var json = EntityJson.instrumentPerformanceContext(params);
        json.add("items", items); //$NON-NLS-1$
        return json;
    }

    public static JsonElement get(Client client, ExchangeRateProviderFactory factory, String uuid,
                    String openingDateParam, String closingDateParam, String currencyParam, String costMethodParam,
                    String taxesAndFeesParam, String metricsParam)
    {
        return get(client, converters(factory), uuid, openingDateParam, closingDateParam, currencyParam,
                        costMethodParam, taxesAndFeesParam, metricsParam);
    }

    /* package */ static JsonElement get(Client client, Function<String, CurrencyConverter> converters, String uuid,
                    String openingDateParam, String closingDateParam, String currencyParam, String costMethodParam,
                    String taxesAndFeesParam, String metricsParam)
    {
        // address the resource before interpreting the query, as the routes
        // that resolve the {file} segment do
        Security security = SecuritiesHandler.find(client, uuid);

        var params = parse(client, openingDateParam, closingDateParam, currencyParam, costMethodParam,
                        taxesAndFeesParam, metricsParam);

        // a member lookup of the collection: what the collection would not
        // contain, this route cannot return - synthesizing a record from no
        // line items would mean reading getters that were never meant to run
        // empty
        var record = snapshot(client, converters, params).getRecord(security)
                        .orElseThrow(() -> ApiException.noActivityInPeriod(MessageFormat.format(
                                        "{0} was neither held nor traded between {1} and {2}", security.getName(), //$NON-NLS-1$
                                        params.openingDate(), params.closingDate())));

        // the item's own fields are merged into the context at top level, so
        // that the item route returns the same shape one element of the
        // collection has, plus the context it was computed with
        var json = EntityJson.instrumentPerformanceContext(params);
        EntityJson.instrumentPerformance(record, params).entrySet()
                        .forEach(entry -> json.add(entry.getKey(), entry.getValue()));
        return json;
    }

    private static Function<String, CurrencyConverter> converters(ExchangeRateProviderFactory factory)
    {
        return currency -> new CurrencyConverterImpl(factory, currency);
    }

    private static LazySecurityPerformanceSnapshot snapshot(Client client,
                    Function<String, CurrencyConverter> converters, Params params)
    {
        return LazySecurityPerformanceSnapshot.create(client, converters.apply(params.currency()),
                        Interval.of(params.openingDate(), params.closingDate()));
    }

    /**
     * Validates all six parameters, reporting every actionable violation in one
     * 400 so that a client self-corrects in a single round-trip.
     */
    private static Params parse(Client client, String openingDateParam, String closingDateParam, String currencyParam,
                    String costMethodParam, String taxesAndFeesParam, String metricsParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        // null (not today) when unparseable: the range check must not judge a
        // value that never parsed
        var openingDate = CalcParams.openingDate(openingDateParam, errors);
        var closingDate = CalcParams.closingDate(closingDateParam, errors);
        var currency = CalcParams.reportingCurrency(client, currencyParam, errors);
        var costMethod = CalcParams.costMethod(costMethodParam, errors);
        var taxesAndFees = CalcParams.taxesAndFees(taxesAndFeesParam, errors);
        Set<MetricGroup> metrics = MetricGroup.parse(metricsParam, errors);
        CalcParams.requireRange(openingDate, closingDate, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        return new Params(openingDate, closingDate, currency, costMethod, taxesAndFees, metrics);
    }
}
