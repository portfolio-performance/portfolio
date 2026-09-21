package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class ClientStoreTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Instant now = Instant.parse("2026-07-20T10:00:00Z");

    private Path dir()
    {
        return tempFolder.getRoot().toPath();
    }

    private ClientStore newStore()
    {
        return new ClientStore(dir(), () -> now);
    }

    @Test
    public void testPersistentTokenAuthenticatesAcrossInstances()
    {
        var token = newStore().addPersistentClient("Claude Code");

        assertThat(token.length() > 30, is(true));

        var client = newStore().authenticate(token);
        assertThat(client.isPresent(), is(true));
        assertThat(client.get().name(), is("Claude Code"));
        assertThat(client.get().session(), is(false));
    }

    @Test
    public void testStoredFileContainsHashButNeverThePlaintextToken() throws Exception
    {
        var token = newStore().addPersistentClient("Claude Code");

        var file = dir().resolve("api-clients.json");
        assertThat(Files.exists(file), is(true));

        var content = Files.readString(file);
        assertThat(content.contains(token), is(false));
        assertThat(content.contains("Claude Code"), is(true));
    }

    @Test
    public void testClientFileIsOwnerOnlyOnPosix() throws Exception
    {
        newStore().addPersistentClient("Claude Code");

        var file = dir().resolve("api-clients.json");
        try
        {
            var permissions = Files.getPosixFilePermissions(file);
            assertThat(permissions, is(PosixFilePermissions.fromString("rw-------")));
        }
        catch (UnsupportedOperationException e)
        {
            // non-POSIX file system: nothing to assert
        }
    }

    @Test
    public void testSessionTokenNeverTouchesDiskAndDiesWithTheStore()
    {
        var store = newStore();
        var token = store.addSessionClient("Claude Code");

        assertThat(store.authenticate(token).isPresent(), is(true));
        assertThat(store.authenticate(token).get().session(), is(true));

        // nothing persisted: a fresh store (= app restart) rejects the token
        assertThat(Files.exists(dir().resolve("api-clients.json")), is(false));
        assertThat(newStore().authenticate(token).isPresent(), is(false));
    }

    @Test
    public void testRevokeInvalidatesImmediately()
    {
        var store = newStore();
        var token = store.addPersistentClient("Claude Code");

        store.revoke(store.listClients().get(0).id());

        assertThat(store.authenticate(token).isPresent(), is(false));
        assertThat(store.listClients(), hasSize(0));
        // revocation is persisted, too
        assertThat(newStore().authenticate(token).isPresent(), is(false));
    }

    @Test
    public void testRevokeSessionClient()
    {
        var store = newStore();
        var token = store.addSessionClient("Claude Code");

        store.revoke(store.listClients().get(0).id());

        assertThat(store.authenticate(token).isPresent(), is(false));
    }

    @Test
    public void testUnknownTokenIsRejected()
    {
        var store = newStore();
        store.addPersistentClient("Claude Code");

        assertThat(store.authenticate("not-a-token").isPresent(), is(false));
    }

    @Test
    public void testDuplicateNamesAreAllowed()
    {
        var store = newStore();
        var first = store.addPersistentClient("Claude Code");
        var second = store.addPersistentClient("Claude Code");

        assertThat(first, is(not(second)));
        assertThat(store.listClients(), hasSize(2));
    }

    /**
     * The store is the single minting boundary, so it must sanitize the name
     * regardless of caller — the manual "Add client" path must not be able to
     * store control characters that the pairing path strips.
     */
    @Test
    public void testNameIsSanitizedOnMint()
    {
        var store = newStore();
        store.addPersistentClient("Claude Code\r\n");

        assertThat(store.listClients().get(0).name(), is("Claude Code"));
    }

    /**
     * The revoke handle exposed to the UI must be an opaque identity of its
     * own, not the token's hash — otherwise the UI is coupled to the hashing
     * scheme. It must also be stable across a reload.
     */
    @Test
    public void testClientIdIsAnOpaqueHandleIndependentOfTheToken()
    {
        var token = newStore().addPersistentClient("Claude Code");

        var id = newStore().listClients().get(0).id();
        assertThat(id, is(not(token)));
        assertThat(UUID.fromString(id).toString(), is(id)); // a synthetic UUID, not a token hash

        // stable across reloads, so the UI's revoke handle keeps working
        assertThat(newStore().listClients().get(0).id(), is(id));
    }

    @Test
    public void testBlankNameIsRejected()
    {
        var store = newStore();
        try
        {
            store.addPersistentClient("  \r\n");
            assertThat("expected IllegalArgumentException", false, is(true));
        }
        catch (IllegalArgumentException e)
        {
            // expected: nothing left after sanitization
        }
        assertThat(store.listClients(), hasSize(0));
    }

    private void writeStore(String content) throws Exception
    {
        Files.writeString(dir().resolve("api-clients.json"), content);
    }

    /** a syntactically broken file must not leave the API dead - start empty */
    @Test
    public void testFileThatIsNotJsonStartsEmpty() throws Exception
    {
        writeStore("this is not json");

        assertThat(newStore().listClients(), hasSize(0));
    }

    /** a corrupt date must be handled like any other malformed data, not crash construction */
    @Test
    public void testEntryWithMalformedDateIsSkipped() throws Exception
    {
        writeStore("{\"clients\":[{\"id\":\"i\",\"tokenHash\":\"h\",\"name\":\"n\",\"created\":\"not-a-date\"}]}");

        assertThat(newStore().listClients(), hasSize(0));
    }

    /** one malformed entry must not discard the sibling entries that parse fine */
    @Test
    public void testValidEntriesSurviveAMalformedSibling() throws Exception
    {
        var good = "{\"id\":\"g\",\"tokenHash\":\"h\",\"name\":\"Good\",\"created\":\"2026-07-20T10:00:00Z\"}";
        var bad = "{\"id\":\"b\",\"name\":\"Bad\",\"created\":\"2026-07-20T10:00:00Z\"}"; // no tokenHash
        writeStore("{\"clients\":[" + good + "," + bad + "]}");

        var clients = newStore().listClients();
        assertThat(clients, hasSize(1));
        assertThat(clients.get(0).name(), is("Good"));
    }

    @Test
    public void testLastUsedIsTrackedButFileWritesAreThrottled()
    {
        var token = newStore().addPersistentClient("Claude Code");

        var store = newStore();
        assertThat(store.listClients().get(0).lastUsed(), is(nullValue()));

        store.authenticate(token);
        assertThat(newStore().listClients().get(0).lastUsed(), is(Instant.parse("2026-07-20T10:00:00Z")));

        // 30s later: tracked in memory, but not yet rewritten to disk
        now = Instant.parse("2026-07-20T10:00:30Z");
        store.authenticate(token);
        assertThat(store.listClients().get(0).lastUsed(), is(Instant.parse("2026-07-20T10:00:30Z")));
        assertThat(newStore().listClients().get(0).lastUsed(), is(Instant.parse("2026-07-20T10:00:00Z")));

        // 90s later: persisted again
        now = Instant.parse("2026-07-20T10:01:30Z");
        store.authenticate(token);
        assertThat(newStore().listClients().get(0).lastUsed(), is(Instant.parse("2026-07-20T10:01:30Z")));
    }
}
