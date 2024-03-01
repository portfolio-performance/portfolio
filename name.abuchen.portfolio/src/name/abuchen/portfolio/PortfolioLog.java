package name.abuchen.portfolio;

import java.util.List;

import org.eclipse.core.runtime.ILog;
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
        try
        {
            Platform.getLog(FrameworkUtil.getBundle(PortfolioLog.class)).log(status);
        }
        catch (NullPointerException | IllegalArgumentException e)
        {
            // when running unit tests via Infinitest, the platform log is not
            // available
            System.err.println(status); // NOSONAR
        }
    }

    /**
     * Logs the given error to the application log.
     * 
     * @param t
     *            {@link Throwable}
     */
    public static void error(Throwable t)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

    /**
     * Logs the given errors to the application log.
     * 
     * @param t
     *            {@link Throwable}
     */
    public static void error(List<? extends Throwable> errors)
    {
        ILog log = Platform.getLog(FrameworkUtil.getBundle(PortfolioLog.class));
        for (Throwable t : errors)
            log.log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

    /**
     * Logs the messages of the given errors but without the full stack trace.
     * Instead, the message will include the calling class name only. This is
     * useful for common error messages which occur as part of the price
     * updates.
     */
    public static void abbreviated(List<? extends Throwable> errors)
    {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        Class<?> callerClass = walker.getCallerClass();

        ILog log = Platform.getLog(FrameworkUtil.getBundle(PortfolioLog.class));
        for (Throwable t : errors)
            log.log(new Status(IStatus.ERROR, PLUGIN_ID, "[" + callerClass.getSimpleName() + "] " + t.getMessage())); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Logs the message of the given error but without the full stack trace.
     * Instead, the message will include the calling class name only. This is
     * useful for common error messages which occur as part of the price
     * updates.
     */
    public static void abbreviated(Throwable error)
    {
        abbreviated(List.of(error));
    }

    /**
     * Logs the given error message to the application log.
     * 
     * @param message
     *            error message
     */
    public static void error(String message)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    /**
     * Logs the given warning message to the application log.
     * 
     * @param message
     *            warning message
     */
    public static void warning(String message)
    {
        log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

    /**
     * Logs an informational message to the application log.
     * 
     * @param message
     *            warning message
     */
    public static void info(String message)
    {
        log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

}
