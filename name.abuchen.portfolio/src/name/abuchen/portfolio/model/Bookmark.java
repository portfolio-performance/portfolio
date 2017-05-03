package name.abuchen.portfolio.model;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;


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
        String url = pattern;
        Class<? extends Security> securityClass = security.getClass();
        HashMap<String, String> types = new HashMap<String, String>();
        TreeMap <Integer,Entry<String, String>> replaceTypes = new TreeMap<Integer,Entry<String, String>>();

        
        
        types.put("{tickerSymbol}", "getTickerSymbol");
        types.put("{isin}",  "getIsin");
        types.put("{wkn}", "getWkn");
        types.put("{name}",  "getName");

       
        for(Entry<String, String> type : types.entrySet()){
            if(pattern.contains(type.getKey()))
              replaceTypes.put(pattern.indexOf(type.getKey()), type);
        }

        for( Integer key :replaceTypes.keySet()){
            Entry<String, String> replacement = replaceTypes.get(key);
            Method method;
            try
            {
                method = securityClass.getMethod(replacement.getValue());
                String queryString = (String) method.invoke(security);
                
                if(!replacementDone && queryString!= null && queryString.length() > 0 ){
                        url = url.replace(replacement.getKey(), encode(queryString));
                        replacementDone = Boolean.TRUE;
                }
                else 
                    url = url.replace(replacement.getKey(), encode(""));
            }
            catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
            {
                e.printStackTrace();
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
