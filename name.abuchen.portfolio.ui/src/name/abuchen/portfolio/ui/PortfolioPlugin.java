package name.abuchen.portfolio.ui;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.ui.preferences.ScopedPreferenceStore;

public class PortfolioPlugin implements BundleActivator
{
    public interface Preferences
    {
        String UPDATE_SITE = "UPDATE_SITE"; //$NON-NLS-1$
        String AUTO_UPDATE = "AUTO_UPDATE"; //$NON-NLS-1$
    }

    public static final String PLUGIN_ID = "name.abuchen.portfolio.ui"; //$NON-NLS-1$

    public static final String IMG_LOGO_16 = "pp_16.png"; //$NON-NLS-1$
    public static final String IMG_LOGO_32 = "pp_32.png"; //$NON-NLS-1$
    public static final String IMG_LOGO_48 = "pp_48.png"; //$NON-NLS-1$
    public static final String IMG_LOGO_128 = "pp_128.png"; //$NON-NLS-1$
    public static final String IMG_LOGO_256 = "pp_256.png"; //$NON-NLS-1$
    public static final String IMG_LOGO_512 = "pp_512.png"; //$NON-NLS-1$

    public static final String IMG_LOGO = IMG_LOGO_128;
    public static final String IMG_LOGO_SMALL = IMG_LOGO_48;

    public static final String IMG_SECURITY = "security.png"; //$NON-NLS-1$
    public static final String IMG_ACCOUNT = "account.png"; //$NON-NLS-1$
    public static final String IMG_PORTFOLIO = "portfolio.png"; //$NON-NLS-1$
    public static final String IMG_WATCHLIST = "watchlist.png"; //$NON-NLS-1$
    public static final String IMG_INVESTMENTPLAN = "investmentplan.png"; //$NON-NLS-1$
    public static final String IMG_NOTE = "note.png"; //$NON-NLS-1$

    public static final String IMG_PLUS = "plus.png"; //$NON-NLS-1$
    public static final String IMG_CONFIG = "config.png"; //$NON-NLS-1$
    public static final String IMG_EXPORT = "export.png"; //$NON-NLS-1$
    public static final String IMG_SAVE = "save.png"; //$NON-NLS-1$
    public static final String IMG_FILTER = "filter.png"; //$NON-NLS-1$

    public static final String IMG_VIEW_TABLE = "view_table.png"; //$NON-NLS-1$
    public static final String IMG_VIEW_TREEMAP = "view_treemap.png"; //$NON-NLS-1$
    public static final String IMG_VIEW_PIECHART = "view_piechart.png"; //$NON-NLS-1$
    public static final String IMG_VIEW_REBALANCING = "view_rebalancing.png"; //$NON-NLS-1$
    public static final String IMG_VIEW_STACKEDCHART = "view_stackedchart.png"; //$NON-NLS-1$

    public static final String IMG_CHECK = "check.png"; //$NON-NLS-1$
    public static final String IMG_QUICKFIX = "quickfix.png"; //$NON-NLS-1$
    public static final String IMG_ADD = "add.png"; //$NON-NLS-1$
    public static final String IMG_REMOVE = "remove.png"; //$NON-NLS-1$

    public static final String IMG_CATEGORY = "category.png"; //$NON-NLS-1$
    public static final String IMG_UNASSIGNED_CATEGORY = "unassigned.png"; //$NON-NLS-1$

    public static final String IMG_TEXT = "text.png"; //$NON-NLS-1$

    public static final String IMG_ERROR = "error.png"; //$NON-NLS-1$
    public static final String IMG_WARNING = "warning.png"; //$NON-NLS-1$
    public static final String IMG_INFO = "info.png"; //$NON-NLS-1$

    private static PortfolioPlugin instance;

    private Bundle bundle;
    private ImageRegistry imageRegistry;
    private IPreferenceStore preferenceStore;

    public PortfolioPlugin()
    {
        super();
        instance = this;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        bundle = context.getBundle();

        setupProxyAuthenticator();

        imageRegistry = new ImageRegistry();
        initializeImageRegistry(imageRegistry);

        preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PortfolioPlugin.PLUGIN_ID);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        if (preferenceStore != null && preferenceStore.needsSaving())
            ((ScopedPreferenceStore) preferenceStore).save();
    }

    private void setupProxyAuthenticator()
    {
        // http://stackoverflow.com/questions/1626549/authenticated-http-proxy-with-java/16340273#16340273
        // Java ignores http.proxyUser. Here come's the workaround.
        Authenticator.setDefault(new Authenticator()
        {
            @SuppressWarnings("nls")
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                if (getRequestorType() == RequestorType.PROXY)
                {
                    String protocol = getRequestingProtocol().toLowerCase();
                    String host = System.getProperty(protocol + ".proxyHost", "");
                    String port = System.getProperty(protocol + ".proxyPort", "80");
                    String user = System.getProperty(protocol + ".proxyUser", "");
                    String password = System.getProperty(protocol + ".proxyPassword", "");

                    if (getRequestingHost().equalsIgnoreCase(host) && Integer.parseInt(port) == getRequestingPort())
                        return new PasswordAuthentication(user, password.toCharArray());
                }
                return null;
            }
        });
    }

    private void initializeImageRegistry(ImageRegistry registry)
    {
        // Enable use of HiDPI icons as described here:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=459412#c8
        // But: for now it needs enabling via JFace debug option, which in turn
        // are only set when using the non-e4 org.eclipse.ui bundle. Alas, we
        // enable it directly.
        // On Mac OS X it works, on Linux the wrong images got loaded, on
        // Windows I could not tell a difference. Therefore I activated it only
        // for Mac OS X.
        if (Platform.OS_MACOSX.equals(Platform.getOS()))
            org.eclipse.jface.internal.InternalPolicy.DEBUG_LOAD_URL_IMAGE_DESCRIPTOR_2x = true;

        Bundle bundle = Platform.getBundle(PLUGIN_ID);

        for (String key : new String[] { IMG_LOGO_16, IMG_LOGO_32, IMG_LOGO_48, IMG_LOGO_128, IMG_LOGO_256,
                        IMG_LOGO_512, IMG_ACCOUNT, IMG_PORTFOLIO, IMG_SECURITY, IMG_WATCHLIST, IMG_INVESTMENTPLAN,
                        IMG_NOTE, IMG_PLUS, IMG_CONFIG, IMG_EXPORT, IMG_SAVE, IMG_FILTER, IMG_VIEW_TABLE,
                        IMG_VIEW_TREEMAP, IMG_VIEW_PIECHART, IMG_VIEW_REBALANCING, IMG_VIEW_STACKEDCHART, IMG_CHECK,
                        IMG_QUICKFIX, IMG_ADD, IMG_REMOVE, IMG_CATEGORY, IMG_UNASSIGNED_CATEGORY, IMG_TEXT, IMG_ERROR,
                        IMG_WARNING, IMG_INFO })
        {
            IPath path = new Path("icons/" + key); //$NON-NLS-1$
            URL url = FileLocator.find(bundle, path, null);
            ImageDescriptor desc = ImageDescriptor.createFromURL(url);
            registry.put(key, desc);
        }
    }

    public Bundle getBundle()
    {
        return bundle;
    }

    public ImageRegistry getImageRegistry()
    {
        return imageRegistry;
    }

    public IPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public IPath getStateLocation()
    {
        return Platform.getStateLocation(bundle);
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

    public static void log(String message)
    {
        log(new Status(Status.ERROR, PLUGIN_ID, message));
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

    public static boolean isDevelopmentMode()
    {
        return System.getProperty("osgi.dev") != null; //$NON-NLS-1$
    }
}
