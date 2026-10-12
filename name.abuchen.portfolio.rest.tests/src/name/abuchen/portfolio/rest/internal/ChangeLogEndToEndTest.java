package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

@SuppressWarnings("nls")
public class ChangeLogEndToEndTest
{
    @Test
    public void testPatchThroughRouterLogsChangeWithFileLabel() throws Exception
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Old");

        var captured = new CopyOnWriteArrayList<IStatus>();
        ILogListener listener = (status, plugin) -> captured.add(status);
        ILog log = Platform.getLog(FrameworkUtil.getBundle(PortfolioLog.class));
        log.addLogListener(listener);

        var node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        try
        {
            var registry = new FileAccessRegistry(node);
            registry.setEnabled("/tmp/x.portfolio", true);
            var host = new FakeHost(List.of(new FakeHost.FakeOpenFile("/tmp/x.portfolio", "MyFile", client)));

            var router = ApiRoutes.create(registry, host,
                            new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
            var uuid = registry.byPath("/tmp/x.portfolio").orElseThrow().uuid();
            var match = router.match("PATCH", "/v1/files/" + uuid + "/instruments/" + security.getUUID());

            match.handler().handle(new Request("PATCH", "irrelevant", match.pathParams(),
                            "{\"name\":\"New\"}".getBytes(StandardCharsets.UTF_8)));

            var entry = captured.stream() //
                            .filter(IStatus::isMultiStatus) //
                            .filter(s -> s.getMessage().contains("New")) //
                            .findFirst().orElse(null);

            assertThat(entry, is(notNullValue()));
            assertThat(entry.getMessage(), containsString("MyFile"));
            assertThat(entry.getChildren().length, is(1));
            assertThat(entry.getChildren()[0].getMessage(), is("name: 'Old' → 'New'"));
        }
        finally
        {
            log.removeLogListener(listener);
            node.removeNode();
        }
    }

    @Test
    public void testDeleteThroughRouterLogsSingleLineEntry() throws Exception
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Doomed");

        var captured = new CopyOnWriteArrayList<IStatus>();
        ILogListener listener = (status, plugin) -> captured.add(status);
        ILog log = Platform.getLog(FrameworkUtil.getBundle(PortfolioLog.class));
        log.addLogListener(listener);

        var node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        try
        {
            var registry = new FileAccessRegistry(node);
            registry.setEnabled("/tmp/x.portfolio", true);
            var host = new FakeHost(List.of(new FakeHost.FakeOpenFile("/tmp/x.portfolio", "MyFile", client)));

            var router = ApiRoutes.create(registry, host,
                            new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
            var uuid = registry.byPath("/tmp/x.portfolio").orElseThrow().uuid();
            var match = router.match("DELETE", "/v1/files/" + uuid + "/instruments/" + security.getUUID());

            match.handler().handle(new Request("DELETE", "irrelevant", match.pathParams(), new byte[0]));

            var entry = captured.stream() //
                            .filter(s -> !s.isMultiStatus()) //
                            .filter(s -> s.getMessage().contains("Doomed")) //
                            .findFirst().orElse(null);

            assertThat(entry, is(notNullValue()));
            assertThat(entry.getMessage(), containsString("MyFile"));
            assertThat(entry.getSeverity(), is(IStatus.INFO));
        }
        finally
        {
            log.removeLogListener(listener);
            node.removeNode();
        }
    }
}
