package name.abuchen.portfolio.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bookmark
{
    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("\\{([^}]*)\\}"); //$NON-NLS-1$

    private String label;
    private String pattern;

    public Bookmark(String label, String pattern)
    {
        this.label = label;
        this.pattern = pattern;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public void setPattern(String pattern)
    {
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

    public String constructURL(Client client, Security security)
    {
        Map<String, String> types = new HashMap<>();
        types.put("tickerSymbol", security.getTickerSymbol()); //$NON-NLS-1$
        types.put("tickerSymbolPrefix", getTickerPrefix(security.getTickerSymbol())); //$NON-NLS-1$
        types.put("isin", security.getIsin()); //$NON-NLS-1$
        types.put("wkn", security.getWkn()); //$NON-NLS-1$
        types.put("name", security.getName()); //$NON-NLS-1$

        client.getSettings().getAttributeTypes() //
                        .filter(a -> a.supports(Security.class)) //
                        .filter(a -> !types.containsKey(a.getColumnLabel())) //
                        .forEach(attrib -> {
                            Object value = security.getAttributes().get(attrib);
                            types.put(attrib.getColumnLabel(), attrib.getConverter().toString(value));
                        });

        StringBuilder answer = new StringBuilder();
        int position = 0;

        Matcher matcher = REPLACEMENT_PATTERN.matcher(pattern);

        while (matcher.find())
        {
            answer.append(pattern.substring(position, matcher.start()));
            position = matcher.end();

            for (String key : matcher.group(1).split(",")) //$NON-NLS-1$
            {
                String replacement = types.get(key);
                if (replacement != null && !replacement.isEmpty())
                {
                    answer.append(encode(replacement));
                    break;
                }
            }
        }

        answer.append(pattern.substring(position));

        return answer.toString();
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

    /**
     * Returns the prefix of a ticker symbol, e.g. without exchange rate suffix.
     * For example, for "BAS.DE" it would return "BAS".
     */
    private String getTickerPrefix(String tickerSymbol)
    {
        if (tickerSymbol == null)
            return null;

        int dot = tickerSymbol.indexOf('.');
        return dot > 0 ? tickerSymbol.substring(0, dot) : tickerSymbol;
    }
}
