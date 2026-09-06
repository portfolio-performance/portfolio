package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
import name.abuchen.portfolio.rest.internal.mcp.McpEnvelope;
import name.abuchen.portfolio.rest.internal.mcp.McpTool;
import name.abuchen.portfolio.rest.internal.mcp.McpTools;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * Guards against drift between the routing table and the hand-authored
 * {@code mcp-tools.json}, as {@link OpenApiSpecDriftTest} does for
 * {@code openapi.yaml}: everything a reader of that document could get wrong
 * silently, plus the one property that is a decision rather than mechanics -
 * which operations are deliberately absent.
 */
@SuppressWarnings("nls")
public class McpToolsDriftTest
{
    /**
     * The routes no tool serves: the endpoint itself, the version endpoint a
     * client reads before it speaks, and the three deliberate absences - a
     * model must not pull a 97,000-character specification into its context,
     * nor drive its own authorisation.
     */
    private static final Set<String> ROUTES_WITHOUT_A_TOOL = Set.of( //
                    "POST /mcp", //
                    "GET /v1/openapi.yaml", //
                    "GET /v1/version", //
                    "POST /v1/auth/requests", //
                    "GET /v1/auth/requests/{id}");

    private static final Pattern TOOL_NAME = Pattern.compile("pp_[a-z0-9_]+");

    private static final Pattern PATH_PLACEHOLDER = Pattern.compile("\\{([^}]+)\\}");

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

    /** also asserts the document is reachable as a bundle entry, not just in the IDE */
    @Test
    public void testTheSixteenToolsAreServed()
    {
        assertThat(McpTools.all(), hasSize(16));
        assertThat(McpTools.listPayload().size(), is(16));
    }

    @Test
    public void testEveryToolPointsAtARealRoute()
    {
        var routes = new LinkedHashSet<>(createRouter().routeSignatures());

        var dangling = new TreeSet<String>();
        for (McpTool tool : McpTools.all())
        {
            var signature = tool.method() + " " + tool.pathTemplate();
            if (!routes.contains(signature))
                dangling.add(tool.name() + " -> " + signature);
        }

        assertThat("tools whose route does not exist: " + dangling, dangling, is(empty()));
    }

    /** the direction that catches a route added without a tool */
    @Test
    public void testEveryRouteHasATool()
    {
        var served = new TreeSet<String>();
        for (McpTool tool : McpTools.all())
            served.add(tool.method() + " " + tool.pathTemplate());

        var uncovered = new TreeSet<>(createRouter().routeSignatures());
        uncovered.removeAll(served);
        uncovered.removeAll(ROUTES_WITHOUT_A_TOOL);

        assertThat("routes no MCP tool serves: " + uncovered, uncovered, is(empty()));
    }

    @Test
    public void testQueryBindingsAreParametersTheRouteAccepts()
    {
        var permitted = createRouter().permittedQueryParameters();

        var rejected = new TreeSet<String>();
        for (McpTool tool : McpTools.all())
        {
            var accepted = permitted.get(tool.method() + " " + tool.pathTemplate());
            for (McpTool.Binding binding : tool.bindings())
            {
                if (binding.where() == McpTool.Where.QUERY && !accepted.contains(binding.wire()))
                    rejected.add(tool.name() + " -> ?" + binding.wire());
            }
        }

        assertThat("query bindings the route would reject with a 400: " + rejected, rejected, is(empty()));
    }

    @Test
    public void testPathBindingsCoverEveryPlaceholder()
    {
        for (McpTool tool : McpTools.all())
        {
            var placeholders = new LinkedHashSet<String>();
            var matcher = PATH_PLACEHOLDER.matcher(tool.pathTemplate());
            while (matcher.find())
                placeholders.add(matcher.group(1));

            var bound = new LinkedHashSet<String>();
            for (McpTool.Binding binding : tool.bindings())
            {
                if (binding.where() == McpTool.Where.PATH)
                    bound.add(binding.wire());
            }

            assertThat(tool.name() + " path bindings", bound, is(placeholders));
        }
    }

    /**
     * The routing table only ever insists on a path segment or a query
     * parameter, so a required body property would be a promise no handler
     * enforces.
     */
    @Test
    public void testEveryRequiredArgumentIsBoundToTheAddress()
    {
        for (McpTool tool : McpTools.all())
        {
            for (String required : tool.requiredArguments())
            {
                var binding = tool.binding(required);
                assertThat(tool.name() + " binds required argument " + required, binding, is(notNullValue()));
                assertThat(tool.name() + "." + required, binding.where(), is(not(McpTool.Where.BODY)));
            }
        }
    }

    /** no default file and no active file, on all but the first call */
    @Test
    public void testFileIsRequiredOnFifteenOfSixteen()
    {
        var without = new TreeSet<String>();
        for (McpTool tool : McpTools.all())
        {
            if (!tool.requiredArguments().contains("file"))
                without.add(tool.name());
        }

        assertThat(without, is(new TreeSet<>(Set.of("pp_list_files"))));
    }

