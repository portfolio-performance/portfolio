package name.abuchen.portfolio.rest.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;

import org.osgi.framework.FrameworkUtil;

/**
 * Serves the API's own OpenAPI description, so that a client can discover the
 * full contract at runtime - drift-free by construction, because the document
 * returned is the one shipped with the running server. The endpoint is exempt
 * from bearer auth (see {@code RestApiServer}): the document carries no user
 * data and describes the pairing flow itself.
 * <p/>
 * The document lives at the bundle root ({@code openapi.yaml}), not on the Java
 * classpath, so it is read as a bundle entry - which resolves both from the
 * exploded project directory during development and from the packaged bundle.
 */
public final class OpenApiHandler
{
    private static final String RESOURCE = "/openapi.yaml"; //$NON-NLS-1$
    private static final String CONTENT_TYPE = "application/yaml"; //$NON-NLS-1$

    private static volatile byte[] cached;

    private OpenApiHandler()
    {
    }

    public static Response serve()
    {
        // clone so each Response owns its buffer, like the other Response
        // factories; the cache exists only to avoid re-reading the bundle
        // entry, not to share a mutable array across responses
        return Response.of(200, CONTENT_TYPE, document().clone());
    }

    private static byte[] document()
    {
        var bytes = cached;
        if (bytes != null)
            return bytes;

        try (InputStream in = documentUrl().openStream())
        {
            bytes = in.readAllBytes();
            cached = bytes;
            return bytes;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot read " + RESOURCE, e); //$NON-NLS-1$
        }
    }

    private static URL documentUrl()
    {
        var bundle = FrameworkUtil.getBundle(OpenApiHandler.class);
        var url = bundle != null ? bundle.getEntry(RESOURCE) : OpenApiHandler.class.getResource(RESOURCE);
        if (url == null)
            throw new IllegalStateException(RESOURCE + " is not on the bundle - check build.properties bin.includes"); //$NON-NLS-1$
        return url;
    }
}
