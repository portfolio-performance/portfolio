package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.util.Interval;

/**
 * Everything the per-holding JSON mapping needs beyond the {@code AssetPosition}
 * itself: the reporting-period performance record for each security (in the
 * reporting currency, and - only for securities whose own currency differs -
 * in that native currency too), the chosen cost method, the client's
 * taxonomies, and the currency converter (for the exchange rate and the
 * reporting-currency quote). Built once per request by {@link HoldingsHandler}.
 */
/* package */ record HoldingsContext(Client client, LocalDate closingDate, Interval interval, CostMethod costMethod,
                CurrencyConverter converter, Map<Security, LazySecurityPerformanceRecord> records,
                Map<String, Map<Security, LazySecurityPerformanceRecord>> localRecordsByCurrency,
                List<Taxonomy> taxonomies)
{
    /** the security's performance record in the reporting currency, if it has any line item in the interval */
    Optional<LazySecurityPerformanceRecord> record(Security security)
    {
        return Optional.ofNullable(records.get(security));
    }

    /**
     * The security's performance record in its own currency - only present
     * when that currency differs from the reporting currency, since the
     * reporting-currency record already applies otherwise.
     */
    Optional<LazySecurityPerformanceRecord> localRecord(Security security)
    {
        var currencyCode = security.getCurrencyCode();
        if (currencyCode == null)
            return Optional.empty();

        var byCurrency = localRecordsByCurrency.get(currencyCode);
        return byCurrency == null ? Optional.empty() : Optional.ofNullable(byCurrency.get(security));
    }
}
