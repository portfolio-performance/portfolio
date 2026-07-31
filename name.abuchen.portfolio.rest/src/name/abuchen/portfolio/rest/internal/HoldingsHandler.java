package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public final class HoldingsHandler
{
    private HoldingsHandler()
    {
    }

    /**
     * Values all holdings at the given date (default: today) in the given
     * currency (default: the file's base currency). A currency without an
     * exchange rate series converts 1:1 - the same silent fallback the UI
     * applies. The factory is the host's per-file instance - it registers a
     * listener on the client, so this handler must not construct its own.
     * <p>
     * Beyond the point-in-time snapshot, each instrument position is also
     * enriched with period-dependent metrics (cost basis, returns, dividends)
     * computed over {@code (openingDate, date]}. {@code openingDate} defaults
     * to "since inception" - see {@link #inceptionDate(Client, LocalDate)} -
     * and {@code costMethod} defaults to FIFO, matching the performance
     * endpoint.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String dateParam,
                    String openingDateParam, String currencyParam, String costMethodParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var date = LocalDate.now();
        if (dateParam != null)
        {
            try
            {
                date = LocalDate.parse(dateParam);
            }
            catch (DateTimeParseException e)
            {
                errors.add(new ApiException.FieldError("date", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "date must be an ISO 8601 date (YYYY-MM-DD)")); //$NON-NLS-1$
            }
        }

        LocalDate openingDate = null;
        if (openingDateParam != null)
        {
            try
            {
                openingDate = LocalDate.parse(openingDateParam);
            }
            catch (DateTimeParseException e)
            {
                errors.add(new ApiException.FieldError("openingDate", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "openingDate must be an ISO 8601 date (YYYY-MM-DD)")); //$NON-NLS-1$
            }
        }

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

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        if (openingDate != null && !openingDate.isBefore(date))
            throw ApiException.badRequest(List.of(new ApiException.FieldError("openingDate", "invalid-range", //$NON-NLS-1$ //$NON-NLS-2$
                            "openingDate must be before date"))); //$NON-NLS-1$

        var converter = new CurrencyConverterImpl(factory, currency);
        var snapshot = ClientSnapshot.create(client, converter, date);

        var interval = Interval.of(openingDate != null ? openingDate : inceptionDate(client, date), date);
        var context = buildContext(client, converter, currency, interval, costMethod, date, snapshot);

        return EntityJson.toJson(context, snapshot);
    }

    /**
     * "Since inception" for the whole file: the day before its earliest
     * transaction date (one day earlier because {@link Interval} is half-open
     * and excludes its start - the earliest transaction must fall inside the
     * interval, not on its boundary), or {@code date} itself (an empty
     * interval) when the file has no transaction yet.
     * <p>
     * Deliberately not {@link LocalDate#MIN}: unlike the cost-basis and
     * dividend metrics (which only ever scan the actual transaction list, so
     * an arbitrarily distant interval start costs nothing extra), the
     * TTWROR calculation builds a day-indexed series over the whole interval -
     * the same "since inception" default the desktop's statement of assets
     * view uses for its non-period-selectable columns would exhaust the heap
     * for period-selectable ones such as TTWROR. Bounding "inception" to the
     * file's actual first activity keeps the one shared interval safe for
     * every metric.
     */
    private static LocalDate inceptionDate(Client client, LocalDate date)
    {
        return client.getAllTransactions().stream() //
                        .map(pair -> pair.getTransaction().getDateTime().toLocalDate()) //
                        .min(Comparator.naturalOrder()) //
                        .map(earliest -> earliest.minusDays(1)) //
                        .orElse(date);
    }

    /**
     * Builds the reporting-currency performance record for every security,
     * plus - only for securities whose own currency differs from the
     * reporting currency - a second record in that native currency (mirroring
     * the desktop's "…BaseCurrency" columns, which recompute the same metrics
     * with the converter's term currency set to the security's own currency).
     */
    private static HoldingsContext buildContext(Client client, CurrencyConverterImpl converter, String currency,
                    Interval interval, CostMethod costMethod, LocalDate date, ClientSnapshot snapshot)
    {
        var records = LazySecurityPerformanceSnapshot.create(client, converter, interval).getRecords().stream()
                        .collect(Collectors.toMap(LazySecurityPerformanceRecord::getSecurity, r -> r));

        var localRecordsByCurrency = new HashMap<String, Map<Security, LazySecurityPerformanceRecord>>();
        snapshot.getAssetPositions() //
                        .map(AssetPosition::getSecurity) //
                        .filter(Objects::nonNull) //
                        .map(Security::getCurrencyCode) //
                        .filter(Objects::nonNull) //
                        .filter(cc -> !cc.equals(currency)) //
                        .distinct() //
                        .forEach(cc -> localRecordsByCurrency.computeIfAbsent(cc,
                                        key -> LazySecurityPerformanceSnapshot.create(client, converter.with(key), interval)
                                                        .getRecords().stream()
                                                        .collect(Collectors.toMap(
                                                                        LazySecurityPerformanceRecord::getSecurity,
                                                                        r -> r))));

        return new HoldingsContext(client, date, interval, costMethod, converter, records, localRecordsByCurrency,
                        client.getTaxonomies());
    }
}
