package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

@SuppressWarnings("nls")
public class PatchSecurityTest
{
    private static JsonObject json(String body)
    {
        return JsonParser.parseString(body).getAsJsonObject();
    }

    @Test
    public void testPatchSingleFieldLeavesOthersUntouched()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Old");
        security.setIsin("DE0001234567");

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"name\":\"New\"}"));

        assertThat(security.getName(), is("New"));
        assertThat(security.getIsin(), is("DE0001234567"));
    }

    @Test
    public void testNullClearsOptionalField()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setIsin("DE0001234567");

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"isin\":null}"));

        assertThat(security.getIsin(), nullValue());
    }

    @Test
    public void testEmptyPatchDoesNotMarkFileDirty()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        var dirty = new AtomicBoolean();
        client.addPropertyChangeListener("dirty", event -> dirty.set(true));

        SecuritiesHandler.patch(client, security.getUUID(), json("{}"));
        assertThat(dirty.get(), is(false));

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"name\":\"New\"}"));
        assertThat(dirty.get(), is(true));
    }

    @Test
    public void testAllViolationsReportedAtOnceAndModelUnchanged()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Old");

        try
        {
            SecuritiesHandler.patch(client, security.getUUID(),
                            json("{\"name\":null,\"bogus\":1,\"currencyCode\":\"XXX\"}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().size(), is(3));
            assertThat(security.getName(), is("Old")); // nothing applied
        }
    }

    @Test
    public void testCurrencyChangeBlockedWhenTransactionsExist()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client); // EUR by default
        new PortfolioBuilder().buy(security, "2024-01-02", 100 * Values.Share.factor(), 1000 * Values.Amount.factor())
                        .addTo(client);

        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"currencyCode\":\"USD\"}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).code(), is("locked-by-transactions"));
        }
    }

    @Test
    public void testWriteGateReturns423WhileUserEditing() throws Exception
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        var node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        try
        {
            var registry = new FileAccessRegistry(node);
            registry.setEnabled("/tmp/x.portfolio", true);
            var host = new FakeHost(List.of(new FakeHost.FakeOpenFile("/tmp/x.portfolio", "x", client)));
            host.setUserEditing(true);

            var router = ApiRoutes.create(registry, host);
            var uuid = registry.byPath("/tmp/x.portfolio").orElseThrow().uuid();
            var match = router.match("PATCH", "/v1/files/" + uuid + "/instruments/" + security.getUUID());

            try
            {
                match.handler().handle(new Request("PATCH", "irrelevant", match.pathParams(),
                                "{\"name\":\"X\"}".getBytes(StandardCharsets.UTF_8)));
                Assert.fail("expected ApiException");
            }
            catch (ApiException e)
            {
                assertThat(e.getStatus(), is(423));
            }
        }
        finally
        {
            node.removeNode();
        }
    }
}
