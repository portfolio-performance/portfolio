package name.abuchen.portfolio;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

public class PortfolioLog
{
    /**
     * Plugin ID for logging.
     */
    private static final String PLUGIN_ID = "name.abuchen.portfolio"; //$NON-NLS-1$

    private PortfolioLog()
    {
    }

    private static void log(IStatus status)
    {
        Platform.getLog(FrameworkUtil.getBundle(PortfolioLog.class)).log(status);
    }

    /**
     * Logs the given error status to the application log.
     * 
     * @param t
     *            {@link Throwable}
     */
    public static void error(Throwable t)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

    /**
     * Logs the given error status to the application log.
     * 
     * @param message
     *            error message
     */
    public static void error(String message)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    /**
     * Logs the given warning status to the application log.
     * 
     * @param message
     *            warning message
     */
    public static void warning(String message)
    {
        log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

}
