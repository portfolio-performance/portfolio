package name.abuchen.portfolio.model;

import java.util.Locale;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.money.Values;

/**
 * Partial exemption (Teilfreistellung) of an investment fund held by a private
 * investor according to § 20 InvStG. The value is the fraction of the gains
 * that is exempt from tax, for example 0.30 for equity funds.
 */
public enum PartialExemption
{
    NONE(0.00, Messages.PartialExemptionNone), //
    MIXED_FUND(0.15, Messages.PartialExemptionMixedFund), //
    EQUITY_FUND(0.30, Messages.PartialExemptionEquityFund), //
    REAL_ESTATE_FUND(0.60, Messages.PartialExemptionRealEstateFund), //
    FOREIGN_REAL_ESTATE_FUND(0.80, Messages.PartialExemptionForeignRealEstateFund);

    private static final double PRECISION = 1E-9;

    private final double value;
    private final String description;

    private PartialExemption(double value, String description)
    {
        this.value = value;
        this.description = description;
    }

    public double getValue()
    {
        return value;
    }

    /**
     * German language without a country also counts as Germany because the
     * language preference offers a plain German entry.
     */
    public static boolean isRelevantFor(Locale locale)
    {
        if (locale == null)
            return false;

        var country = locale.getCountry();
        return country.isEmpty() ? Locale.GERMAN.getLanguage().equals(locale.getLanguage())
                        : Locale.GERMANY.getCountry().equals(country);
    }

    /**
     * Label for the selection: the percentage which is stored together with the
     * fund types the rate applies to.
     */
    public String getLabel()
    {
        return Values.Percent2.format(Double.valueOf(value)) + " - " + description; //$NON-NLS-1$
    }

    public static Optional<PartialExemption> of(Double value)
    {
        if (value == null)
            return Optional.empty();

        for (var candidate : values())
            if (Math.abs(candidate.value - value) < PRECISION)
                return Optional.of(candidate);

        return Optional.empty();
    }

    public static String[] getLabels()
    {
        var candidates = values();
        var labels = new String[candidates.length];

        for (var ii = 0; ii < candidates.length; ii++)
            labels[ii] = candidates[ii].getLabel();

        return labels;
    }

    /**
     * Returns the value at the given selection index or null if the index does
     * not denote an option.
     */
    public static Double valueAt(int index)
    {
        var candidates = values();
        return index >= 0 && index < candidates.length ? Double.valueOf(candidates[index].value) : null;
    }

    /**
     * Returns the selection index of the given value or -1 if the value is not
     * one of the statutory rates, for example because it was entered before the
     * selection was introduced.
     */
    public static int indexOf(Double value)
    {
        return of(value).map(PartialExemption::ordinal).orElse(-1);
    }
}
