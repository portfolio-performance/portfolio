package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.money.CurrencyConverterImpl;

/**
 * Which currency pairs a report converted at 1:1 because there is nothing to
 * convert with.
 * <p/>
 * {@link CurrencyConverterImpl} substitutes a rate of 1 in exactly two cases:
 * the pair has no time series at all - the factory answers that with a
 * placeholder series that returns 1 on every lookup - or a series exists but
 * answers nothing for the date asked, so the converter falls back on its own. A
 * series that does answer a date before its first quote with that first quote
 * commits an inaccuracy of a different kind, not a 1:1 conversion, and is
 * deliberately not reported here.
 * <p/>
 * A money field cannot say "this number is unconverted" without becoming a second
 * shape for every amount, so the report says it once as {@code fxWarnings}: always
 * present, empty when nothing fell back - the same rule as {@code warnings} on a
 * computed collection. Which pairs fell back is decided while the report is
 * computed, by a {@link ConversionRecorder}, for the date of each conversion.
 */
public final class FxWarnings
{
    /** no exchange rate series exists for the pair at all */
    /* package */ static final String NO_SERIES = "no-series"; //$NON-NLS-1$

    /** a series exists, but answered no rate for a date the report converted at */
    /* package */ static final String NO_RATE = "no-rate"; //$NON-NLS-1$

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

    /**
     * Adds {@code fxWarnings} for every pair the recorder saw fall back - always
     * present, empty when nothing fell back.
     */
    public static void attach(JsonObject report, ConversionRecorder conversions)
    {
        var warnings = new JsonArray();
        for (var warning : conversions.fallbacks())
            warnings.add(EntityJson.toJson(warning));
        report.add("fxWarnings", warnings); //$NON-NLS-1$
    }
}
