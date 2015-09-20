package name.abuchen.portfolio.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Bookmark
{
    private String label;
    private String pattern;

    public Bookmark(String label, String pattern)
    {
        this.label = label;
        this.pattern = pattern;
    }

    public void setLabel(String label){
        this.label = label;
    }
    
    public String getLabel()
    {
        return label;
    }

    public void setPattern(String pattern){
        this.pattern = pattern;
    }
    
    public String getPattern()
    {
        return pattern;
    }

    public boolean isSeparator()
    {
        return "-".equals(label); //$NON-NLS-1$
    }

    public String constructURL(Security security)
    {
        String url = pattern.replace("{tickerSymbol}", encode(security.getTickerSymbol())); //$NON-NLS-1$
        url = url.replace("{isin}", encode(security.getIsin())); //$NON-NLS-1$
        url = url.replace("{wkn}", encode(security.getWkn()));   //$NON-NLS-1$
        url = url.replace("{name}", encode(security.getName())); //$NON-NLS-1$
        
        return url;
    }

    private String encode(String s)
    {
        try
        {
            return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8.name()); //$NON-NLS-1$
        }
        catch (UnsupportedEncodingException e)
        {
            // should not happen as UTF-8 is always supported
            throw new UnsupportedOperationException(e);
        }
    }
}
