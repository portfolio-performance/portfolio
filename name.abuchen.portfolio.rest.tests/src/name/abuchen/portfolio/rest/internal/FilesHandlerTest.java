package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.testsupport.FakeHost;
import name.abuchen.portfolio.rest.testsupport.FakeHost.FakeOpenFile;
import name.abuchen.portfolio.rest.testsupport.FakeHost.OpenBehavior;

@SuppressWarnings("nls")
public class FilesHandlerTest
{
    // platform-specific absolute paths: the open-file SPI takes a Path
    private static final String PATH = Path.of("target", "x.portfolio").toAbsolutePath().toString();
    private static final String OTHER = Path.of("target", "other.portfolio").toAbsolutePath().toString();

    private IEclipsePreferences node;
    private FileAccessRegistry registry;
    private FakeHost host;
    private FakeOpenFile file;
    private Client client;
    private Security security;
    private String fileId;

    @Before
    public void setUp()
    {
        client = new Client();
        security = new SecurityBuilder().addTo(client);

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        registry = new FileAccessRegistry(node);
        registry.setEnabled(PATH, true);
        registry.setAlias(PATH, "main");
        fileId = registry.byPath(PATH).orElseThrow().uuid();

        file = new FakeOpenFile(PATH, "x.portfolio", client);
        host = new FakeHost(List.of(file));
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    private Response call(String method, String path, String body) throws Exception
    {
        var router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
        var match = router.match(method, path);
        return match.handler().handle(new Request(method, path, match.pathParams(),
                        body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8)));
    }

