package name.abuchen.portfolio.ui.addons;

import jakarta.inject.Inject;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.extensions.Preference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class ProxyAddon
{

    @Inject
    public void setupProxyService(@Preference(value = UIConstants.Preferences.PROXY_HOST) String proxyHost,
                    @Preference(value = UIConstants.Preferences.PROXY_PORT) int proxyPort)
    {
        BundleContext bc = FrameworkUtil.getBundle(ProxyAddon.class).getBundleContext();
        ServiceReference<IProxyService> serviceReference = bc.getServiceReference(IProxyService.class);
        IProxyService proxyService = bc.getService(serviceReference);

        setupProxy(proxyService, proxyHost, proxyPort);

        bc.ungetService(serviceReference);
    }

    private void setupProxy(IProxyService proxyService, String proxyHost, int proxyPort)
    {
        // proxy user and password are not stored in the preferences because we
        // do not want to get into the game of securely storing passwords.
        // As a workaround, offer special system properties as the default ones
        // are overwritten by the proxy service.

        try
        {
            IProxyData[] proxyData = proxyService.getProxyData();
            for (IProxyData data : proxyData)
            {
                if (IProxyData.HTTP_PROXY_TYPE.equals(data.getType())
                                || IProxyData.HTTPS_PROXY_TYPE.equals(data.getType()))
                {
                    data.setHost(proxyHost);
                    data.setPort(proxyHost == null ? -1 : proxyPort);
                    data.setUserid(proxyHost == null ? null : System.getProperty("name.abuchen.portfolio.proxyUser")); //$NON-NLS-1$
                    data.setPassword(proxyHost == null ? null
                                    : System.getProperty("name.abuchen.portfolio.proxyPassword")); //$NON-NLS-1$
                }
            }

            proxyService.setProxyData(proxyData);
            proxyService.setSystemProxiesEnabled(false);
            proxyService.setProxiesEnabled(true);
        }
        catch (CoreException e)
        {
            PortfolioPlugin.log(e);
        }
    }
}
