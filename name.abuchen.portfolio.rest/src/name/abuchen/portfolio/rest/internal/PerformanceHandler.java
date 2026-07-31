package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.util.Interval;

public final class PerformanceHandler
{
    private static final int MAX_SERIES_YEARS = 20;

    private PerformanceHandler()
    {
    }

    /**
     * Computes the portfolio's performance between the opening and closing
     * valuation dates in the given currency (default: the file's base
     * currency). Reports the time-weighted (TTWROR) and money-weighted (IRR)
     * return plus the value-change breakdown that reconciles the opening to the
     * closing value. The dates are valuation snapshots, not a record range: an
     * activity on the opening date is part of the opening balance, not the
     * period. The factory is the host's per-file instance - it registers a
     * listener on the client, so this handler must not construct its own.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String openingDateParam,
                    String closingDateParam, String currencyParam, String costMethodParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var range = parseRange(client, openingDateParam, closingDateParam, currencyParam, errors);

        var costMethod = parseCostMethod(costMethodParam, errors);

        checkRange(range, errors);
        if (costMethod == null)
            costMethod = CostMethod.FIFO;

        var converter = new CurrencyConverterImpl(factory, range.currency());
        var interval = Interval.of(range.openingDate(), range.closingDate());

        var performance = new ClientPerformanceSnapshot(client, converter, interval, costMethod.useFifo());

        // TTWROR comes from a second engine; IRR is money-weighted, both are
        // independent of the cost method
        var ttwror = PerformanceIndex.forClient(client, converter, interval, new ArrayList<>())
                        .getFinalAccumulatedPercentage();
        var irr = performance.getPerformanceIRR();

        return EntityJson.performance(range.openingDate(), range.closingDate(), range.currency(), ttwror, irr,
                        performance);
    }

    /**
     * The daily index behind the same period {@link #list} reports on: one point
     * per calendar day with the accumulated TTWROR, total assets, invested
     * capital, that day's external flow and the running drawdown, plus the risk
     * metrics derived from the series. The cost method is irrelevant here - none
     * of these figures depend on it.
     */
    public static JsonElement series(Client client, ExchangeRateProviderFactory factory, String openingDateParam,
                    String closingDateParam, String currencyParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var range = parseRange(client, openingDateParam, closingDateParam, currencyParam, errors);
        checkRange(range, errors);

        // one array slot per calendar day upstream, so an unbounded range is an
        // unbounded response
        if (range.openingDate().plusYears(MAX_SERIES_YEARS).isBefore(range.closingDate()))
            throw ApiException.badRequest(List.of(new ApiException.FieldError("closingDate", "invalid-range",
                            "the range must not exceed " + MAX_SERIES_YEARS + " years")));

        var converter = new CurrencyConverterImpl(factory, range.currency());
        var interval = Interval.of(range.openingDate(), range.closingDate());
        var index = PerformanceIndex.forClient(client, converter, interval, new ArrayList<>());

        return EntityJson.performanceSeries(client, range.openingDate(), range.closingDate(), range.currency(),
                        index);
    }

    /** the requested period; openingDate is null when it could not be parsed */
    /* package */ record Range(LocalDate openingDate, LocalDate closingDate, String currency)
    {
    }

    /**
     * Collects every parameter problem instead of failing on the first, so a
     * caller sees all of them at once. Errors are appended in parameter order.
     */
    /* package */ static Range parseRange(Client client, String openingDateParam, String closingDateParam,
                    String currencyParam, List<ApiException.FieldError> errors)
    {
        LocalDate openingDate = null;
        if (openingDateParam == null)
            errors.add(new ApiException.FieldError("openingDate", "required", "openingDate is required"));
        else
            openingDate = parseDate("openingDate", openingDateParam, errors);

        var closingDate = LocalDate.now();
        if (closingDateParam != null)
        {
            var parsed = parseDate("closingDate", closingDateParam, errors);
            if (parsed != null)
                closingDate = parsed;
        }

        var currency = client.getBaseCurrency();
        if (currencyParam != null)
        {
            if (CurrencyUnit.getInstance(currencyParam) == null)
                errors.add(new ApiException.FieldError("currency", "unknown-currency",
                                currencyParam + " is not a known currency"));
            else
                currency = currencyParam;
        }

        return new Range(openingDate, closingDate, currency);
    }

    /* package */ static void checkRange(Range range, List<ApiException.FieldError> errors)
    {
        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // the range constraint can only be judged once both dates parsed
        if (!range.openingDate().isBefore(range.closingDate()))
            throw ApiException.badRequest(List.of(new ApiException.FieldError("closingDate", "invalid-range",
                            "closingDate must be after openingDate")));
    }

    /**
     * Returns null when the parameter was absent or invalid - an invalid value
     * also lands an error, so the caller only reaches its default for an absent one.
     */
    /* package */ static CostMethod parseCostMethod(String costMethodParam, List<ApiException.FieldError> errors)
    {
        if (costMethodParam == null)
            return null;
        if ("fifo".equals(costMethodParam))
            return CostMethod.FIFO;
        if ("moving-average".equals(costMethodParam))
            return CostMethod.MOVING_AVERAGE;

        errors.add(new ApiException.FieldError("costMethod", "invalid-value",
                        "costMethod must be fifo or moving-average"));
        return null;
    }

    private static LocalDate parseDate(String field, String value, List<ApiException.FieldError> errors)
    {
        try
        {
            return LocalDate.parse(value);
        }
        catch (DateTimeParseException e)
        {
            errors.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            field + " must be an ISO 8601 date (YYYY-MM-DD)")); //$NON-NLS-1$
            return null;
        }
    }
}