    private static JsonObject json(Response response)
    {
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static ApiException expectApiException(ThrowingRunnable runnable) throws Exception
    {
        try
        {
            runnable.run();
        }
        catch (ApiException e)
        {
            return e;
        }
        Assert.fail("expected ApiException");
        return null;
    }

    private interface ThrowingRunnable
    {
        void run() throws Exception;
    }

    @Test
    public void testListAndGetReportDirtyFlag() throws Exception
    {
        var list = json(call("GET", "/v1/files", null));
        var item = list.getAsJsonArray("items").get(0).getAsJsonObject();
        assertThat(item.get("dirty").getAsBoolean(), is(false));

        client.markDirty();

        list = json(call("GET", "/v1/files", null));
        item = list.getAsJsonArray("items").get(0).getAsJsonObject();
        assertThat(item.get("dirty").getAsBoolean(), is(true));

        var single = json(call("GET", "/v1/files/" + fileId, null));
        assertThat(single.get("id").getAsString(), is(fileId));
        assertThat(single.get("alias").getAsString(), is("main"));
        assertThat(single.get("path").getAsString(), is(PATH));
        assertThat(single.get("dirty").getAsBoolean(), is(true));

        // addressable by alias, too
        assertThat(json(call("GET", "/v1/files/main", null)).get("id").getAsString(), is(fileId));
    }

    @Test
    public void testGetUnknownFileIs404() throws Exception
    {
        var e = expectApiException(() -> call("GET", "/v1/files/" + UUID.randomUUID(), null));
        assertThat(e.getStatus(), is(404));
    }

    @Test
    public void testWriteMarksDirtyAndSaveClearsIt() throws Exception
    {
        call("PATCH", "/v1/files/" + fileId + "/instruments/" + security.getUUID(), "{\"name\":\"New\"}");
        assertThat(json(call("GET", "/v1/files/" + fileId, null)).get("dirty").getAsBoolean(), is(true));

        var response = call("POST", "/v1/files/" + fileId + "/save", null);

        assertThat(response.status(), is(200));
        var body = json(response);
        assertThat(body.get("dirty").getAsBoolean(), is(false));
        assertThat(body.get("savedAt"), is(notNullValue()));
        assertThat(body.get("id").getAsString(), is(fileId));
        assertThat(file.saveCount(), is(1));
        assertThat(file.isDirty(), is(false));
    }

    private Response save(Map<String, String> query) throws Exception
    {
        var router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
        var path = "/v1/files/" + fileId + "/save";
        var match = router.match("POST", path);
        return match.handler().handle(new Request("POST", path, match.pathParams(), query, new byte[0]));
    }

    @Test
    public void testSaveWaitsForBackgroundUpdatesOffTheUIThread() throws Exception
    {
        file.setUIThreadProbe(host::isInSyncExec);

        var body = json(save(Map.of()));

        assertThat(file.awaitedTimeout(), is(Duration.ofSeconds(30)));
        assertThat(file.awaitedOnUIThread(), is(false));
        assertThat(body.get("backgroundUpdatesPending").getAsBoolean(), is(false));
        assertThat(file.saveCount(), is(1));
    }

    @Test
    public void testSaveReportsBackgroundUpdateStillRunning() throws Exception
    {
        file.setBackgroundUpdateFinishes(false);

        var body = json(save(Map.of("waitForUpdates", "5")));

        assertThat(file.awaitedTimeout(), is(Duration.ofSeconds(5)));
        assertThat(body.get("backgroundUpdatesPending").getAsBoolean(), is(true));
        assertThat(file.saveCount(), is(1));
    }

    @Test
    public void testSaveWithoutWaiting() throws Exception
    {
        file.setBackgroundUpdateFinishes(false);

        var body = json(save(Map.of("waitForUpdates", "0")));

        assertThat(file.awaitedTimeout(), is(nullValue()));
        assertThat(body.get("backgroundUpdatesPending").getAsBoolean(), is(false));
    }

    @Test
    public void testSaveRejectsInvalidWait() throws Exception
    {
        for (var value : List.of("-1", "61", "soon"))
        {
            var e = expectApiException(() -> save(Map.of("waitForUpdates", value)));
            assertThat(e.getStatus(), is(400));
        }
        assertThat(file.saveCount(), is(0));
    }

    @Test
    public void testSaveRunsOnUIThread() throws Exception
    {
        call("POST", "/v1/files/" + fileId + "/save", null);

        assertThat(host.hasAccessedOutsideUIThread(), is(false));
        assertThat(host.syncExecResults().stream().anyMatch(Response.class::isInstance), is(true));
    }

    @Test
    public void testSaveIs423WhileUserEditing() throws Exception
    {
        host.setUserEditing(true);

        var e = expectApiException(() -> call("POST", "/v1/files/" + fileId + "/save", null));

        assertThat(e.getStatus(), is(423));
        assertThat(file.saveCount(), is(0));
    }

    @Test
    public void testSaveFailureIs500SaveFailed() throws Exception
    {
        file.failSaveWith(new IOException("disk full"));
        client.markDirty();

        var e = expectApiException(() -> call("POST", "/v1/files/" + fileId + "/save", null));

        assertThat(e.getStatus(), is(500));
        assertThat(e.getType(), is("save-failed"));
        assertThat(e.getDetail(), is("disk full"));
        assertThat(file.isDirty(), is(true));
    }

    @Test
    public void testSaveUnknownFileIs404() throws Exception
    {
        var e = expectApiException(() -> call("POST", "/v1/files/" + UUID.randomUUID() + "/save", null));
        assertThat(e.getStatus(), is(404));
    }

    @Test
    public void testOpenAlreadyOpenFileReturnsIt() throws Exception
    {
        var response = call("POST", "/v1/files/open", "{\"path\":" + quote(PATH) + "}");

        assertThat(response.status(), is(200));
        assertThat(json(response).get("id").getAsString(), is(fileId));
        assertThat(host.openedPaths(), is(empty()));
    }

    @Test
    public void testOpenEnabledFileOpensIt() throws Exception
    {
        registry.setEnabled(OTHER, true);
        var otherId = registry.byPath(OTHER).orElseThrow().uuid();
        host.addOpenableFile(OTHER, new Client(), OpenBehavior.LOADED);

        var response = call("POST", "/v1/files/open", "{\"path\":" + quote(OTHER) + "}");

        assertThat(response.status(), is(201));
        assertThat(response.headers().get("Location"), is("/v1/files/" + otherId));
        var body = json(response);
        assertThat(body.get("id").getAsString(), is(otherId));
        assertThat(body.get("path").getAsString(), is(OTHER));
        assertThat(body.get("dirty").getAsBoolean(), is(false));

        // now addressable
        assertThat(json(call("GET", "/v1/files", null)).getAsJsonArray("items").size(), is(2));
        assertThat(call("GET", "/v1/files/" + otherId, null).status(), is(200));

        assertThat(host.hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testOpenFileNotEnabledIs404AndDoesNotTouchIt() throws Exception
    {
        registry.ensureRecord(OTHER); // known, but not enabled
        host.addOpenableFile(OTHER, new Client(), OpenBehavior.LOADED);

        var e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":" + quote(OTHER) + "}"));

        assertThat(e.getStatus(), is(404));
        assertThat(host.openedPaths(), is(empty()));
    }

    @Test
    public void testOpenUnknownPathIs404() throws Exception
    {
        var unknown = Path.of("target", "unknown.portfolio").toAbsolutePath().toString();

        var e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":" + quote(unknown) + "}"));

