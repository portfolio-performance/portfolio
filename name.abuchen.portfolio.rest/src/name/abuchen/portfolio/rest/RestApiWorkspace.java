package name.abuchen.portfolio.rest;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.FrameworkUtil;

/**
 * Locates the workspace-scoped state of the API: the preferences holding the
 * global settings and the per-file access records, and the state location
 * holding the bearer token. Nothing of this lives inside the portfolio file.
 */
public final class RestApiWorkspace
{
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

    public static TokenStore createTokenStore()
    {
        var bundle = FrameworkUtil.getBundle(RestApiConstants.class);
        return new TokenStore(Platform.getStateLocation(bundle).toFile().toPath());
    }
}
