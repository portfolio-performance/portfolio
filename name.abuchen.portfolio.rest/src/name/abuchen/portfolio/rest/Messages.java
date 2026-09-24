package name.abuchen.portfolio.rest;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
    public static String MsgApiFileOpened;
    public static String MsgApiFileSaved;
    public static String MsgApiInstrumentChanged;
    public static String MsgApiInstrumentDeleted;
    public static String MsgApiInstrumentFieldChanged;
    public static String MsgApiTransactionCreated;
    public static String MsgApiTransactionDeleted;
    public static String MsgApiTransactionUpdated;
    public static String MsgApiValueUnset;
    public static String MsgApiValueRemoved;
    public static String MsgErrorAliasAlreadyInUse;
    public static String MsgErrorAliasMustMatchPattern;
    public static String MsgErrorAliasMustNotLookLikeUUID;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
