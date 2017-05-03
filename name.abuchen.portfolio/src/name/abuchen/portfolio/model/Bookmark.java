package name.abuchen.portfolio.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Bookmark
{
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

    public String constructURL(Security security)
    {
        boolean replacementDone = Boolean.FALSE;


        HashMap<String, String> types = new HashMap<String, String>();
        types.put("tickerSymbol", security.getTickerSymbol());
        types.put("isin",  security.getIsin());
        types.put("wkn", security.getWkn());
        types.put("name",  security.getName());

        List<String> patterns = new ArrayList<>();

        Pattern p = Pattern.compile("\\{(.*?)\\}");
        Matcher m = p.matcher(pattern);
        while(m.find()) {
            Arrays.stream(m.group(1).split(","))
            .filter(t -> pattern.contains(t) )
            .forEach(patterns::add);
        }
        
        String url = pattern.replace(",", encode("")).replace("{",encode("")).replace("}", encode(""));
        
        for( String key :patterns){
            try
            {
                String replacement = types.get(key);
                if(!replacementDone && replacement != null && replacement.length() > 0 ){
                        url = url.replace(key, encode(replacement));
                        replacementDone = Boolean.TRUE;
                }
                else 
                    url = url.replace(key, encode(""));
                
        
            }
            catch ( SecurityException e)
            {
                throw new RuntimeException(e);
            }

        }
        
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
