package name.abuchen.portfolio.online;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

    public String constructURL(Security security)
    {
        String url = pattern.replace("{tickerSymbol}", encode(security.getTickerSymbol())); //$NON-NLS-1$
        url = url.replace("{isin}", encode(security.getIsin())); //$NON-NLS-1$
        return url;
    }

    private String encode(String s)
    {
        try
        {
            return s == null ? "" : URLEncoder.encode(s, "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch (UnsupportedEncodingException ignore)
        {
            // UTF-8 is always supported
            return s;
        }
    }
}
