package name.abuchen.portfolio.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.FrameworkUtil;

public class FinancePlugin extends AbstractUIPlugin
{
    public static final String PLUGIN_IN = "name.abuchen.portfolio.ui"; //$NON-NLS-1$

    public static final void log(IStatus status)
    {
        Platform.getLog(FrameworkUtil.getBundle(FinancePlugin.class)).log(status);
    }

}
