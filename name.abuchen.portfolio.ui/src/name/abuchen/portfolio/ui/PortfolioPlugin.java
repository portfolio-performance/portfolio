package name.abuchen.portfolio.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class PortfolioPlugin extends AbstractUIPlugin
{
    public static final String PLUGIN_ID = "name.abuchen.portfolio.ui"; //$NON-NLS-1$

    public static final String IMG_SECURITY = "security"; //$NON-NLS-1$
    public static final String IMG_ACCOUNT = "account"; //$NON-NLS-1$
    public static final String IMG_PORTFOLIO = "portfolio"; //$NON-NLS-1$

    private static PortfolioPlugin instance;

    public PortfolioPlugin()
    {
        super();
        instance = this;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry registry)
    {
        Bundle bundle = Platform.getBundle(PLUGIN_ID);

        for (String key : new String[] { IMG_ACCOUNT, IMG_PORTFOLIO, IMG_SECURITY })
        {
            IPath path = new Path("icons/" + key + ".gif"); //$NON-NLS-1$ //$NON-NLS-2$
            URL url = FileLocator.find(bundle, path, null);
            ImageDescriptor desc = ImageDescriptor.createFromURL(url);
            registry.put(key, desc);
        }
    }

    public static PortfolioPlugin getDefault()
    {
        return instance;
    }

    public static final void log(IStatus status)
    {
        Platform.getLog(FrameworkUtil.getBundle(PortfolioPlugin.class)).log(status);
    }

    public static void log(Throwable t)
    {
        log(new Status(Status.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

}
