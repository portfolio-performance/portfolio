package name.abuchen.portfolio.online;

import java.net.URI;
import java.net.URISyntaxException;

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

    public URI constructURL(Security security) throws URISyntaxException
    {
        String url = pattern.replace("{tickerSymbol}", security.getTickerSymbol()); //$NON-NLS-1$
        url = url.replace("{isin}", security.getIsin()); //$NON-NLS-1$
        return new URI(url);
    }
}
