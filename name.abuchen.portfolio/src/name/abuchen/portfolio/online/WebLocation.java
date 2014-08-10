package name.abuchen.portfolio.online;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    public URI constructURL(Security security) throws IOException, URISyntaxException
    {
        String url = pattern.replace("{tickerSymbol}", encode(security.getTickerSymbol())); //$NON-NLS-1$
        url = url.replace("{isin}", encode(security.getIsin())); //$NON-NLS-1$
        return new URI(url);
    }

    private String encode(String s) throws IOException
    {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8.name()); //$NON-NLS-1$
    }
}
