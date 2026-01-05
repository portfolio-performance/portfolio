package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
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
        var codes = BUNDLE.getKeys();
        while (codes.hasMoreElements())
        {
            var currencyCode = codes.nextElement();
            if (currencyCode.indexOf('.') >= 0)
                continue;

            var displayName = BUNDLE.getString(currencyCode);

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

    public static CurrencyUnit getDefaultInstance(Locale locale)
    {
        try
        {
            if (!Set.of(Locale.getISOCountries()).contains(locale.getCountry()))
                return CurrencyUnit.getInstance(EUR);

            var defaultCurrencyISO4217 = java.util.Currency.getInstance(locale);
            if (defaultCurrencyISO4217 == null || defaultCurrencyISO4217.getCurrencyCode() == null)
                return CurrencyUnit.getInstance(EUR);

            var defaultCurrencyUnit = CurrencyUnit.getInstance(defaultCurrencyISO4217.getCurrencyCode());
            return defaultCurrencyUnit != null ? defaultCurrencyUnit : CurrencyUnit.getInstance(EUR);
        }
        catch (NullPointerException | IllegalArgumentException e)
        {
            return CurrencyUnit.getInstance(EUR);
        }
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

    public static CurrencyUnit getInstanceByDisplayName(String displayName)
    {
        if (displayName == null)
            return null;

        for (CurrencyUnit unit : CACHE.values())
        {
            if (displayName.equalsIgnoreCase(unit.getDisplayName()))
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
            var letter = unit.getCurrencyCode().charAt(0) - 'A';
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
        final var prime = 31;
        var result = 1;
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

        var other = (CurrencyUnit) obj;
        if (!Objects.equals(currencyCode, other.currencyCode))
            return false;
        return true;
    }
}
