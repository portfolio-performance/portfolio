package name.abuchen.portfolio.json;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Security;

public class JSecurity
{
    private String name;
    private String isin;
    private String wkn;
    private String tickerSymbol;
    private String currency;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getIsin()
    {
        return isin;
    }

    public void setIsin(String isin)
    {
        this.isin = isin;
    }

    public String getWkn()
    {
        return wkn;
    }

    public void setWkn(String wkn)
    {
        this.wkn = wkn;
    }

    public String getTickerSymbol()
    {
        return tickerSymbol;
    }

    public void setTickerSymbol(String tickerSymbol)
    {
        this.tickerSymbol = tickerSymbol;
    }
    
    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    public static JSecurity from(Security security)
    {
        JSecurity s = new JSecurity();
        s.name = security.getName();
        s.isin = Strings.emptyToNull(security.getIsin());
        s.wkn = Strings.emptyToNull(security.getWkn());
        s.tickerSymbol = Strings.emptyToNull(security.getTickerSymbol());
        s.currency = Strings.emptyToNull(security.getCurrencyCode());
        return s;
    }
}
