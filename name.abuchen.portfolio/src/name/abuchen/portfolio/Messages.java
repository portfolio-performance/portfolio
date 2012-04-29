package name.abuchen.portfolio;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.messages"; //$NON-NLS-1$
    public static String ColumnCapitalGains;
    public static String ColumnEarnings;
    public static String ColumnFinalValue;
    public static String ColumnInitialValue;
    public static String ColumnPaidFees;
    public static String ColumnPaidTaxes;
    public static String ColumnPerformance;
    public static String ColumnTransfers;
    public static String LabelDeposits;
    public static String LabelInterest;
    public static String LabelJointPortfolio;
    public static String LabelPortfolio;
    public static String LabelRemovals;
    public static String LabelYahooFinance;
    public static String MsgErrorsConvertingValue;
    public static String MsgMissingResponse;
    public static String MsgMissingTickerSymbol;
    public static String MsgMoreResulstsAvailable;
    public static String MsgNoResults;
    public static String MsgResponseContainsNoIndices;
    public static String MsgUnexpectedHeader;
    public static String MsgUnexpectedTag;
    public static String MsgUnexpectedValue;
    public static String MsgUnsupportedVersionClientFiled;
    public static String QuoteFeedManual;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {}
}
