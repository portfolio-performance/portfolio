package name.abuchen.portfolio.online.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ExchangeLabels
{
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.online.impl.exchange-labels"; //$NON-NLS-1$
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    public static String getString(String key)
    {
        try
        {
            return BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            // just return key w/o provider prefix
            int p = key.indexOf('.');
            return p >= 0 ? key.substring(p + 1) : key;
        }
    }

    public static List<String> getAllExchangeKeys(String providerPrefix)
    {
        int length = providerPrefix.length();

        List<String> answer = new ArrayList<>();

        Enumeration<String> keys = BUNDLE.getKeys();
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement();

            if (!key.startsWith(providerPrefix))
                continue;

            answer.add(key.substring(length));
        }

        Collections.sort(answer);

        return answer;
    }
}
