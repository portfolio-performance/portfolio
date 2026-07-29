package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * The query parameters shared by the calculation endpoints. They accumulate
 * their violations into one list rather than throwing, so that a handler reports
 * every actionable problem in a single 400 and an agent self-corrects in one
 * round-trip.
 * <p/>
 * A parse that fails returns null - never a default. A value that never parsed
 * must not be judged by a later constraint, which would put a second, spurious
 * error on the same field.
 */
public final class CalcParams
{
    private CalcParams()
    {
    }

    public static LocalDate date(String field, String value, List<ApiException.FieldError> errors)
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

    /** the required opening date of an interval */
    public static LocalDate openingDate(String value, List<ApiException.FieldError> errors)
    {
        if (value == null)
        {
            errors.add(new ApiException.FieldError("openingDate", "required", "openingDate is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        return date("openingDate", value, errors); //$NON-NLS-1$
    }

    /** the closing date of an interval, defaulting to today when absent */
    public static LocalDate closingDate(String value, List<ApiException.FieldError> errors)
    {
        return value == null ? LocalDate.now() : date("closingDate", value, errors); //$NON-NLS-1$
    }

    /**
     * The range constraint depends on the two dates having parsed and on nothing
     * else, so it accumulates with the other violations instead of hiding behind
     * them. Skipped when either date is absent or unparseable - the constraint
     * cannot be judged then.
     */
    public static void requireRange(LocalDate openingDate, LocalDate closingDate,
                    List<ApiException.FieldError> errors)
    {
        if (openingDate != null && closingDate != null && !openingDate.isBefore(closingDate))
            errors.add(new ApiException.FieldError("closingDate", "invalid-range", //$NON-NLS-1$ //$NON-NLS-2$
                            "closingDate must be after openingDate")); //$NON-NLS-1$
    }

    /** the reporting currency, defaulting to the file's base currency */
    public static String currency(Client client, String value, List<ApiException.FieldError> errors)
    {
        if (value == null)
            return client.getBaseCurrency();

        if (CurrencyUnit.getInstance(value) == null)
        {
            errors.add(new ApiException.FieldError("currency", "unknown-currency", //$NON-NLS-1$ //$NON-NLS-2$
                            value + " is not a known currency")); //$NON-NLS-1$
            return client.getBaseCurrency();
        }

        return value;
    }

    public static CostMethod costMethod(String value, List<ApiException.FieldError> errors)
    {
        if (value == null || "fifo".equals(value)) //$NON-NLS-1$
            return CostMethod.FIFO;

        if ("moving-average".equals(value)) //$NON-NLS-1$
            return CostMethod.MOVING_AVERAGE;

        errors.add(new ApiException.FieldError("costMethod", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "costMethod must be fifo or moving-average")); //$NON-NLS-1$
        return CostMethod.FIFO;
    }

    public static String wireName(CostMethod costMethod)
    {
        return switch (costMethod)
        {
            case FIFO -> "fifo"; //$NON-NLS-1$
            case MOVING_AVERAGE -> "moving-average"; //$NON-NLS-1$
        };
    }

    public static TaxesAndFees taxesAndFees(String value, List<ApiException.FieldError> errors)
    {
        if (value == null || "included".equals(value)) //$NON-NLS-1$
            return TaxesAndFees.INCLUDED;

        if ("excluded".equals(value)) //$NON-NLS-1$
            return TaxesAndFees.NOT_INCLUDED;

        errors.add(new ApiException.FieldError("taxesAndFees", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "taxesAndFees must be included or excluded")); //$NON-NLS-1$
        return TaxesAndFees.INCLUDED;
    }

    public static String wireName(TaxesAndFees taxesAndFees)
    {
        return taxesAndFees.isIncluded() ? "included" : "excluded"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
