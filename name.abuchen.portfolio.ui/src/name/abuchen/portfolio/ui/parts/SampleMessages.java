package name.abuchen.portfolio.ui.parts;

import org.eclipse.osgi.util.NLS;

public class SampleMessages extends NLS {
    
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.ui.parts.samplemessages"; //$NON-NLS-1$

    public static String Kommer_RiskFreePart;
    public static String Kommer_RiskBasedPart;
    public static String Kommer_Commodities;
    public static String Kommer_RealAssetsWorldwide;
    public static String Kommer_AssetsEmergingMarkets;
    public static String Kommer_AssetsSmallCap;
    public static String Kommer_AssetsDevMarkets;
    public static String Kommer_AssetsLargeCapValue;
    public static String Kommer_JapanAustralia;
    public static String Kommer_WesternEurope;
    public static String Kommer_USANorthAm;
    public static String Kommer_WatchlistIndices;
    public static String Kommer_WatchlistAsia;
    public static String Kommer_CallDepositMoneyAccount;
    public static String Kommer_SecuritiesAccount;
    public static String Dax_Equity;
    public static String Dax_Cash;
    public static String Dax_Debt;
    public static String Dax_RealEstate;
    public static String Dax_Commodity;
    public static String Dax_Industries;
    public static String Dax_IndustriesSector;
    public static String Dax_IndustriesGroup;
    public static String Dax_IndustriesDomain;
    public static String Dax_Branch;
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SampleMessages.class);
    }

    private SampleMessages() {
    }
} 
