package name.abuchen.portfolio.online;

import java.net.MalformedURLException;
import java.net.URL;

import name.abuchen.portfolio.model.Security;

public class WebLocation
{
    private String label;
    private String pattern;

    public WebLocation(String label, String pattern)
    {
        this.label = label;
        this.pattern = pattern;
    }

    public String getLabel()
    {
        return label;
    }

    public String getPattern()
    {
        return pattern;
    }

    public URL constructURL(Security security) throws MalformedURLException
    {
        String url = pattern.replace("{tickerSymbol}", security.getTickerSymbol()); //$NON-NLS-1$
        url = url.replace("{isin}", security.getIsin()); //$NON-NLS-1$
        return new URL(url);
    }
}
