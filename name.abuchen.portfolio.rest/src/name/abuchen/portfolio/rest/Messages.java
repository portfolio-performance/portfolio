package name.abuchen.portfolio.rest;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
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
