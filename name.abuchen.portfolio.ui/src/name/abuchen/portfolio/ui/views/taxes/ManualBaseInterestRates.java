package name.abuchen.portfolio.ui.views.taxes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Optional;

import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * The base interest rates entered by the user. They are stored per portfolio
 * file in the preference store, because they override the published rates of a
 * single file only.
 */
/* package */ class ManualBaseInterestRates
{
    private static final String PREF_PREFIX = AdvanceLumpSumView.class.getSimpleName()
                    + "-manual-base-interest-rate-"; //$NON-NLS-1$

    private final IPreferenceStore preferences;

    /* package */ ManualBaseInterestRates(IPreferenceStore preferences)
    {
        this.preferences = preferences;
    }

    /* package */ Optional<BigDecimal> get(int year)
    {
        var value = preferences.getString(PREF_PREFIX + year);
        if (value.isEmpty())
            return Optional.empty();

        try
        {
            return Optional.of(new BigDecimal(value));
        }
        catch (NumberFormatException e)
        {
            PortfolioPlugin.log(e);
            return Optional.empty();
        }
    }

    /* package */ void put(int year, BigDecimal rate)
    {
        preferences.setValue(PREF_PREFIX + year, rate.toPlainString());
    }

    /* package */ void remove(int year)
    {
        preferences.setToDefault(PREF_PREFIX + year);
    }

    /* package */ static BigDecimal parsePercent(String text)
    {
        if (text == null)
            return null;

        var value = text.strip();
        if (value.endsWith("%")) //$NON-NLS-1$
            value = value.substring(0, value.length() - 1).strip();

        var format = (DecimalFormat) NumberFormat.getNumberInstance();
        format.setGroupingUsed(false);
        format.setParseBigDecimal(true);
        var decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        value = value.replace('.', decimalSeparator).replace(',', decimalSeparator);
        var position = new ParsePosition(0);
        return format.parse(value, position) instanceof BigDecimal number && position.getIndex() == value.length()
                        ? number.setScale(2, RoundingMode.HALF_UP)
                        : null;
    }
}
