package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

/**
 * Which currency pairs a report converted at 1:1 because there is nothing to
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
 * The pairs checked are the ones the report's calculation actually converted, as
 * a {@link ConversionRecorder} saw them - not the currencies the file happens to
 * use, which would warn about money that never reached the response.
 */
public final class FxWarnings
{
    /** no exchange rate series exists for the pair at all */
    private static final String NO_SERIES = "no-series"; //$NON-NLS-1$

    /** a series exists, but holds no rates, so every date falls back */
    private static final String NO_RATE = "no-rate"; //$NON-NLS-1$

    /**
     * One pair that was converted at 1:1.
     *
     * @param currency
     *            the currency the amounts were in
     * @param reportingCurrency
     *            the currency they were expressed in - the report's reporting
     *            currency, or an instrument's own currency for its local figures
     */
    public record Warning(String currency, String reportingCurrency, String reason)
    {
    }

    private FxWarnings()
    {
    }

    /** adds {@code fxWarnings} for every pair the recorder saw that fell back */
    public static void attach(JsonObject report, ConversionRecorder conversions, ExchangeRateProviderFactory factory)
    {
        var warnings = new JsonArray();
        for (var warning : list(conversions.pairs(), factory))
            warnings.add(EntityJson.toJson(warning));
        report.add("fxWarnings", warnings); //$NON-NLS-1$
    }

    /** the pairs that fell back, in the order given */
    /* package */ static List<Warning> list(Collection<ConversionRecorder.Pair> pairs,
                    ExchangeRateProviderFactory factory)
    {
        var warnings = new ArrayList<Warning>();
        for (var pair : pairs)
        {
            var reason = reasonOf(pair, factory);
            if (reason != null)
                warnings.add(new Warning(pair.base(), pair.term(), reason));
        }
        return warnings;
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
     */
    private static String reasonOf(ConversionRecorder.Pair pair, ExchangeRateProviderFactory factory)
    {
        if (!factory.hasTimeSeries(pair.base(), pair.term()))
            return NO_SERIES;

        // a series holding no rates answers nothing at any date; today is the
        // date the converter's own fallback carries
        var series = factory.getTimeSeries(pair.base(), pair.term());
        return series.lookupRate(LocalDate.now()).isPresent() ? null : NO_RATE;
    }
}
