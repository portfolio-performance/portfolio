package name.abuchen.portfolio.model;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class PredefinedSecurity
{

    public static Map<String, PredefinedSecurity> PREDEFINED_SECURITIES = new HashMap<String, PredefinedSecurity>();
    
    public static Map<String, PredefinedSecurity> read(String identifier) {
        ResourceBundle bundle = ResourceBundle
                        .getBundle("name.abuchen.portfolio.model." + identifier + "-security"); //$NON-NLS-1$ //$NON-NLS-2$
        Enumeration<String> securities = bundle.getKeys();
        while (securities.hasMoreElements()) {
            String security = securities.nextElement();
            String name = bundle.getString(security);
            PREDEFINED_SECURITIES.put(security, new PredefinedSecurity(security, name));
        }
        return PREDEFINED_SECURITIES;
    }
    
    String symbol, name;
    
    public PredefinedSecurity(String symbol, String name)
    {
        this.symbol = symbol;
        this.name = name;
    }

    public String getSymbol()
    {
        return symbol;
    }

    
    public String getName()
    {
        return name;
    }

}
