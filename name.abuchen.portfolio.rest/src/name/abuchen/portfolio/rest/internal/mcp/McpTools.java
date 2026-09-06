package name.abuchen.portfolio.rest.internal.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.framework.FrameworkUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Reads the sixteen tools from {@code mcp-tools.json}. Hand-authored and
 * guarded by {@code McpToolsDriftTest}, which is how {@code openapi.yaml} is
 * kept honest too; there is no generator and no {@code package.json}.
 * <p/>
 * Like the OpenAPI document beside it, the file lives at the bundle root rather
 * than on the classpath, so it is read as a bundle entry - which resolves both
 * from the project directory and from the packaged bundle.
 */
public final class McpTools
{
    private static final String RESOURCE = "/mcp-tools.json"; //$NON-NLS-1$

    /** the four fields a client is shown; the rest are ours */
    private static final List<String> PAYLOAD_FIELDS = List.of("name", "description", "inputSchema", "annotations"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private static volatile List<McpTool> cached;

    private McpTools()
    {
    }

    public static List<McpTool> all()
    {
        var tools = cached;
        if (tools == null)
        {
            tools = parse(read());
            cached = tools;
        }
        return tools;
    }

    public static Optional<McpTool> byName(String name)
    {
        return all().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    /** the four published fields, rebuilt per call so no caller can mutate the next one's copy */
    public static JsonArray listPayload()
    {
        var array = new JsonArray();
        for (McpTool tool : all())
        {
            var json = new JsonObject();
            json.addProperty(PAYLOAD_FIELDS.get(0), tool.name());
            json.addProperty(PAYLOAD_FIELDS.get(1), tool.description());
            json.add(PAYLOAD_FIELDS.get(2), tool.inputSchema().deepCopy());
            json.add(PAYLOAD_FIELDS.get(3), tool.annotations().deepCopy());
            array.add(json);
        }
        return array;
    }

    private static List<McpTool> parse(JsonObject document)
    {
        var tools = new ArrayList<McpTool>();

        for (var element : document.getAsJsonArray("tools")) //$NON-NLS-1$
        {
            var json = element.getAsJsonObject();
            var bindings = new ArrayList<McpTool.Binding>();

            for (var entry : json.getAsJsonArray("bindings")) //$NON-NLS-1$
            {
                var binding = entry.getAsJsonObject();
                var explode = binding.get("explode"); //$NON-NLS-1$
                bindings.add(new McpTool.Binding(binding.get("arg").getAsString(), //$NON-NLS-1$
                                McpTool.Where.valueOf(binding.get("where").getAsString().toUpperCase()), //$NON-NLS-1$
                                binding.get("wire").getAsString(), explode == null || explode.getAsBoolean())); //$NON-NLS-1$
            }

            tools.add(new McpTool(json.get("name").getAsString(), json.get("operationId").getAsString(), //$NON-NLS-1$ //$NON-NLS-2$
                            json.get("method").getAsString(), json.get("pathTemplate").getAsString(), //$NON-NLS-1$ //$NON-NLS-2$
                            json.get("description").getAsString(), json.getAsJsonObject("inputSchema"), //$NON-NLS-1$ //$NON-NLS-2$
                            json.getAsJsonObject("annotations"), List.copyOf(bindings))); //$NON-NLS-1$
        }

        return List.copyOf(tools);
    }

    private static JsonObject read()
    {
        try (InputStream in = documentUrl().openStream())
        {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot read " + RESOURCE, e); //$NON-NLS-1$
        }
    }

    private static URL documentUrl()
    {
        var bundle = FrameworkUtil.getBundle(McpTools.class);
        var url = bundle != null ? bundle.getEntry(RESOURCE) : McpTools.class.getResource(RESOURCE);
        if (url == null)
            throw new IllegalStateException(RESOURCE + " is not on the bundle - check build.properties bin.includes"); //$NON-NLS-1$
        return url;
    }
}
