package name.abuchen.portfolio.online.impl;

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
}
