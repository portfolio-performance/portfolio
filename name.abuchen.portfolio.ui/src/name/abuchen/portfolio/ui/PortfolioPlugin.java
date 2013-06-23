package name.abuchen.portfolio.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class PortfolioPlugin extends AbstractUIPlugin
{
    @SuppressWarnings("nls")
    private final class ManuallyUpdateDaxSampleBecauseOfMissingRootFilesJob extends Job
    {
        private ManuallyUpdateDaxSampleBecauseOfMissingRootFilesJob()
        {
            super("Update dax.xml sample");
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            URL installURL = Platform.getInstallLocation().getURL();
            File f = new File(installURL.getFile(), "dax.xml");
            if (f.exists())
                return Status.OK_STATUS;

            InputStream in = getClass().getResourceAsStream("/dax.xml");
            if (in == null)
                return Status.OK_STATUS;

            try
            {
                FileOutputStream out = new FileOutputStream(f);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1)
                    out.write(buffer, 0, len);

                out.close();
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
            }

            return Status.OK_STATUS;
        }
    }

    public interface Preferences
    {
        String UPDATE_SITE = "UPDATE_SITE"; //$NON-NLS-1$
        String AUTO_UPDATE = "AUTO_UPDATE"; //$NON-NLS-1$
    }

    public static final String PLUGIN_ID = "name.abuchen.portfolio.ui"; //$NON-NLS-1$

    public static final String IMG_LOGO = "pp_128"; //$NON-NLS-1$

    public static final String IMG_SECURITY = "security"; //$NON-NLS-1$
    public static final String IMG_ACCOUNT = "account"; //$NON-NLS-1$
    public static final String IMG_PORTFOLIO = "portfolio"; //$NON-NLS-1$
    public static final String IMG_WATCHLIST = "watchlist"; //$NON-NLS-1$
    public static final String IMG_INVESTMENTPLAN = "investmentplan"; //$NON-NLS-1$

    public static final String IMG_PLUS = "plus"; //$NON-NLS-1$
    public static final String IMG_CONFIG = "config"; //$NON-NLS-1$
    public static final String IMG_EXPORT = "export"; //$NON-NLS-1$
    public static final String IMG_SAVE = "save"; //$NON-NLS-1$

    public static final String IMG_VIEW_TABLE = "view_table"; //$NON-NLS-1$
    public static final String IMG_VIEW_TREEMAP = "view_treemap"; //$NON-NLS-1$
    public static final String IMG_VIEW_PIECHART = "view_piechart"; //$NON-NLS-1$

    public static final String IMG_CHECK = "check"; //$NON-NLS-1$
    public static final String IMG_QUICKFIX = "quickfix"; //$NON-NLS-1$

    private static PortfolioPlugin instance;

    public PortfolioPlugin()
    {
        super();
        instance = this;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);

        if (!"no".equals(System.getProperty("name.abuchen.portfolio.auto-updates"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            Job job = new ManuallyUpdateDaxSampleBecauseOfMissingRootFilesJob();
            job.setSystem(true);
            job.schedule(2000);
        }

    }

    @Override
    protected void initializeImageRegistry(ImageRegistry registry)
    {
        Bundle bundle = Platform.getBundle(PLUGIN_ID);

        for (String key : new String[] { IMG_LOGO, IMG_ACCOUNT, IMG_PORTFOLIO, IMG_SECURITY, IMG_WATCHLIST,
                        IMG_INVESTMENTPLAN, IMG_PLUS, IMG_CONFIG, IMG_EXPORT, IMG_SAVE, IMG_VIEW_TABLE,
                        IMG_VIEW_TREEMAP, IMG_VIEW_PIECHART, IMG_CHECK, IMG_QUICKFIX })
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

    public static void log(List<Exception> errors)
    {
        for (Exception e : errors)
            log(e);
    }

    public static ImageDescriptor descriptor(String key)
    {
        return getDefault().getImageRegistry().getDescriptor(key);
    }

    public static Image image(String key)
    {
        return getDefault().getImageRegistry().get(key);
    }

}
