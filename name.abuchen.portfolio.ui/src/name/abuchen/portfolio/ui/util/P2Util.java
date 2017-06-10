package name.abuchen.portfolio.ui.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.ui.PortfolioPlugin;

public class P2Util
{
    private P2Util()
    {}

    public static Collection<URI> toURIs(IEclipsePreferences preferences)
    {
        Collection<URI> uris = new ArrayList<>();
        String[] keys;
        try
        {
            keys = preferences.keys();
        }
        catch (BackingStoreException e)
        {
            PortfolioPlugin.log(e);
            return uris;
        }
        for (String updateSite : keys)
        {
            try
            {
                URI uri = new URI(updateSite);
                if (uri.isAbsolute())
                {
                    uris.add(uri);
                }
            }
            catch (Exception e)
            {
                PortfolioPlugin.log(e);
            }
        }

        return uris;
    }
}
