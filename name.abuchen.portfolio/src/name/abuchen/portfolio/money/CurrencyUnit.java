package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NavigableMap;
import java.util.ResourceBundle;
import java.util.TreeMap;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.util.Pair;

public final class CurrencyUnit implements Comparable<CurrencyUnit>
{
    public static final CurrencyUnit EMPTY = new CurrencyUnit(Messages.LabelNoCurrency,
                    Messages.LabelNoCurrencyDescription, null);
    public static final String EUR = "EUR"; //$NON-NLS-1$
    public static final String USD = "USD"; //$NON-NLS-1$

    private static final String BUNDLE_NAME = "name.abuchen.portfolio.money.currencies"; //$NON-NLS-1$
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
    private static final Map<String, CurrencyUnit> CACHE = new HashMap<>();

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
            catch (MissingResourceException ignore) // NOSONAR
            {
                // no symbol defined
            }

            CACHE.put(currencyCode, new CurrencyUnit(currencyCode, displayName, currencySymbol));
        }
    }

    private String currencyCode;
    private String displayName;
    private String currencySymbol;

    private CurrencyUnit(String currencyCode, String displayName, String currencySymbol)
    {
        this.currencyCode = currencyCode;
        this.displayName = displayName;
        this.currencySymbol = currencySymbol;
    }

    public static List<CurrencyUnit> getAvailableCurrencyUnits()
    {
        return new ArrayList<>(CACHE.values());
    }

    public static CurrencyUnit getInstance(String currencyCode)
    {
        return CACHE.get(currencyCode);
    }

    public static CurrencyUnit getInstanceBySymbol(String currencySymbol)
    {
        if (currencySymbol == null)
            return null;

        for (CurrencyUnit unit : CACHE.values())
        {
            if (currencySymbol.equals(unit.getCurrencySymbol()))
                return unit;
        }

        return null;
    }

    public static boolean containsCurrencyCode(String currencyCode)
    {
        return CACHE.containsKey(currencyCode);
    }

    public static List<Pair<String, List<CurrencyUnit>>> getAvailableCurrencyUnitsGrouped()
    {
        NavigableMap<Integer, Pair<String, List<CurrencyUnit>>> sublists = new TreeMap<>();
        sublists.put(0, new Pair<>("A - D", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(4, new Pair<>("E - I", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(9, new Pair<>("J - M", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(13, new Pair<>("N - R", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(18, new Pair<>("S - V", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(22, new Pair<>("W - Z", new ArrayList<>())); //$NON-NLS-1$

        for (CurrencyUnit unit : CACHE.values())
        {
            int letter = unit.getCurrencyCode().charAt(0) - 'A';
            sublists.floorEntry(letter).getValue().getRight().add(unit);
        }

        List<Pair<String, List<CurrencyUnit>>> answer = new ArrayList<>(sublists.values());

        for (Pair<String, List<CurrencyUnit>> pair : answer)
            Collections.sort(pair.getRight());

        return answer;
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
