package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import name.abuchen.portfolio.Messages;

public final class CurrencyUnit implements Comparable<CurrencyUnit>
{
    public static final CurrencyUnit EMPTY = new CurrencyUnit(Messages.LabelNoCurrency, Messages.LabelNoCurrencyDescription, null);
    public static final String EUR = "EUR"; //$NON-NLS-1$

    private static final String BUNDLE_NAME = "name.abuchen.portfolio.money.currencies"; //$NON-NLS-1$
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
    private static final Map<String, CurrencyUnit> CACHE = new HashMap<String, CurrencyUnit>();

    static
    {
        Enumeration<String> codes = BUNDLE.getKeys();
        while (codes.hasMoreElements())
        {
            String currencyCode = codes.nextElement();
            if (currencyCode.indexOf('.') >= 0)
                continue;

            String displayName = BUNDLE.getString(currencyCode);

            // currency symbol
            String currencySymbol = null;
            try
            {
                currencySymbol = BUNDLE.getString(currencyCode + ".symbol"); //$NON-NLS-1$
            }
            catch (MissingResourceException ignore)
            {
                // no symbol defined
            }

            CACHE.put(currencyCode, new CurrencyUnit(currencyCode, displayName, currencySymbol));
        }
    }

    private String currencyCode;
    private String displayName;
    private String currencySymbol;

    public static List<CurrencyUnit> getAvailableCurrencyUnits()
    {
        return new ArrayList<CurrencyUnit>(CACHE.values());
    }

    public static CurrencyUnit getInstance(String currencyCode)
    {
        return CACHE.get(currencyCode);
    }

    private CurrencyUnit(String currencyCode, String displayName, String currencySymbol)
    {
        this.currencyCode = currencyCode;
        this.displayName = displayName;
        this.currencySymbol = currencySymbol;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getCurrencySymbol()
    {
        return currencySymbol;
    }

    public String getLabel()
    {
        return MessageFormat.format(Messages.FixAssignCurrencyCode, currencyCode, displayName);
    }

    @Override
    public String toString()
    {
        return getLabel();
    }

    @Override
    public int compareTo(CurrencyUnit other)
    {
        return getCurrencyCode().compareTo(other.getCurrencyCode());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((currencyCode == null) ? 0 : currencyCode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CurrencyUnit other = (CurrencyUnit) obj;
        if (currencyCode == null)
        {
            if (other.currencyCode != null)
                return false;
        }
        else if (!currencyCode.equals(other.currencyCode))
            return false;
        return true;
    }
}
