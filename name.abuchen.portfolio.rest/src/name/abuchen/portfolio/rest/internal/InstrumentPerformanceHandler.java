package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

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
 * <p/>
 * A report over every instrument of a working file is more than one MCP answer
 * carries, so its rows are paged like every other list, keyed by the uuid of the
 * instrument a row is about. Paging is not only about size here: a row's lazy
 * figures, the daily series behind {@code timeWeighted} and {@code risk}, are
 * computed while the row is serialized, so a page costs the page and not the
 * whole report.
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

    public static JsonElement list(Client client, String fileId, ExchangeRateProviderFactory factory,
                    String openingDateParam, String closingDateParam, String currencyParam, String costMethodParam,
                    String taxesAndFeesParam, String metricsParam, String cursorParam, String limitParam)
    {
        var conversions = new ConversionRecorder(factory);
        var report = list(client, converters(factory, conversions), fileId, openingDateParam, closingDateParam,
                        currencyParam, costMethodParam, taxesAndFeesParam, metricsParam, cursorParam, limitParam)
                        .getAsJsonObject();
        FxWarnings.attach(report, conversions);
        return report;
    }

    /* package */ static JsonElement list(Client client, Function<String, CurrencyConverter> converters,
                    String fileId, String openingDateParam, String closingDateParam, String currencyParam,
                    String costMethodParam, String taxesAndFeesParam, String metricsParam, String cursorParam,
                    String limitParam)
    {
        var params = parse(client, openingDateParam, closingDateParam, currencyParam, costMethodParam,
                        taxesAndFeesParam, metricsParam);

        // which rows the report holds is decided by the file and the interval alone;
        // the metric groups, the cost method and the reporting currency only change
        // what a row says, so they stay out of the scope and a cursor survives them
        var pagingErrors = new ArrayList<ApiException.FieldError>();
        var scope = "performance|file=" + (fileId == null ? "" : fileId) //$NON-NLS-1$ //$NON-NLS-2$
                        + "|openingDate=" + params.openingDate() //$NON-NLS-1$
                        + "|closingDate=" + params.closingDate(); //$NON-NLS-1$
        var paging = Paging.parse(Paging.Kind.PERFORMANCE, scope, cursorParam, limitParam, pagingErrors);
        if (!pagingErrors.isEmpty())
            throw ApiException.badRequest(pagingErrors);

        var records = snapshot(client, converters, params).getRecords();

        // the model yields the records in the iteration order of a HashMap, so
        // sorting is what makes repeated calls comparable at all; two instruments
        // of the same name - or of names the collator holds equal - are told
        // apart by their uuid
        var sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(LazySecurityPerformanceRecord::getSecurityName, TextUtil::compare)
                        .thenComparing(record -> record.getSecurity().getUUID()));

        // serializing a record is what computes its lazy figures - the daily
        // series behind timeWeighted and risk above all - so this is where the time
        // goes, and why a page is cheaper than the report: only what a page holds
        // gets its series built. A paged answer is in the order of the instrument
        // uuid rather than by name, as a page of the statement of assets is
        var progress = Progress.current();
        var rendered = new AtomicInteger();
        var json = EntityJson.instrumentPerformanceContext(params);
        paging.apply(Paging.Kind.PERFORMANCE, json, "items", sorted, //$NON-NLS-1$
                        record -> record.getSecurity().getUUID(), record -> {
                            progress.checkCancelled();
                            var done = rendered.getAndIncrement();
                            progress.step(done, sorted.size(), progressLabel(done, sorted.size(), paging.given()));
                            return EntityJson.instrumentPerformance(record, params);
                        });
        return json;
    }

    /**
     * What one step of the loop reports. Over a page the report's row count is not
     * the number of rows this call renders, and "instrument 3 of 898" on a page of
     * sixty would read as a calculation that stopped early.
     */
    private static String progressLabel(int done, int inAll, boolean paged)
    {
        if (paged)
            return "instrument " + (done + 1) + " of this page, " + inAll + " rows in the report"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        return "instrument " + (done + 1) + " of " + inAll; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static JsonElement get(Client client, ExchangeRateProviderFactory factory, String uuid,
                    String openingDateParam, String closingDateParam, String currencyParam, String costMethodParam,
                    String taxesAndFeesParam, String metricsParam)
    {
        var conversions = new ConversionRecorder(factory);
        var report = get(client, converters(factory, conversions), uuid, openingDateParam, closingDateParam,
                        currencyParam, costMethodParam, taxesAndFeesParam, metricsParam).getAsJsonObject();
        FxWarnings.attach(report, conversions);
        return report;
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

    /** converters into any currency, each recording what it converted */
    private static Function<String, CurrencyConverter> converters(ExchangeRateProviderFactory factory,
                    ConversionRecorder conversions)
    {
        return currency -> conversions.wrap(new CurrencyConverterImpl(factory, currency));
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
