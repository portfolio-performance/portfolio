package name.abuchen.portfolio.build.rbe;

public final class Preferences
{
    public static final int NEW_LINE_UNIX = 0;
    public static final int NEW_LINE_WIN = 1;
    public static final int NEW_LINE_MAC = 2;

    private Preferences()
    {}

    public static boolean getConvertEncodedToUnicode()
    {
        return true;
    }

    public static String getKeyGroupSeparator()
    {
        return "."; //$NON-NLS-1$
    }

    public static boolean getAlignEqualSigns()
    {
        return true;
    }

    public static boolean getSpacesAroundEqualSigns()
    {
        return true;
    }

    public static boolean getGroupKeys()
    {
        return true;
    }

    public static int getGroupLevelDepth()
    {
        return 1;
    }

    public static int getGroupLineBreaks()
    {
        return 1;
    }

    public static boolean getGroupAlignEqualSigns()
    {
        return true;
    }

    public static boolean getShowGenerator()
    {
        return false;
    }

    public static int getWrapCharLimit()
    {
        return 80;
    }

    public static int getWrapIndentSpaces()
    {
        return 8;
    }

    public static boolean getWrapLines()
    {
        return false;
    }

    public static boolean getWrapAlignEqualSigns()
    {
        return true;
    }

    public static boolean getConvertUnicodeToEncoded()
    {
        return true;
    }

    public static boolean getConvertUnicodeToEncodedUpper()
    {
        return true;
    }

    public static int getNewLineType()
    {
        return NEW_LINE_UNIX;
    }

    public static boolean getNewLineNice()
    {
        return false;
    }

    public static boolean getKeepEmptyFields()
    {
        return false;
    }

    public static boolean getForceNewLineType()
    {
        return false;
    }
}
