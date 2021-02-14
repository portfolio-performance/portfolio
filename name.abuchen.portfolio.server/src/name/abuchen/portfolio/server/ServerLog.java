package name.abuchen.portfolio.server;

import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

public class ServerLog
{
    private static final String PLUGIN_ID = "name.abuchen.portfolio.server"; //$NON-NLS-1$

    private ServerLog()
    {}

    private static void log(IStatus status)
    {
        try
        {
            Platform.getLog(FrameworkUtil.getBundle(ServerLog.class)).log(status);
        }
        catch (NullPointerException e)
        {
            System.err.println(status); // NOSONAR
        }
    }

    public static void error(Throwable t)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

    public static void error(List<? extends Throwable> errors)
    {
        ILog log = Platform.getLog(FrameworkUtil.getBundle(ServerLog.class));
        for (Throwable t : errors)
            log.log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

    public static void error(String message)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    public static void warning(String message)
    {
        log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }
}
