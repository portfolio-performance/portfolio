package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
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

        // null (not today) when unparseable: the range check must not judge a
        // value that never parsed
        var openingDate = CalcParams.openingDate(openingDateParam, errors);
        var closingDate = CalcParams.closingDate(closingDateParam, errors);
        var currency = CalcParams.currency(client, currencyParam, errors);
        var costMethod = CalcParams.costMethod(costMethodParam, errors);
        CalcParams.requireRange(openingDate, closingDate, errors);

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
}
