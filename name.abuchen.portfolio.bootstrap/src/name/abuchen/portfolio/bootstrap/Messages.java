package name.abuchen.portfolio.bootstrap;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.bootstrap.messages"; //$NON-NLS-1$
    public static String LabelCancel;
    public static String LabelCloneWindow;
    public static String LabelCloseWindow;
    public static String LabelNo;
    public static String LabelSaveAll;
    public static String LabelSaveNone;
    public static String LabelYes;
    public static String MsgMinimumRequiredVersion;
    public static String SaveHandlerMsgSelectFileToSave;
    public static String SaveHandlerPrompt;
    public static String SaveHandlerTitle;
    public static String TitleJavaVersion;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
