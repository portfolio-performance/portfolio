package name.abuchen.portfolio.ui.parts;

import java.util.ResourceBundle;

public enum SampleMessages {
    
    Kommer_RiskFreePart,
    Kommer_RiskBasedPart,
    Kommer_Commodities,
    Kommer_RealAssetsWorldwide,
    Kommer_AssetsEmergingMarkets,
    Kommer_AssetsSmallCap,
    Kommer_AssetsDevMarkets,
    Kommer_AssetsLargeCapValue,
    Kommer_JapanAustralia,
    Kommer_WesternEurope,
    Kommer_USANorthAm,
    Kommer_WatchlistIndices,
    Kommer_WatchlistAsia,
    Kommer_CallDepositMoneyAccount,
    Kommer_SecuritiesAccount,
    Dax_Equity,
    Dax_Cash,
    Dax_Debt,
    Dax_RealEstate,
    Dax_Commodity,
    Dax_Industries,
    Dax_IndustriesSector,
    Dax_IndustriesGroup,
    Dax_IndustriesDomain,
    Dax_Branch;
    
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.ui.parts.samplemessages");
    
    public String toString() 
    {
        return RESOURCES.getString(name());
    }
} 
