package name.abuchen.portfolio.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.FrameworkUtil;

public class PortfolioPlugin extends AbstractUIPlugin
{
    public static final String PLUGIN_IN = "name.abuchen.portfolio.ui"; //$NON-NLS-1$

    public static final void log(IStatus status)
    {
        Platform.getLog(FrameworkUtil.getBundle(PortfolioPlugin.class)).log(status);
    }

    public static void log(Throwable t)
    {
        log(new Status(Status.ERROR, PLUGIN_IN, t.getMessage(), t));
    }

}
