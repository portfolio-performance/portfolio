package name.abuchen.portfolio.ui;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;

import name.abuchen.portfolio.ui.preferences.ScopedPreferenceStore;

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

public class PortfolioPlugin implements BundleActivator
{
    public interface Preferences
    {
        String UPDATE_SITE = "UPDATE_SITE"; //$NON-NLS-1$
        String AUTO_UPDATE = "AUTO_UPDATE"; //$NON-NLS-1$
    }

    public static final String PLUGIN_ID = "name.abuchen.portfolio.ui"; //$NON-NLS-1$

    public static final String IMG_LOGO_16 = "pp_16"; //$NON-NLS-1$
    public static final String IMG_LOGO_32 = "pp_32"; //$NON-NLS-1$
    public static final String IMG_LOGO_48 = "pp_48"; //$NON-NLS-1$
    public static final String IMG_LOGO_128 = "pp_128"; //$NON-NLS-1$
    public static final String IMG_LOGO_256 = "pp_256"; //$NON-NLS-1$
    public static final String IMG_LOGO_512 = "pp_512"; //$NON-NLS-1$

    public static final String IMG_LOGO = IMG_LOGO_128;
    public static final String IMG_LOGO_SMALL = IMG_LOGO_48;

    public static final String IMG_SECURITY = "security"; //$NON-NLS-1$
    public static final String IMG_ACCOUNT = "account"; //$NON-NLS-1$
    public static final String IMG_PORTFOLIO = "portfolio"; //$NON-NLS-1$
    public static final String IMG_WATCHLIST = "watchlist"; //$NON-NLS-1$
    public static final String IMG_INVESTMENTPLAN = "investmentplan"; //$NON-NLS-1$
    public static final String IMG_NOTE = "note"; //$NON-NLS-1$

    public static final String IMG_PLUS = "plus"; //$NON-NLS-1$
    public static final String IMG_CONFIG = "config"; //$NON-NLS-1$
    public static final String IMG_EXPORT = "export"; //$NON-NLS-1$
    public static final String IMG_SAVE = "save"; //$NON-NLS-1$
    public static final String IMG_FILTER = "filter"; //$NON-NLS-1$

    public static final String IMG_VIEW_TABLE = "view_table"; //$NON-NLS-1$
    public static final String IMG_VIEW_TREEMAP = "view_treemap"; //$NON-NLS-1$
    public static final String IMG_VIEW_PIECHART = "view_piechart"; //$NON-NLS-1$
    public static final String IMG_VIEW_REBALANCING = "view_rebalancing"; //$NON-NLS-1$
    public static final String IMG_VIEW_STACKEDCHART = "view_stackedchart"; //$NON-NLS-1$

    public static final String IMG_CHECK = "check"; //$NON-NLS-1$
    public static final String IMG_QUICKFIX = "quickfix"; //$NON-NLS-1$
    public static final String IMG_ADD = "add"; //$NON-NLS-1$
    public static final String IMG_REMOVE = "remove"; //$NON-NLS-1$

    public static final String IMG_CATEGORY = "category"; //$NON-NLS-1$
    public static final String IMG_UNASSIGNED_CATEGORY = "unassigned"; //$NON-NLS-1$

    public static final String IMG_TEXT = "text"; //$NON-NLS-1$

    public static final String IMG_ERROR = "error"; //$NON-NLS-1$
    public static final String IMG_WARNING = "warning"; //$NON-NLS-1$
    public static final String IMG_INFO = "info"; //$NON-NLS-1$

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
        Bundle bundle = Platform.getBundle(PLUGIN_ID);

        for (String key : new String[] { IMG_LOGO_16, IMG_LOGO_32, IMG_LOGO_48, IMG_LOGO_128, IMG_LOGO_256,
                        IMG_LOGO_512, IMG_ACCOUNT, IMG_PORTFOLIO, IMG_SECURITY, IMG_WATCHLIST, IMG_INVESTMENTPLAN,
                        IMG_NOTE, IMG_PLUS, IMG_CONFIG, IMG_EXPORT, IMG_SAVE, IMG_FILTER, IMG_VIEW_TABLE,
                        IMG_VIEW_TREEMAP, IMG_VIEW_PIECHART, IMG_VIEW_REBALANCING, IMG_VIEW_STACKEDCHART, IMG_CHECK,
                        IMG_QUICKFIX, IMG_ADD, IMG_REMOVE, IMG_CATEGORY, IMG_UNASSIGNED_CATEGORY, IMG_TEXT, IMG_ERROR,
                        IMG_WARNING, IMG_INFO })
        {
            IPath path = new Path("icons/" + key + ".gif"); //$NON-NLS-1$ //$NON-NLS-2$
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
}
