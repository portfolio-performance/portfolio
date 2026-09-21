package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

@SuppressWarnings("nls")
public class AttributeTypesEndpointTest
{
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
    public void testDiscoveryRouteResolvesAndListsDefaultTypes() throws Exception
    {
        var client = new Client();
        new SecurityBuilder().addTo(client);

        var registry = new FileAccessRegistry(node);
        registry.setEnabled("/tmp/x.portfolio", true);
        var host = new FakeHost(List.of(new FakeHost.FakeOpenFile("/tmp/x.portfolio", "x", client)));
        var router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
        var fileId = registry.byPath("/tmp/x.portfolio").orElseThrow().uuid();

        var match = router.match("GET", "/v1/files/" + fileId + "/instruments/attribute-types");
        var response = match.handler().handle(
                        new Request("GET", "irrelevant", match.pathParams(), new byte[0]));

        var body = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
        var items = body.getAsJsonArray("items");

        var ids = new ArrayList<String>();
        String terType = null;
        boolean logoSupported = true;
        for (var element : items)
        {
            JsonObject item = element.getAsJsonObject();
            ids.add(item.get("id").getAsString());
            if ("ter".equals(item.get("id").getAsString()))
                terType = item.get("type").getAsString();
            if ("logo".equals(item.get("id").getAsString()))
                logoSupported = item.get("supported").getAsBoolean();
        }

        assertThat(ids, hasItem("ter"));
        assertThat(ids, hasItem("logo"));
        assertThat(terType, is("percent"));
        assertThat(logoSupported, is(false)); // image is discoverable but unsupported
    }
}
