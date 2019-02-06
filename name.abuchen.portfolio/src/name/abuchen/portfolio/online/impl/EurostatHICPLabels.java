package name.abuchen.portfolio.online.impl;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class EurostatHICPLabels
{
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.online.impl.eurostathicp-labels"; //$NON-NLS-1$
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    public static String getString(String key)
    {
        try
        {
            return BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            return key;
        }
    }
}
