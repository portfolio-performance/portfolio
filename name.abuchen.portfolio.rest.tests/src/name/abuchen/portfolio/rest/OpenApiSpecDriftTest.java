package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * Guards against drift between the routing table and the hand-authored OpenAPI
 * document: routes, operations, query parameters, and the 400 that the
 * query-parameter check can produce on any of them must match.
 */
@SuppressWarnings("nls")
public class OpenApiSpecDriftTest
{
    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "patch", "delete", "head", "options",
                    "trace");

    /**
     * Captures the {@code code} (second constructor argument) of a
     * {@code FieldError(field, "code", …)} construction. Every emission site
     * passes the code as a same-line string literal, so a line-oriented scan is
     * sufficient; the {@code record FieldError(String field, String code, …)}
     * declaration does not match because its second argument is not a literal.
     */
    private static final Pattern FIELD_ERROR_CODE = Pattern.compile("FieldError\\(\\s*[^,]+,\\s*\"([a-z-]+)\"");

    private static final Pattern STATUS_CODE = Pattern.compile("[1-5][0-9][0-9]");

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

    @Test
    public void testFieldErrorCodesAndSpecAreInLockstep() throws IOException
    {
        var emitted = emittedFieldErrorCodes();
        var documented = documentedFieldErrorCodes();

        var undocumented = new TreeSet<>(emitted);
        undocumented.removeAll(documented);

        var orphaned = new TreeSet<>(documented);
        orphaned.removeAll(emitted);

        assertThat("FieldError codes emitted by handlers but missing from the OpenAPI FieldError.code enum: "
                        + undocumented, undocumented, is(empty()));
        assertThat("codes in the OpenAPI FieldError.code enum that no handler emits: " + orphaned, orphaned,
                        is(empty()));
    }

    @Test
    public void testQueryParametersAndSpecAreInLockstep() throws IOException
    {
        var declared = createRouter().permittedQueryParameters();
        var documented = documentedQueryParameters();

        for (var route : declared.entrySet())
            assertThat("query parameters permitted by " + route.getKey(), new TreeSet<>(route.getValue()),
                            is(new TreeSet<>(documented.getOrDefault(route.getKey(), Set.of()))));
    }

    /** Any request can carry an unknown query parameter, so any operation can answer 400. */
    @Test
    public void testEveryOperationDocumentsTheInvalidRequestResponse() throws IOException
    {
        var responses = documentedResponseCodes();

        var missing = new TreeSet<String>();
        for (var operation : responses.entrySet())
            if (!operation.getValue().contains("400"))
                missing.add(operation.getKey());

        assertThat("operations that can be rejected 400 invalid-request but do not document it: " + missing, missing,
                        is(empty()));
    }

    /** The real routes, as "METHOD /pattern" signatures. */
    private Set<String> actualRoutes()
    {
        return new LinkedHashSet<>(createRouter().routeSignatures());
    }

    private Router createRouter()
    {
        var host = new FakeHost(List.of());
        return ApiRoutes.create(new FileAccessRegistry(node), host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
    }

    /**
     * The {@code in: query} parameters of each documented operation, keyed
     * "METHOD /path". Resolves {@code $ref}s into {@code components/parameters}
     * and folds in the path-level {@code parameters} block, which applies to
     * every operation of that path.
     */
    private Map<String, Set<String>> documentedQueryParameters() throws IOException
    {
        var components = documentedComponentParameters();

        var pathLevel = new HashMap<String, Set<String>>();
        var operations = new LinkedHashMap<String, Set<String>>();

        var inPaths = false;
        String currentPath = null;
        String currentOperation = null;

        // the indentation of the "parameters:" key we are currently inside, or
        // -1; everything deeper belongs to it, anything shallower ends it
        var blockIndent = -1;
        Set<String> target = null;
        String pendingName = null;

        for (var raw : readSpec().split("\n", -1))
        {
            if (raw.isBlank())
                continue;

            var indent = indentOf(raw);
            var content = raw.strip();

            if (blockIndent >= 0 && indent <= blockIndent)
            {
                blockIndent = -1;
                target = null;
                pendingName = null;
            }

            if (blockIndent >= 0)
            {
                if (indent == blockIndent + 2 && content.startsWith("- "))
                {
                    pendingName = null;
                    var item = content.substring(2).strip();

                    if (item.startsWith("$ref:"))
                    {
                        var ref = unquote(item.substring("$ref:".length()).strip());
                        var resolved = components.get(ref.substring(ref.lastIndexOf('/') + 1));
                        if (resolved != null)
                            target.add(resolved);
                    }
                    else if (item.startsWith("name:"))
                    {
                        pendingName = unquote(item.substring("name:".length()).strip());
                    }
                }
                else if (indent == blockIndent + 4 && content.startsWith("in:") && pendingName != null)
                {
                    if ("query".equals(unquote(content.substring("in:".length()).strip())))
                        target.add(pendingName);
                    pendingName = null;
                }
                continue;
            }

            if (indent == 0)
            {
                inPaths = "paths:".equals(content);
                currentPath = null;
                currentOperation = null;
            }
            else if (!inPaths)
            {
                continue;
            }
            else if (indent == 2 && content.startsWith("/") && content.endsWith(":"))
            {
                currentPath = content.substring(0, content.length() - 1);
                currentOperation = null;
                pathLevel.computeIfAbsent(currentPath, key -> new LinkedHashSet<>());
            }
            else if (indent == 4 && currentPath != null && "parameters:".equals(content))
            {
                blockIndent = 4;
                target = pathLevel.get(currentPath);
            }
            else if (indent == 4 && currentPath != null && content.endsWith(":")
                            && HTTP_METHODS.contains(content.substring(0, content.length() - 1)))
            {
                currentOperation = content.substring(0, content.length() - 1).toUpperCase(Locale.ROOT) + " "
                                + currentPath;
                operations.put(currentOperation, new LinkedHashSet<>());
            }
            else if (indent == 6 && currentOperation != null && "parameters:".equals(content))
            {
                blockIndent = 6;
                target = operations.get(currentOperation);
            }
        }

        if (operations.isEmpty())
            throw new IllegalStateException("no operations parsed from the OpenAPI document");

        // a path-level parameter applies to every operation of that path
        for (var operation : operations.entrySet())
            operation.getValue().addAll(pathLevel.getOrDefault(operation.getKey().split(" ", 2)[1], Set.of()));

        return operations;
    }

    /** The response status codes documented for each operation, keyed "METHOD /path". */
    private Map<String, Set<String>> documentedResponseCodes() throws IOException
    {
        var result = new LinkedHashMap<String, Set<String>>();

        var inPaths = false;
        String currentPath = null;
        Set<String> target = null;
        var inResponses = false;

        for (var raw : readSpec().split("\n", -1))
        {
            if (raw.isBlank())
                continue;

            var indent = indentOf(raw);
            var content = raw.strip();

            if (inResponses && indent <= 6)
                inResponses = false;

            if (indent == 0)
            {
                inPaths = "paths:".equals(content);
                currentPath = null;
                target = null;
            }
            else if (!inPaths)
            {
                continue;
            }
            else if (indent == 2 && content.startsWith("/") && content.endsWith(":"))
            {
                currentPath = content.substring(0, content.length() - 1);
                target = null;
            }
            else if (indent == 4 && currentPath != null && content.endsWith(":")
                            && HTTP_METHODS.contains(content.substring(0, content.length() - 1)))
            {
                target = new LinkedHashSet<>();
                result.put(content.substring(0, content.length() - 1).toUpperCase(Locale.ROOT) + " " + currentPath,
                                target);
            }
            else if (indent == 6 && target != null && "responses:".equals(content))
            {
                inResponses = true;
            }
            else if (indent == 8 && inResponses && content.endsWith(":"))
            {
                // a status code, not some other key of the responses block
                var code = unquote(content.substring(0, content.length() - 1));
                if (STATUS_CODE.matcher(code).matches())
                    target.add(code);
            }
        }

        if (result.isEmpty())
            throw new IllegalStateException("no operations parsed from the OpenAPI document");

        return result;
    }

    /**
     * The reusable parameters under {@code components/parameters}, as component
     * name to parameter name - only those with {@code in: query}, the rest
     * cannot be a query parameter however they are referenced.
     */
    private Map<String, String> documentedComponentParameters() throws IOException
    {
        var result = new HashMap<String, String>();

        var inComponents = false;
        var inParameters = false;
        String component = null;
        String name = null;

        for (var raw : readSpec().split("\n", -1))
        {
            if (raw.isBlank())
                continue;

            var indent = indentOf(raw);
            var content = raw.strip();

            if (indent == 0)
            {
                inComponents = "components:".equals(content);
                inParameters = false;
            }
            else if (inComponents && indent == 2 && content.endsWith(":"))
            {
                inParameters = "parameters:".equals(content);
                component = null;
            }
            else if (inParameters && indent == 4 && content.endsWith(":"))
            {
                component = content.substring(0, content.length() - 1);
                name = null;
            }
            else if (inParameters && component != null && indent == 6)
            {
                if (content.startsWith("name:"))
                    name = unquote(content.substring("name:".length()).strip());
                else if (content.startsWith("in:") && name != null
                                && "query".equals(unquote(content.substring("in:".length()).strip())))
                    result.put(component, name);
            }
        }

        if (result.isEmpty())
            throw new IllegalStateException("no reusable query parameters parsed from the OpenAPI document");

        return result;
    }

    private static String unquote(String value)
    {
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'")))
            return value.substring(1, value.length() - 1);
        return value;
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
                    operations.add(key.toUpperCase(Locale.ROOT) + " " + currentPath);
            }
        }

        if (operations.isEmpty())
            throw new IllegalStateException("no operations parsed from the OpenAPI document");

        return operations;
    }

    /** Every FieldError code the production handlers actually emit. */
    private Set<String> emittedFieldErrorCodes() throws IOException
    {
        var srcRoot = locateSourceRoot();

        List<Path> javaFiles;
        try (var stream = Files.walk(srcRoot))
        {
            javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
        }

        var codes = new TreeSet<String>();
        for (var path : javaFiles)
        {
            var matcher = FIELD_ERROR_CODE.matcher(Files.readString(path));
            while (matcher.find())
                codes.add(matcher.group(1));
        }

        if (codes.isEmpty())
            throw new IllegalStateException("no FieldError codes found under " + srcRoot);

        return codes;
    }

    /** The FieldError.code enum values documented in the OpenAPI file. */
    private Set<String> documentedFieldErrorCodes() throws IOException
    {
        var codes = new TreeSet<String>();

        var inFieldError = false;
        var inEnum = false;

        for (var raw : readSpec().split("\n", -1))
        {
            if (raw.isBlank())
                continue;

            var indent = indentOf(raw);
            var content = raw.strip();

            // schema names live at indent 4 under components/schemas
            if (indent == 4 && content.endsWith(":"))
            {
                inFieldError = "FieldError:".equals(content);
                inEnum = false;
            }
            else if (inFieldError && content.equals("enum:"))
                inEnum = true;
            else if (inFieldError && inEnum && content.startsWith("- "))
                codes.add(content.substring(2).strip());
            else if (inFieldError && indent <= 8 && !content.startsWith("- "))
                inEnum = false; // left the code property's enum block
        }

        if (codes.isEmpty())
            throw new IllegalStateException("no FieldError.code enum parsed from the OpenAPI document");

        return codes;
    }

    /** Locates {@code name.abuchen.portfolio.rest/src} by walking up from the working directory. */
    private Path locateSourceRoot() throws IOException
    {
        var dir = Path.of("").toAbsolutePath();
        for (var d = dir; d != null; d = d.getParent())
        {
            var candidate = d.resolve("name.abuchen.portfolio.rest").resolve("src");
            if (Files.isDirectory(candidate))
                return candidate;
        }

        throw new IOException("rest plugin source not found from working directory " + dir);
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
