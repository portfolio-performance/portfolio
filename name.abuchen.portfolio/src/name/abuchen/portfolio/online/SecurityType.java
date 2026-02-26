package name.abuchen.portfolio.online;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.Messages;

@SuppressWarnings("nls")
public class SecurityType
{
    private static Map<String, String> typeMap;

    static
    {
        typeMap = new HashMap<>();

        // Convert the security type to a standard value
        typeMap.put("bond", Messages.LabelSearchBond);
        typeMap.put("closed-end fund", Messages.LabelSearchCloseEndFund);
        typeMap.put("common", Messages.LabelSearchShare);
        typeMap.put("common stock", Messages.LabelSearchShare);
        typeMap.put("currency", Messages.LabelSearchCurrency);
        typeMap.put("digital currency", Messages.LabelSearchCryptoCurrency);
        typeMap.put("cryptocurrency", Messages.LabelSearchCryptoCurrency);
        typeMap.put("crypto", Messages.LabelSearchCryptoCurrency);
        typeMap.put("etf", Messages.LabelSearchETF);
        typeMap.put("etc", Messages.LabelSearchETC);
        typeMap.put("exchange-traded note", Messages.LabelSearchETN);
        typeMap.put("equity", Messages.LabelSearchShare);
        typeMap.put("fund", Messages.LabelSearchFund);
        typeMap.put("future", Messages.LabelSearchFuture);
        typeMap.put("index", Messages.LabelSearchIndex);
        typeMap.put("mutual fund", Messages.LabelSearchMutualFund);
        typeMap.put("mutualfund", Messages.LabelSearchMutualFund);
        typeMap.put("new york registered shares", Messages.LabelSearchShare);
        typeMap.put("physical currency", Messages.LabelSearchCurrency);
        typeMap.put("preferred stock", Messages.LabelSearchPreferredStock);
        typeMap.put("real estate investment trust (reit)", Messages.LabelSearchReit);
        typeMap.put("reit", Messages.LabelSearchReit);
        typeMap.put("warrant", Messages.LabelSearchWarrant);
        typeMap.put("commodity", Messages.LabelCommodity);
    }

    private SecurityType()
    {
    }

    public static String convertType(String type)
    {
        if (type == null)
            return null;

        // keep original capitalization for unknown types
        return typeMap.getOrDefault(trim(type).toLowerCase(), type);
    }
}
