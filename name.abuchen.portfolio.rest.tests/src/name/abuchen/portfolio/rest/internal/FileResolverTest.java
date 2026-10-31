package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

@SuppressWarnings("nls")
public class FileResolverTest
{
    private IEclipsePreferences node;
    private FileAccessRegistry registry;

    @Before
    public void setUp()
    {
        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        registry = new FileAccessRegistry(node);
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    @Test
    public void testUnknownAndDisabledAreIndistinguishable404()
    {
        registry.ensureRecord("/tmp/a.portfolio"); // exists but not enabled
        var resolver = new FileResolver(registry, new FakeHost(List.of()));

        int unknownStatus = statusOf(resolver, "does-not-exist");
        int disabledStatus = statusOf(resolver, registry.byPath("/tmp/a.portfolio").orElseThrow().uuid());

        assertThat(unknownStatus, is(404));
        assertThat(disabledStatus, is(404));
    }

    @Test
    public void testEnabledButNotOpenIs409FileNotOpen()
    {
        registry.setEnabled("/tmp/a.portfolio", true);
        var resolver = new FileResolver(registry, new FakeHost(List.of()));

        var uuid = registry.byPath("/tmp/a.portfolio").orElseThrow().uuid();
        try
        {
            resolver.resolve(uuid);
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(409));
            assertThat(e.getType(), is("file-not-open"));
        }
    }

    @Test
    public void testEnabledAndOpenResolves()
    {
        registry.setEnabled("/tmp/a.portfolio", true);
        var host = new FakeHost(List.of(new FakeHost.FakeOpenFile("/tmp/a.portfolio", "a", new Client())));
        var resolver = new FileResolver(registry, host);

        var uuid = registry.byPath("/tmp/a.portfolio").orElseThrow().uuid();
        assertThat(resolver.resolve(uuid).file().getPath(), is("/tmp/a.portfolio"));
    }

    private static int statusOf(FileResolver resolver, String id)
    {
        try
        {
            resolver.resolve(id);
            throw new AssertionError("expected ApiException");
        }
        catch (ApiException e)
        {
            return e.getStatus();
        }
    }
}