    @Test
    public void testEveryDeclaredArgumentIsBoundAndEveryBindingDeclared()
    {
        for (McpTool tool : McpTools.all())
        {
            var declared = new TreeSet<>(tool.declaredArguments());

            var bound = new TreeSet<String>();
            for (McpTool.Binding binding : tool.bindings())
                bound.add(binding.arg());

            assertThat(tool.name() + " arguments and bindings", bound, is(declared));
        }
    }

    /** repetition has no representation in the dispatcher, so nothing may declare it */
    @Test
    public void testArrayQueryBindingsAreNotExploded()
    {
        for (McpTool tool : McpTools.all())
        {
            var properties = tool.inputSchema().getAsJsonObject("properties");
            for (McpTool.Binding binding : tool.bindings())
            {
                if (binding.where() != McpTool.Where.QUERY)
                    continue;
                var type = properties.getAsJsonObject(binding.arg()).get("type");
                if (type != null && "array".equals(type.getAsString()))
                    assertThat(tool.name() + "." + binding.arg() + " explode", binding.explode(), is(false));
            }
        }
    }

    @Test
    public void testTheThreeDeliberateAbsencesStayAbsent()
    {
        var operations = new TreeSet<String>();
        for (McpTool tool : McpTools.all())
            operations.add(tool.operationId());

        for (String excluded : List.of("getOpenApiDocument", "createPairingRequest", "pollPairingRequest"))
            assertThat(excluded + " must not be a tool", operations.contains(excluded), is(false));
    }

    @Test
    public void testNamesFollowTheNamingRules()
    {
        var seen = new TreeSet<String>();
        for (McpTool tool : McpTools.all())
        {
            assertThat(tool.name() + " is unique", seen.add(tool.name()), is(true));
            assertThat(tool.name() + " matches the naming rules", TOOL_NAME.matcher(tool.name()).matches(), is(true));
            // MCP's limit is 64; the longest here is 34
            assertThat(tool.name() + " length", tool.name().length() <= 64, is(true));
        }
    }

    /**
     * Annotate honestly. Nothing depends on a client acting on an annotation,
     * but a flag contradicting the method is a lie told to whichever one does.
     */
    @Test
    public void testAnnotationsMatchWhatTheToolActuallyDoes()
    {
        for (McpTool tool : McpTools.all())
        {
            var annotations = tool.annotations();
            var readOnly = "GET".equals(tool.method());

            assertThat(tool.name() + " readOnlyHint", annotations.get("readOnlyHint").getAsBoolean(), is(readOnly));
            // MCP defaults destructiveHint to true whenever readOnlyHint is
            // false, and a merge patch clears fields when given null, so it is
            // not additive either
            assertThat(tool.name() + " destructiveHint", annotations.get("destructiveHint").getAsBoolean(),
                            is(!readOnly));
            assertThat(tool.name() + " idempotentHint", annotations.get("idempotentHint").getAsBoolean(), is(true));
            // a closed local system, not the open internet
            assertThat(tool.name() + " openWorldHint", annotations.get("openWorldHint").getAsBoolean(), is(false));
        }
    }

    /** exactly two writes, and they are the two the surface names */
    @Test
    public void testTheWriteToolsAreTheTwoExpectedOnes()
    {
        var writes = new TreeSet<String>();
        for (McpTool tool : McpTools.all())
        {
            if (!tool.isReadOnly())
                writes.add(tool.name());
        }

        assertThat(writes, is(new TreeSet<>(Set.of("pp_update_instrument", "pp_delete_instrument"))));
    }

    @Test
    public void testEveryToolAndArgumentCarriesText()
    {
        for (McpTool tool : McpTools.all())
        {
            assertThat(tool.name() + " description", tool.description().isBlank(), is(false));

            var schema = tool.inputSchema();
            assertThat(tool.name() + " forbids additional properties",
                            schema.get("additionalProperties").getAsBoolean(), is(false));

            var properties = schema.getAsJsonObject("properties");
            for (String argument : properties.keySet())
            {
                var description = properties.getAsJsonObject(argument).get("description");
                assertThat(tool.name() + "." + argument + " description", description, is(notNullValue()));
                assertThat(tool.name() + "." + argument + " description",
                            description.getAsString().isBlank(), is(false));
            }
        }
    }

    /**
     * The model widens and narrows the groups itself, which is how an oversized
     * response recovers - so its words and the envelope's must be the same seven.
     */
    @Test
    public void testTheMetricsEnumMatchesTheEnvelope()
    {
        var found = new ArrayList<List<String>>();
        for (McpTool tool : McpTools.all())
        {
            var properties = tool.inputSchema().getAsJsonObject("properties");
            if (!properties.has("metrics"))
                continue;

            var values = new ArrayList<String>();
            properties.getAsJsonObject("metrics").getAsJsonObject("items").getAsJsonArray("enum")
                            .forEach(element -> values.add(element.getAsString()));
            found.add(values);
        }

        assertThat("two tools take a metrics selection", found, hasSize(2));
        for (List<String> values : found)
            assertThat(values, is(McpEnvelope.METRIC_GROUPS));
    }

    private Router createRouter()
    {
        var host = new FakeHost(List.of());
        return ApiRoutes.create(new FileAccessRegistry(node), host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
    }
}
