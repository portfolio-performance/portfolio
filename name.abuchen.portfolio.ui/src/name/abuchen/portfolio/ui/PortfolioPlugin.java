package name.abuchen.portfolio.ui;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.ui.preferences.ScopedPreferenceStore;

public class PortfolioPlugin implements BundleActivator
{
    public static final String PLUGIN_ID = "name.abuchen.portfolio.ui"; //$NON-NLS-1$

    private static PortfolioPlugin instance;

    private Bundle bundle;
    private IPreferenceStore preferenceStore;
    private IDialogSettings dialogSettings;

    public PortfolioPlugin()
    {
        super();
        instance = this; // NOSONAR bundle is singleton
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        bundle = context.getBundle();

        setupProxyAuthenticator();

        preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PortfolioPlugin.PLUGIN_ID);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        if (preferenceStore != null && preferenceStore.needsSaving())
            ((ScopedPreferenceStore) preferenceStore).save();

        saveDialogSettings();

        Job.getJobManager().cancel(null);
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

    public Bundle getBundle()
    {
        return bundle;
    }

    public IPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public IPath getStateLocation()
    {
        return Platform.getStateLocation(bundle);
    }

    public IDialogSettings getDialogSettings()
    {
        if (dialogSettings == null)
            loadDialogSettings();

        return dialogSettings;
    }

    private File getSettingsFile()
    {
        return new File(getStateLocation().toFile(), "dialog_settings.xml"); //$NON-NLS-1$
    }

    private void loadDialogSettings()
    {
        try
        {
            dialogSettings = new DialogSettings("PP"); //$NON-NLS-1$

            File file = getSettingsFile();
            if (file.exists())
                dialogSettings.load(file.getAbsolutePath());
        }
        catch (IOException e)
        {
            log(e);
        }
    }

    private void saveDialogSettings()
    {
        if (dialogSettings == null)
            return;

        try
        {
            dialogSettings.save(getSettingsFile().getAbsolutePath());
        }
        catch (IOException e)
        {
            log(e);
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

    public static void log(String message, Throwable t)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
    }

    public static void log(Throwable t)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

    public static void log(String message)
    {
        log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    public static void info(String message)
    {
        log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

    public static void log(List<Exception> errors)
    {
        for (Exception e : errors)
            log(e);
    }

    public static boolean isDevelopmentMode()
    {
        return System.getProperty("osgi.dev") != null; //$NON-NLS-1$
    }
}
