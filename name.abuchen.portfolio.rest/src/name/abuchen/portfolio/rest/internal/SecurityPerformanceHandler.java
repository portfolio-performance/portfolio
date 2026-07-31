package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public final class SecurityPerformanceHandler
{
    private SecurityPerformanceHandler()
    {
    }

    /**
     * Per-security performance over the period: every security with activity in
     * the interval, including positions closed before the closing date. That is
     * the difference from {@code GET .../holdings?date=closingDate}, which can
     * only see what is still held - so a security sold at a profit mid-period is
     * invisible there but is often the largest contributor here.
     * <p>
     * The factory is the host's per-file instance - it registers a listener on
     * the client, so this handler must not construct its own.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String openingDateParam,
                    String closingDateParam, String currencyParam, String costMethodParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var range = PerformanceHandler.parseRange(client, openingDateParam, closingDateParam, currencyParam, errors);
        var costMethod = PerformanceHandler.parseCostMethod(costMethodParam, errors);
        PerformanceHandler.checkRange(range, errors);

        var converter = new CurrencyConverterImpl(factory, range.currency());
        var interval = Interval.of(range.openingDate(), range.closingDate());
        var snapshot = LazySecurityPerformanceSnapshot.create(client, converter, interval);

        return EntityJson.securityPerformance(range.currency(), costMethod == null ? CostMethod.FIFO : costMethod,
                        snapshot.getRecords());
    }
}
