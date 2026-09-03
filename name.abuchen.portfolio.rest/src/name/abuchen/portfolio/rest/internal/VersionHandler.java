package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonObject;

import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.rest.RestApiConstants;

/**
 * Answers "which contract am I talking to?" without making the client parse the
 * 97 KB OpenAPI document for one line of it. Exempt from bearer auth like that
 * document: it carries no user data, and a client needs it before deciding
 * whether its own assumptions still hold.
 * <p/>
 * Two versions, because they answer different questions. {@code apiVersion} is
 * what a client checks its own expectations against; {@code applicationVersion}
 * is what it tells a <em>human</em> to update, and no user thinks in API
 * versions.
 */
public final class VersionHandler
{
    private VersionHandler()
    {
    }

    public static Response serve()
    {
        var json = new JsonObject();
        json.addProperty("apiVersion", RestApiConstants.API_VERSION); //$NON-NLS-1$

        var application = applicationVersion();
        if (application != null)
            json.addProperty("applicationVersion", application); //$NON-NLS-1$

        return Response.json(200, json);
    }

    /**
     * The running application's release, or null outside OSGi - the headless
     * dev server has no bundle, and an absent key is honester than a fake
     * version a client might compare against.
     */
    private static String applicationVersion()
    {
        var bundle = FrameworkUtil.getBundle(VersionHandler.class);
        if (bundle == null)
            return null;

        var version = bundle.getVersion();
        return version.getMajor() + "." + version.getMinor() + "." + version.getMicro(); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
