package name.abuchen.portfolio.online;

import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.Messages;

/**
 * Returns the labels based on the FIGI security types.
 * 
 * @see https://www.openfigi.com
 */
@SuppressWarnings("nls")
public class OpenFIGI
{
    private static final Map<String, String> identifier2label = new HashMap<>();

    static
    {
        identifier2label.put("common stock", Messages.LabelSearchShare);
        identifier2label.put("mutual fund", Messages.LabelSearchMutualFund);
        identifier2label.put("reit", Messages.LabelSearchReit);
        identifier2label.put("index", Messages.LabelSearchIndex);
        identifier2label.put("crypto", Messages.LabelSearchCryptoCurrency);
        identifier2label.put("warrant", Messages.LabelSearchWarrant);
        identifier2label.put("closed-end fund", Messages.LabelSearchCloseEndFund);
        identifier2label.put("bond", Messages.LabelSearchBond);
    }

    private OpenFIGI()
    {
    }

    public static String getTypeLabel(String securityType)
    {
        if (securityType == null)
            return null;

        return identifier2label.getOrDefault(securityType.toLowerCase(), securityType);
    }
}
