package name.abuchen.portfolio.ui.handlers;

/**
 * Helper class to detect whether PP is running on a Unity Desktop. It is used
 * to enable menu items on Unity by default because the enablement state is
 * checked only once.
 */
public class UnityDesktopHelper
{
    private static boolean isUnity;

    static
    {
        String property = System.getProperty("name.abuchen.portfolio.unity-desktop"); //$NON-NLS-1$

        if (property != null)
        {
            isUnity = Boolean.parseBoolean(property);
        }
        else
        {
            isUnity = "Unity".equals(System.getenv("XDG_CURRENT_DESKTOP")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private UnityDesktopHelper()
    {
    }

    public static boolean isUnity()
    {
        return isUnity;
    }
}
