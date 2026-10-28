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
import java.util.List;
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

        // nothing persistent to remove, so this is a complete revocation
        assertThat(store.revoke(store.listClients().get(0).id()), is(true));

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
     * The name is shown in the approval dialog, and whoever asks for access
     * chooses it: a right-to-left override, a zero-width character or a line
     * separator must not be able to make it read as another client's name.
     */
    @Test
    public void testInvisibleCharactersAreStrippedFromTheName()
    {
        assertThat(ClientStore.sanitizeName("Claude\u202E edoC"), is("Claude edoC"));
        assertThat(ClientStore.sanitizeName("Cla\u200Bude\u2066 Code\u2069"), is("Claude Code"));
        assertThat(ClientStore.sanitizeName("Claude\u2028Code\u2029"), is("ClaudeCode"));
        assertThat(ClientStore.sanitizeName("Büro-PC ✓"), is("Büro-PC ✓"));
    }

    /**
     * The file is replaced in one step, so a crash while writing leaves the
     * previous one intact, and no temporary file is left behind.
     */
    @Test
    public void testSavingLeavesNoTemporaryFile() throws Exception
    {
        var store = newStore();
        var first = store.addPersistentClient("First");
        store.addPersistentClient("Second");

        try (var files = Files.list(dir()))
        {
            assertThat(files.map(p -> p.getFileName().toString()).toList(), is(List.of("api-clients.json")));
        }
        assertThat(newStore().authenticate(first).isPresent(), is(true));
        assertThat(newStore().listClients().size(), is(2));
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

    /**
     * A client file that cannot be written makes this token one that outlives
     * nothing: it works now and disappears at the next restart, exactly like a
     * session client. Saying so is what lets the UI call it session-only instead
     * of presenting it as a permanent client that is silently not on disk.
     */
    @Test
    public void testUnwritableClientFileMarksTheTokenAsSessionOnly() throws Exception
    {
        Files.createDirectories(dir().resolve("api-clients.json")); // a directory where a file belongs

        var store = newStore();
        var token = store.addPersistentClient("Claude Code");

        assertThat(store.authenticate(token).isPresent(), is(true));
        assertThat(store.listClients().get(0).session(), is(true));
        assertThat(store.survivesRestart(token), is(false));

        // revoking it is complete, because nothing was ever written
        assertThat(store.revoke(store.listClients().get(0).id()), is(true));
        assertThat(store.authenticate(token).isPresent(), is(false));
    }

    /**
     * A revocation whose write failed is not a completed one. The client is gone
     * for this session, but the file still holds the token, so it authenticates
     * again after a restart - and the UI has to be able to tell the user that.
     */
    @Test
    public void testOnlyAWrittenPersistentClientSurvivesARestart()
    {
        var store = newStore();
        var persistent = store.addPersistentClient("Claude Code");
        var session = store.addSessionClient("Script");

        assertThat(store.survivesRestart(persistent), is(true));
        assertThat(store.survivesRestart(session), is(false));
        assertThat(store.survivesRestart("not-a-token"), is(false));
    }

    @Test
    public void testRevokeThatCouldNotBeWrittenReportsTheFailure() throws Exception
    {
        var store = newStore();
        var token = store.addPersistentClient("Claude Code");
        var id = store.listClients().get(0).id();
        assertThat(store.listClients().get(0).session(), is(false));

        Files.delete(dir().resolve("api-clients.json"));
        Files.createDirectories(dir().resolve("api-clients.json"));

        assertThat(store.revoke(id), is(false));
        assertThat(store.authenticate(token).isPresent(), is(false));
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

    /**
     * A client restricted by a later version must not authenticate
     * with full access here, and must survive this version rewriting the file.
     */
    @Test
    public void testRestrictedClientIsNotAuthenticatedButKept() throws Exception
    {
        var restricted = "{\"id\":\"r\",\"tokenHash\":\"h\",\"name\":\"Reader\","
                        + "\"created\":\"2026-07-20T10:00:00Z\",\"scopes\":[\"read\"]}";
        writeStore("{\"clients\":[" + restricted + "]}");

        var store = newStore();
        assertThat(store.listClients(), hasSize(0));

        // any save rewrites the file
        store.addPersistentClient("Other");

        var content = Files.readString(dir().resolve("api-clients.json"));
        assertThat(content.contains("\"Reader\""), is(true));
        assertThat(content.contains("\"scopes\":[\"read\"]"), is(true));
        assertThat(newStore().listClients(), hasSize(1));
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

    /**
     * A file whose shape this version does not recognise is not an empty file.
     * Reading it as one, and then saving what this version holds, would delete
     * tokens that still exist - so the content stays exactly as it is.
     */
    @Test
    public void testUnrecognisedContentIsNeverOverwritten() throws Exception
    {
        var foreign = "{\"tokens\":[{\"value\":\"a-token-a-later-version-understands\"}]}";
        writeStore(foreign);

        var store = newStore();
        store.addPersistentClient("Claude Code");

        assertThat(Files.readString(dir().resolve("api-clients.json")), is(foreign));
        assertThat(store.listClients(), hasSize(1)); // usable in this run
        assertThat(newStore().listClients(), hasSize(0)); // nothing was written over the old content
    }

    /** the same holds for content that is not parseable at all */
    @Test
    public void testUnparseableContentIsNeverOverwritten() throws Exception
    {
        writeStore("{ clients: [");

        newStore().addPersistentClient("Claude Code");

        assertThat(Files.readString(dir().resolve("api-clients.json")), is("{ clients: ["));
    }

    /**
     * A client file this version cannot read or write must not turn a token that
     * works into a failed request: authentication runs on an HTTP worker, after
     * the token has already been accepted.
     */
    @Test
    public void testAnUnwritableClientFileDoesNotBreakAuthentication() throws Exception
    {
        Files.createDirectories(dir().resolve("api-clients.json")); // a directory where a file belongs

        var store = newStore();
        var token = store.addPersistentClient("Claude Code");

        assertThat(store.authenticate(token).isPresent(), is(true));
        assertThat(store.authenticate(token).get().name(), is("Claude Code"));
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
