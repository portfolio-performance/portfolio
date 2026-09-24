package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.internal.FileResolver;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.WriteContext;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * The write wrapper for handlers that support {@code ?dry_run=true} and
 * {@code clientRef}.
 */
@SuppressWarnings("nls")
public class WriteWithContextTest
{
    private static final String PATH = "/tmp/x.portfolio";

    private IEclipsePreferences node;
    private FakeHost host;
    private FakeHost.FakeOpenFile file;
    private FileResolver resolver;
    private String fileId;
    private final AtomicReference<WriteContext> seen = new AtomicReference<>();

    @Before
    public void setUp()
    {
        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(PATH, true);
        fileId = registry.byPath(PATH).orElseThrow().uuid();

        file = new FakeHost.FakeOpenFile(PATH, "x", new Client());
        host = new FakeHost(List.of(file));
        resolver = new FileResolver(registry, host);
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    private Response call(Map<String, String> query, String body) throws Exception
    {
        var handler = ApiRoutes.writeWith(resolver, host, (ctx, req) -> {
            seen.set(ctx);
            return Response.noContent();
        });
        return handler.handle(new Request("POST", "irrelevant", Map.of("file", fileId), query,
                        body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testDefaultsToRealRunWithoutClientRef() throws Exception
    {
        call(Map.of(), null);

        assertThat(seen.get().file(), is(file));
        assertThat(seen.get().dryRun(), is(false));
        assertThat(seen.get().clientRef(), is(nullValue()));
        assertThat(host.hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testDryRunAndClientRef() throws Exception
    {
        call(Map.of("dry_run", "true"), "{\"clientRef\": \"broker-2026-000123\", \"type\": \"buy\"}");

        assertThat(seen.get().dryRun(), is(true));
        assertThat(seen.get().clientRef(), is("broker-2026-000123"));
    }

    @Test
    public void testExplicitFalseIsARealRun() throws Exception
    {
        call(Map.of("dry_run", "false"), null);
        assertThat(seen.get().dryRun(), is(false));
        assertThat(ApiRoutes.isDryRun(new Request("DELETE", "x", Map.of(), Map.of("dry_run", "true"), new byte[0])),
                        is(true));
    }

    @Test
    public void testInvalidDryRunValueIs400() throws Exception
    {
        try
        {
            call(Map.of("dry_run", "yes"), null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("dry_run"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    @Test
    public void testInvalidClientRefIs422() throws Exception
    {
        for (var body : List.of("{\"clientRef\": 42}", "{\"clientRef\": \" \"}",
                        "{\"clientRef\": \"" + "x".repeat(WriteContext.CLIENT_REF_MAX_LENGTH + 1) + "\"}"))
        {
            try
            {
                call(Map.of(), body);
                Assert.fail("expected ApiException for " + body);
            }
            catch (ApiException e)
            {
                assertThat(e.getStatus(), is(422));
                assertThat(e.getErrors().get(0).field(), is("clientRef"));
            }
        }
    }

    @Test
    public void testNonObjectBodyIsLeftToTheHandler() throws Exception
    {
        call(Map.of(), "[1, 2]");
        assertThat(seen.get().clientRef(), is(nullValue()));
    }

    @Test
    public void testIs423WhileUserEditing() throws Exception
    {
        host.setUserEditing(true);

        try
        {
            call(Map.of(), null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
            assertThat(seen.get(), is(nullValue()));
        }
    }
}