        assertThat(e.getStatus(), is(404));
        assertThat(host.openedPaths(), is(empty()));
    }

    @Test
    public void testOpenEnabledButMissingFileIs404() throws Exception
    {
        registry.setEnabled(OTHER, true);

        var e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":" + quote(OTHER) + "}"));

        assertThat(e.getStatus(), is(404));
    }

    @Test
    public void testOpenEncryptedFileIs409PasswordRequired() throws Exception
    {
        registry.setEnabled(OTHER, true);
        host.addOpenableFile(OTHER, new Client(), OpenBehavior.ENCRYPTED);

        var e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":" + quote(OTHER) + "}"));

        assertThat(e.getStatus(), is(409));
        assertThat(e.getType(), is("password-required"));
    }

    @Test
    public void testOpenFileNeedingMigrationIs409MigrationRequired() throws Exception
    {
        registry.setEnabled(OTHER, true);
        host.addOpenableFile(OTHER, new Client(), OpenBehavior.MIGRATION_REQUIRED);

        var e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":" + quote(OTHER) + "}"));

        assertThat(e.getStatus(), is(409));
        assertThat(e.getType(), is("migration-required"));
    }

    @Test
    public void testOpenFailingFileIs409OpenFailed() throws Exception
    {
        registry.setEnabled(OTHER, true);
        host.addOpenableFile(OTHER, new Client(), OpenBehavior.FAILS);

        var e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":" + quote(OTHER) + "}"));

        assertThat(e.getStatus(), is(409));
        assertThat(e.getType(), is("open-failed"));
        assertThat(e.getDetail(), is("corrupt file"));
    }

    @Test
    public void testOpenTimesOutWith503WhileLoading() throws Exception
    {
        registry.setEnabled(OTHER, true);
        host.addOpenableFile(OTHER, new Client(), OpenBehavior.PENDING);

        var files = new FilesHandler(registry, host);
        files.setOpenTimeout(Duration.ofMillis(50));

        var e = expectApiException(() -> files.open(OTHER));

        assertThat(e.getStatus(), is(503));
        assertThat(e.getType(), is("file-loading"));
        assertThat(e.getHeaders().get("Retry-After"), is("5"));
    }

    @Test
    public void testOpenIs423WhileUserEditing() throws Exception
    {
        registry.setEnabled(OTHER, true);
        host.addOpenableFile(OTHER, new Client(), OpenBehavior.LOADED);
        host.setUserEditing(true);

        var e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":" + quote(OTHER) + "}"));

        assertThat(e.getStatus(), is(423));
        assertThat(host.openedPaths(), is(empty()));
    }

    @Test
    public void testOpenRequiresPath() throws Exception
    {
        var e = expectApiException(() -> call("POST", "/v1/files/open", "{}"));
        assertThat(e.getStatus(), is(422));
        assertThat(e.getErrors().get(0).code(), is("required"));

        e = expectApiException(() -> call("POST", "/v1/files/open", "{\"path\":42}"));
        assertThat(e.getStatus(), is(422));
        assertThat(e.getErrors().get(0).code(), is("invalid-type"));

        e = expectApiException(() -> call("POST", "/v1/files/open", "[]"));
        assertThat(e.getStatus(), is(400));
    }

    private static String quote(String value)
    {
        var json = new JsonObject();
        json.addProperty("v", value);
        return json.get("v").toString();
    }
}
