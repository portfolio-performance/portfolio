package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
 * <p/>
 * The currencies named are the ones the report converted, not the ones the file
 * happens to use. A report covering part of the file - trades of one instrument,
 * the performance of one instrument - would otherwise warn about money that never
 * reached the response.
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
     * Adds {@code fxWarnings} to a report that covers the whole file, whose
     * conversion therefore touches every currency the file uses.
     */
    public static void attach(JsonObject report, Client client, ExchangeRateProviderFactory factory)
    {
        attach(report, currenciesOf(client), factory);
    }

    /**
     * Adds {@code fxWarnings} to a report that covers part of the file, naming
     * only the currencies it converted.
     * <p/>
     * Reading the {@code reportingCurrency} back out of the payload - rather than
     * taking it from the caller - is what makes the warning describe the currency
     * the numbers were computed in, including a value the handler defaulted.
     */
    public static void attach(JsonObject report, Collection<String> currencies, ExchangeRateProviderFactory factory)
    {
        var currency = report.get("reportingCurrency"); //$NON-NLS-1$
        report.add("fxWarnings", check(currencies, factory, currency != null ? currency.getAsString() : null)); //$NON-NLS-1$
    }

    /** one entry per converted currency that is not the reporting currency */
    public static JsonArray check(Collection<String> currencies, ExchangeRateProviderFactory factory,
                    String reportingCurrency)
    {
        var warnings = new JsonArray();
        for (var warning : list(currencies, factory, reportingCurrency))
            warnings.add(EntityJson.toJson(warning));
        return warnings;
    }

    /** the converted currencies in code order, duplicates dropped - a report stays reproducible */
    /* package */ static List<Warning> list(Collection<String> currencies, ExchangeRateProviderFactory factory,
                    String reportingCurrency)
    {
        if (reportingCurrency == null)
            return List.of();

        var warnings = new ArrayList<Warning>();

        currencies.stream() //
                        .filter(Objects::nonNull) //
                        .distinct() //
                        .sorted() //
                        .forEach(currency -> {
                            if (currency.equals(reportingCurrency))
                                return;

                            var reason = reasonOf(factory.getTimeSeries(currency, reportingCurrency));
                            if (reason != null)
                                warnings.add(new Warning(currency, reportingCurrency, reason));
                        });

        return warnings;
    }

    /** every currency the file uses, in the order {@link Client} already sorts */
    /* package */ static List<Warning> list(Client client, ExchangeRateProviderFactory factory,
                    String reportingCurrency)
    {
        return list(currenciesOf(client), factory, reportingCurrency);
    }

    private static List<String> currenciesOf(Client client)
    {
        return client.getUsedCurrencies().stream() //
                        .map(CurrencyUnit::getCurrencyCode) //
                        .toList();
    }

    /**
     * Why this pair converts at 1:1, or null when it converts properly.
     * <p/>
     * Whether a series answers is the same at every date: one holding rates
     * answers any date by clamping to its first or last quote, one holding none
     * answers nothing, and the placeholder answers everything with 1. So a
     * report's valuation date cannot change this verdict - and the date that does
     * change the number, one before the first quote, is the case above that is
     * deliberately not reported.
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
