package name.abuchen.portfolio.online.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Returns the labels based on the ISO 10383 market identifier code.
 * 
 * @see https://www.iso20022.org/market-identifier-codes
 */
public class MarketIdentifierCodes
{
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.online.impl.market-identifier-codes"; //$NON-NLS-1$
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private MarketIdentifierCodes()
    {
    }

    public static String getLabel(String marketIdentifierCode)
    {
        try
        {
            return BUNDLE.getString(marketIdentifierCode);
        }
        catch (MissingResourceException e)
        {
            return marketIdentifierCode;
        }
    }

    public static List<String> getAllMarketIdentifierCodes()
    {
        List<String> answer = new ArrayList<>();

        Enumeration<String> keys = BUNDLE.getKeys();
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement();
            answer.add(key);
        }

        Collections.sort(answer);

        return answer;
    }
}
