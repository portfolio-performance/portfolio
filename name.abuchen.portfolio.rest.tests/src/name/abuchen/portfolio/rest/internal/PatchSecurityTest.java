package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.spi.WriteConfirmation;
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

    /**
     * The application's ISIN column refuses what fails the check digit; the API
     * used to store it anyway. The whole patch fails, as for any other field.
     */
    @Test
    public void testAnInvalidIsinIsRejectedAndNothingIsApplied()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Old");
        security.setIsin("DE0005557508");

        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"name\":\"New\",\"isin\":\"KEINE-ISIN\"}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().size(), is(1));
            assertThat(e.getErrors().get(0).field(), is("isin"));
            assertThat(e.getErrors().get(0).code(), is("invalid-isin"));
            assertThat(security.getName(), is("Old"));
            assertThat(security.getIsin(), is("DE0005557508"));
        }

        // a wrong check digit fails the same way
        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"isin\":\"DE0005557509\"}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).code(), is("invalid-isin"));
        }
    }

    /** surrounding whitespace is ignored and a blank ISIN clears it, as in the ISIN column */
    @Test
    public void testIsinIsTrimmedAndBlankClearsIt()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"isin\":\"  IE00BK1PV551 \"}"));
        assertThat(security.getIsin(), is("IE00BK1PV551"));

        var restated = SecuritiesHandler.patch(client, security.getUUID(), json("{\"isin\":\"IE00BK1PV551 \"}"));
        assertThat(restated.changes().isEmpty(), is(true));

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"isin\":\"   \"}"));
        assertThat(security.getIsin(), nullValue());
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
    public void testCurrencyClearedToNullMarksIndex()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client); // EUR by default

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"currencyCode\":null}"));

        assertThat(security.getCurrencyCode(), nullValue());
    }

    @Test
    public void testClearCurrencyRejectedForExchangeRate()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setTargetCurrencyCode("USD"); // makes it an exchange rate

        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"currencyCode\":null}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).code(), is("exchange-rate-requires-currency"));
            assertThat(security.getCurrencyCode(), is("EUR")); // nothing applied
        }
    }

    @Test
    public void testClearCurrencyBlockedWhenTransactionsExist()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client); // EUR by default
        new PortfolioBuilder().buy(security, "2024-01-02", 100 * Values.Share.factor(), 1000 * Values.Amount.factor())
                        .addTo(client);

        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"currencyCode\":null}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).code(), is("locked-by-transactions"));
        }
    }

    /**
     * Reported from a live test: setting a value to what it already was marked
     * the file dirty, and the user was asked to save a file nothing had changed.
     */
    @Test
    public void testAPatchThatRestatesValuesDoesNotMarkFileDirty()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Same");

        var dirty = new AtomicBoolean();
        client.addPropertyChangeListener("dirty", event -> dirty.set(true));

        var result = SecuritiesHandler.patch(client, security.getUUID(), json("{\"name\":\"Same\"}"));
        assertThat(result.changes().isEmpty(), is(true));
        assertThat(dirty.get(), is(false));
    }

    /** the response says whether anything changed, which its body cannot */
    @Test
    public void testThePatchResponseSaysWhetherAValueChanged() throws Exception
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Same");

        var node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        try
        {
            var registry = new FileAccessRegistry(node);
            registry.setEnabled("/tmp/x.portfolio", true);
            var host = new FakeHost(List.of(new FakeHost.FakeOpenFile("/tmp/x.portfolio", "x", client)));
            var router = ApiRoutes.create(registry, host,
                            new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
            var uuid = registry.byPath("/tmp/x.portfolio").orElseThrow().uuid();
            var match = router.match("PATCH", "/v1/files/" + uuid + "/instruments/" + security.getUUID());

            var restated = match.handler().handle(new Request("PATCH", "irrelevant", match.pathParams(),
                            "{\"name\":\"Same\"}".getBytes(StandardCharsets.UTF_8)));
            assertThat(restated.headers().get(ApiRoutes.CHANGED_HEADER), is("false"));

            var changed = match.handler().handle(new Request("PATCH", "irrelevant", match.pathParams(),
                            "{\"name\":\"Other\"}".getBytes(StandardCharsets.UTF_8)));
            assertThat(changed.headers().get(ApiRoutes.CHANGED_HEADER), is("true"));
        }
        finally
        {
            node.removeNode();
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

            var router = ApiRoutes.create(registry, host,
                            new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
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

    /**
     * A change the user refuses is refused in words the caller can act on: the
     * operation named, because an agent has to know which of its own calls a "no"
     * belonged to - and because that same name is what "allow for this session" is
     * remembered under, so getting it wrong would let one yes cover a different
     * operation. The prompt is asked before the change is queued, so refusing
     * leaves the file exactly as it was.
     */
    @Test
    public void testARefusalNamesTheOperationItRefuses() throws Exception
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        var node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        try
        {
            var registry = new FileAccessRegistry(node);
            registry.setEnabled("/tmp/x.portfolio", true);
            var host = new FakeHost(List.of(new FakeHost.FakeOpenFile("/tmp/x.portfolio", "x", client)));
            host.setWriteApprovalAutoAllows(false);

            var router = ApiRoutes.create(registry, host,
                            new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
            var uuid = registry.byPath("/tmp/x.portfolio").orElseThrow().uuid();
            var match = router.match("PATCH", "/v1/files/" + uuid + "/instruments/" + security.getUUID());

            // the route parks on the answer, so the answer comes from elsewhere
            var answering = new Thread(() ->
            {
                WriteConfirmation seen = null;
                for (int i = 0; i < 2000 && seen == null; i++)
                {
                    seen = host.lastWriteRequest();
                    try
                    {
                        Thread.sleep(5);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (seen != null)
                    seen.decline();
            });
            answering.setDaemon(true);
            answering.start();

            try
            {
                match.handler().handle(new Request("PATCH", "irrelevant", match.pathParams(),
                                "{\"note\":\"anders\"}".getBytes(StandardCharsets.UTF_8)));
                Assert.fail("a refused write went through");
            }
            catch (ApiException e)
            {
                assertThat(e.getStatus(), is(403));
                assertThat(e.getType(), is("write-declined"));
                assertThat(e.getDetail(), containsString("pp_update_instrument"));
            }

            var prompt = host.lastWriteRequest();
            assertThat(prompt, is(notNullValue()));
            assertThat(prompt.getOperation(), is("pp_update_instrument"));
            assertThat(prompt.getSubjectName(), is("ACME"));
            assertThat(prompt.getFileLabel(), is("x"));

            // and the refusal did not grant anything, to this or any other call
            assertThat(host.writeRequests(), is(1));
            assertThat(security.getNote(), is(nullValue()));
        }
        finally
        {
            node.removeNode();
        }
    }

    /**
     * The prompt is asked only about a change that could be carried out: the fields
     * are read - and rejected - before anybody is interrupted. A user who is asked
     * to approve a patch that fails afterwards has learned that approving teaches
     * nothing, which is exactly the habit this prompt is meant to keep.
     */
    @Test
    public void testDescribeRefusesAnUnknownFieldBeforeThePrompt()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        try
        {
            SecuritiesHandler.describe(client, security.getUUID(), json("{\"konnomen\":\"x\"}"), "x");
            Assert.fail("a patch naming a field it cannot write was accepted for the prompt");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).field(), is("konnomen"));
            assertThat(e.getErrors().get(0).code(), is("unknown-field"));
        }

        assertThat("the instrument was not touched", security.getNote(), is(nullValue()));
    }

    /** an instrument the file does not have is turned away before anybody is asked */
    @Test
    public void testDescribeRefusesAnInstrumentNobodyHas()
    {
        var client = new Client();

        try
        {
            SecuritiesHandler.describe(client, UUID.randomUUID().toString(), json("{\"note\":\"x\"}"), "x");
            Assert.fail("an unknown instrument was accepted for the prompt");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    @Test
    public void testDescribeOfAGoodPatchNamesTheInstrumentAndItsFields()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        var facts = SecuritiesHandler.describe(client, security.getUUID(), json("{\"note\":\"anders\"}"), "datei");

        assertThat(facts.fileLabel(), is("datei"));
        assertThat(facts.subjectType(), is("instrument"));
        assertThat(facts.subjectName(), is("ACME"));
        assertThat(facts.detail(), is("note"));
        // the operation is the key a session grant is remembered under, and the
        // describer is not the one who decides it
        assertThat(facts.operation(), is(nullValue()));
    }
}
