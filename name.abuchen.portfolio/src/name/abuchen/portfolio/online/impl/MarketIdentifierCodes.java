package name.abuchen.portfolio.online.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Returns the labels based on the ISO 10383 market identifier code.
 * 
 * @see https://www.iso20022.org/market-identifier-codes
 */
@SuppressWarnings("nls")
public class MarketIdentifierCodes
{
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.online.impl.market-identifier-codes"; //$NON-NLS-1$
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private static final Map<String, String> MIC2YAHOO = new HashMap<>();

    static
    {
        MIC2YAHOO.put("ALXL", "LS");
        MIC2YAHOO.put("ASEX", "AT");
        MIC2YAHOO.put("BVCA", "CR");
        MIC2YAHOO.put("DSMD", "QA");
        MIC2YAHOO.put("MISX", "ME");
        MIC2YAHOO.put("XAMS", "AS");
        MIC2YAHOO.put("XBER", "BE");
        MIC2YAHOO.put("XBKK", "BK");
        MIC2YAHOO.put("XBOM", "BO");
        MIC2YAHOO.put("XBRU", "BR");
        MIC2YAHOO.put("XBUD", "BD");
        MIC2YAHOO.put("XBUE", "BA");
        MIC2YAHOO.put("XCNQ", "CN");
        MIC2YAHOO.put("XDUS", "DU");
        MIC2YAHOO.put("XETR", "DE");
        MIC2YAHOO.put("XFRA", "F");
        MIC2YAHOO.put("XHAM", "HM");
        MIC2YAHOO.put("XHAN", "HA");
        MIC2YAHOO.put("XHKG", "HK");
        MIC2YAHOO.put("XIDX", "JK");
        MIC2YAHOO.put("XIST", "IS");
        MIC2YAHOO.put("XJPX", "T");
        MIC2YAHOO.put("XJSE", "JO");
        MIC2YAHOO.put("XKLS", "KL");
        MIC2YAHOO.put("XLON", "L");
        MIC2YAHOO.put("XMEX", "MX");
        MIC2YAHOO.put("XMIL", "MI");
        MIC2YAHOO.put("XMUN", "MU");
        MIC2YAHOO.put("XNSE", "NS");
        MIC2YAHOO.put("XOSL", "OL");
        MIC2YAHOO.put("XPAR", "PA");
        MIC2YAHOO.put("XSAU", "SAU");
        MIC2YAHOO.put("XSGO", "SN");
        MIC2YAHOO.put("XSHE", "SZ");
        MIC2YAHOO.put("XSHG", "SS");
        MIC2YAHOO.put("XSTU", "SG");
        MIC2YAHOO.put("XTAE", "TA");
        MIC2YAHOO.put("XTAI", "TWO");
        MIC2YAHOO.put("XTSE", "TO");
        MIC2YAHOO.put("XTSX", "V");
        MIC2YAHOO.put("XWBO", "VI");
    }

    private MarketIdentifierCodes()
    {
    }

    public static String getLabel(String marketIdentifierCode)
    {
        try
        {
            if (marketIdentifierCode == null)
                return "";

            // we have translations available for exchange codes used by Yahoo
            // but not for market identifier code (MIC). Use the translated
            // labels if available. In the future, the code should prefer market
            // identifier codes over other custom exchange codes.

            var yahoo = MIC2YAHOO.get(marketIdentifierCode);
            return yahoo != null ? ExchangeLabels.getYahoo(yahoo) : BUNDLE.getString(marketIdentifierCode);
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
