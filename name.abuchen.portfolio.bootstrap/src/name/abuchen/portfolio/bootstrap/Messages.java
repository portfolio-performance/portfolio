package name.abuchen.portfolio.bootstrap;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.bootstrap.messages"; //$NON-NLS-1$
    public static String MsgMinimumRequiredVersion;
    public static String TitleJavaVersion;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {}
}
