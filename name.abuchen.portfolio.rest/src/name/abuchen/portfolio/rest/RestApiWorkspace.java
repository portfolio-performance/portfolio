package name.abuchen.portfolio.rest;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.FrameworkUtil;

/**
 * Locates the workspace-scoped state of the API: the preferences holding the
 * global settings and the per-file access records, and the state location
 * holding the client tokens. Nothing of this lives inside the portfolio file.
 */
public final class RestApiWorkspace
{
    private static ClientStore clientStore;

    private RestApiWorkspace()
    {
    }

    public static IEclipsePreferences preferences()
    {
        return InstanceScope.INSTANCE.getNode(RestApiConstants.PLUGIN_ID);
    }

    public static FileAccessRegistry createFileAccessRegistry()
    {
        return new FileAccessRegistry((IEclipsePreferences) preferences().node(RestApiConstants.PREF_NODE_FILES));
    }

    /**
     * The one client store of the application. A singleton because session
     * tokens live only in this instance: they must survive a server restart
     * (e.g. a port change) and be visible to the preference page.
     */
    public static synchronized ClientStore getClientStore()
    {
        if (clientStore == null)
        {
            var bundle = FrameworkUtil.getBundle(RestApiConstants.class);
            clientStore = new ClientStore(Platform.getStateLocation(bundle).toFile().toPath());
        }
        return clientStore;
    }
}
