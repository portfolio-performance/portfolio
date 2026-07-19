package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * Guards against drift between the routing table ({@link ApiRoutes} /
 * {@code Router}) and the hand-authored OpenAPI document: every route must be
 * documented, and every documented operation must be a real route.
 * <p/>
 * The document is parsed with a deliberately small parser (paths and their HTTP
 * methods only) so the test does not need a YAML library on the target platform.
 */
@SuppressWarnings("nls")
public class OpenApiSpecDriftTest
{
    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "patch", "delete", "head", "options",
                    "trace");

    private IEclipsePreferences node;

    @Before
    public void setUp()
    {
        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    @Test
    public void testSpecAndRoutesAreInLockstep() throws IOException
    {
        var routes = actualRoutes();
        var documented = documentedOperations();

        var undocumented = new TreeSet<>(routes);
        undocumented.removeAll(documented);

        var orphaned = new TreeSet<>(documented);
        orphaned.removeAll(routes);

        assertThat("routes without a matching OpenAPI operation: " + undocumented, undocumented, is(empty()));
        assertThat("OpenAPI operations without a matching route: " + orphaned, orphaned, is(empty()));
    }

    /** The real routes, as "METHOD /pattern" signatures. */
    private Set<String> actualRoutes()
    {
        var router = ApiRoutes.create(new FileAccessRegistry(node), new FakeHost(List.of()));
        return new LinkedHashSet<>(router.routeSignatures());
    }

    /** The operations documented in the OpenAPI file, as "METHOD /pattern". */
    private Set<String> documentedOperations() throws IOException
    {
        var operations = new LinkedHashSet<String>();

        var inPaths = false;
        String currentPath = null;

        for (var raw : readSpec().split("\n", -1))
        {
            if (raw.isBlank() || raw.strip().startsWith("#"))
                continue;

            var indent = indentOf(raw);
            var content = raw.strip();

            if (indent == 0)
            {
                inPaths = "paths:".equals(content);
                currentPath = null;
            }
            else if (inPaths && indent == 2 && content.startsWith("/") && content.endsWith(":"))
            {
                currentPath = content.substring(0, content.length() - 1);
            }
            else if (inPaths && indent == 4 && currentPath != null && content.endsWith(":"))
            {
                var key = content.substring(0, content.length() - 1);
                if (HTTP_METHODS.contains(key))
                    operations.add(key.toUpperCase() + " " + currentPath);
            }
        }

        if (operations.isEmpty())
            throw new IllegalStateException("no operations parsed from the OpenAPI document");

        return operations;
    }

    private static int indentOf(String line)
    {
        var i = 0;
        while (i < line.length() && line.charAt(i) == ' ')
            i++;
        return i;
    }

    /**
     * Reads the specification. Prefers it as a classpath resource of the host
     * bundle (this test is a fragment of it); falls back to walking up from the
     * working directory, so it also runs from an IDE or the reactor root.
     */
    private String readSpec() throws IOException
    {
        try (InputStream in = getClass().getResourceAsStream("/openapi.yaml"))
        {
            if (in != null)
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        var dir = Path.of("").toAbsolutePath();
        for (var d = dir; d != null; d = d.getParent())
        {
            var candidate = d.resolve("name.abuchen.portfolio.rest").resolve("openapi.yaml");
            if (Files.exists(candidate))
                return Files.readString(candidate);
        }

        throw new IOException("openapi.yaml not found from working directory " + dir);
    }
}
