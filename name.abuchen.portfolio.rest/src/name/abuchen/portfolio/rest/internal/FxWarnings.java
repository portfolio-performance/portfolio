package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

/**
 * Which currency pairs a report converts at 1:1 because there is nothing to
 * convert with.
 * <p/>
 * {@link CurrencyConverterImpl} substitutes a rate of 1 in exactly two cases, and
 * both are independent of the date: the pair has no time series at all - the
 * factory answers that with a placeholder series that returns 1 on every lookup -
 * or a series exists but holds no rates, so {@code lookupRate} comes back empty
 * and the converter falls back on its own. A series that does hold rates answers
 * a date before its first quote with that first quote - an inaccuracy of a
 * different kind, not a 1:1 conversion, and deliberately not reported here.
 * <p/>
 * A money field cannot say "this number is unconverted" without becoming a second
 * shape for every amount, so the report says it once as {@code fxWarnings}: always
 * present, empty when nothing fell back - the same rule as {@code warnings} on a
 * computed collection.
 */
public final class FxWarnings
{
    /** no exchange rate series exists for the pair at all */
    private static final String NO_SERIES = "no-series"; //$NON-NLS-1$

    /** a series exists, but holds no rates, so every date falls back */
    private static final String NO_RATE = "no-rate"; //$NON-NLS-1$

    /** one pair that was converted at 1:1 */
    public record Warning(String currency, String reportingCurrency, String reason)
    {
    }

    private FxWarnings()
    {
    }

    /**
     * Adds {@code fxWarnings} to a report whose context has already echoed its
     * {@code reportingCurrency}. Reading the currency back out of the payload is
     * what makes the warning describe the currency the numbers were actually
     * computed in, not the one the caller spelled.
     */
    public static void attach(JsonObject report, Client client, ExchangeRateProviderFactory factory)
    {
        var currency = report.get("reportingCurrency"); //$NON-NLS-1$
        report.add("fxWarnings", check(client, factory, currency != null ? currency.getAsString() : null)); //$NON-NLS-1$
    }

    /** one entry per currency the file uses that is not the reporting currency */
    public static JsonArray check(Client client, ExchangeRateProviderFactory factory, String reportingCurrency)
    {
        var warnings = new JsonArray();
        for (var warning : list(client, factory, reportingCurrency))
            warnings.add(EntityJson.toJson(warning));
        return warnings;
    }

    /** the pairs in {@code client}'s currency order, which is sorted */
    /* package */ static List<Warning> list(Client client, ExchangeRateProviderFactory factory,
                    String reportingCurrency)
    {
        if (reportingCurrency == null)
            return List.of();

        var warnings = new ArrayList<Warning>();

        for (CurrencyUnit unit : client.getUsedCurrencies())
        {
            String currency = unit.getCurrencyCode();
            if (currency == null || currency.equals(reportingCurrency))
                continue;

            var reason = reasonOf(factory.getTimeSeries(currency, reportingCurrency));
            if (reason != null)
                warnings.add(new Warning(currency, reportingCurrency, reason));
        }

        return warnings;
    }

    /**
     * Why this pair converts at 1:1, or null when it converts properly.
     * <p/>
     * The placeholder the factory substitutes for a missing pair is the only exact
     * marker for "no series": a series holding no rates answers nothing, and an
     * inverse or chained one answers with real rates. That type lives in a package
     * this bundle is not friends of, so the one check that needs it is scoped here
     * rather than widening the core bundle's internal API; the test of this class
     * keeps the behaviour honest if core ever replaces the placeholder.
     */
    @SuppressWarnings("restriction")
    private static String reasonOf(ExchangeRateTimeSeries series)
    {
        if (series == null
                        || series instanceof name.abuchen.portfolio.money.impl.EmptyExchangeRateTimeSeries)
            return NO_SERIES;

        // a series holding no rates answers nothing at any date; today is the
        // date the converter's own fallback carries
        return series.lookupRate(LocalDate.now()).isPresent() ? null : NO_RATE;
    }
}
