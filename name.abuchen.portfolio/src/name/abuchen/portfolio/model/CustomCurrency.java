package name.abuchen.portfolio.model;

import java.util.Objects;

public class CustomCurrency
{
    private String currencyCode;
    private String displayName;
    private String currencySymbol;

    public CustomCurrency()
    {
        // needed by XStream
    }

    public CustomCurrency(String currencyCode, String displayName, String currencySymbol)
    {
        this.currencyCode = normalizeCurrencyCode(currencyCode);
        this.displayName = normalize(displayName);
        this.currencySymbol = normalize(currencySymbol);
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = normalizeCurrencyCode(currencyCode);
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = normalize(displayName);
    }

    public String getCurrencySymbol()
    {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol)
    {
        this.currencySymbol = normalize(currencySymbol);
    }

    public String getLabel()
    {
        return currencyCode + " - " + displayName; //$NON-NLS-1$
    }

    @Override
    public String toString()
    {
        return getLabel();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(currencyCode);
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

        CustomCurrency other = (CustomCurrency) obj;
        return Objects.equals(currencyCode, other.currencyCode);
    }

    private static String normalizeCurrencyCode(String value)
    {
        return normalize(value).toUpperCase();
    }

    private static String normalize(String value)
    {
        return value != null ? value.trim() : ""; //$NON-NLS-1$
    }
}
