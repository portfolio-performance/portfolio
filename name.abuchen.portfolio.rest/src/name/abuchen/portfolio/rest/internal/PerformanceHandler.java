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

        LocalDate openingDate = null;
        if (openingDateParam == null)
            errors.add(new ApiException.FieldError("openingDate", "required", "openingDate is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        else
            openingDate = parseDate("openingDate", openingDateParam, errors); //$NON-NLS-1$

        // null (not today) when unparseable: the range check below must not
        // judge a value that never parsed
        var closingDate = closingDateParam == null ? LocalDate.now()
                        : parseDate("closingDate", closingDateParam, errors); //$NON-NLS-1$

        var currency = client.getBaseCurrency();
        if (currencyParam != null)
        {
            if (CurrencyUnit.getInstance(currencyParam) == null)
                errors.add(new ApiException.FieldError("currency", "unknown-currency", //$NON-NLS-1$ //$NON-NLS-2$
                                currencyParam + " is not a known currency")); //$NON-NLS-1$
            else
                currency = currencyParam;
        }

        var costMethod = CostMethod.FIFO;
        if (costMethodParam != null)
        {
            if ("fifo".equals(costMethodParam)) //$NON-NLS-1$
                costMethod = CostMethod.FIFO;
            else if ("moving-average".equals(costMethodParam)) //$NON-NLS-1$
                costMethod = CostMethod.MOVING_AVERAGE;
            else
                errors.add(new ApiException.FieldError("costMethod", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "costMethod must be fifo or moving-average")); //$NON-NLS-1$
        }

        // the range constraint depends on the two dates having parsed and on
        // nothing else, so it accumulates with the other violations instead of
        // hiding behind them: a client fixing both in one round-trip needs to
        // see both
        if (openingDate != null && closingDate != null && !openingDate.isBefore(closingDate))
            errors.add(new ApiException.FieldError("closingDate", "invalid-range", //$NON-NLS-1$ //$NON-NLS-2$
                            "closingDate must be after openingDate")); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var converter = new CurrencyConverterImpl(factory, currency);
        var interval = Interval.of(openingDate, closingDate);

        var performance = new ClientPerformanceSnapshot(client, converter, interval, costMethod.useFifo());

        // TTWROR comes from a second engine; IRR is money-weighted, both are
        // independent of the cost method
        var ttwror = PerformanceIndex.forClient(client, converter, interval, new ArrayList<>())
                        .getFinalAccumulatedPercentage();
        var irr = performance.getPerformanceIRR();

        return EntityJson.performance(openingDate, closingDate, currency, costMethod, ttwror, irr, performance);
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
