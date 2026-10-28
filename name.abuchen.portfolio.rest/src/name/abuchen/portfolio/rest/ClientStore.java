package name.abuchen.portfolio.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import name.abuchen.portfolio.PortfolioLog;

/**
 * Holds the API clients and their bearer tokens: persistent clients as an
 * owner-only JSON file in the plugin state location, session clients in memory
 * only. The file stores a SHA-256 hash of each token, never the plaintext -
 * the token exists only in the one-time response the client received.
 */
public class ClientStore
{
    /**
     * A granted client as shown in the UI. {@code id} is an opaque, stable
     * handle for revoking the client - deliberately independent of the token
     * and its hash, so the UI is not coupled to the hashing scheme.
     */
    public record ApiClient(String id, String name, Instant created, Instant lastUsed, boolean session)
    {
    }

    private static final class Entry
    {
        private final String id;
        private final String tokenHash;
        private final String name;
        private final Instant created;
        /** mutable: an entry that could not be written is a session client after all */
        private boolean session;
        private Instant lastUsed;
        private Instant lastUsedPersisted;

        private Entry(String id, String tokenHash, String name, Instant created, Instant lastUsed, boolean session)
        {
            this.id = id;
            this.tokenHash = tokenHash;
            this.name = name;
            this.created = created;
            this.lastUsed = lastUsed;
            this.lastUsedPersisted = lastUsed;
            this.session = session;
        }

        private ApiClient toApiClient()
        {
            return new ApiClient(id, name, created, lastUsed, session);
        }
    }

    private static final String FILE_NAME = "api-clients.json"; //$NON-NLS-1$

    /** the longest a client name may be, once sanitized */
    public static final int MAX_NAME_LENGTH = 64;

    /** successful authentications update the file at most this often */
    private static final Duration LAST_USED_WRITE_THROTTLE = Duration.ofMinutes(1);

    private final Path file;
    private final Supplier<Instant> clock;
    private final SecureRandom random = new SecureRandom();
    private final List<Entry> entries = new ArrayList<>();

    /**
     * Clients restricted by a later version, kept verbatim. This version cannot
     * enforce a restriction (e.g. read-only), so it does not authenticate them
     * rather than grant them full access - but it writes them back, so that
     * they still exist when the user returns to the later version. A client
     * with full access is therefore always written without "scopes".
     */
    private final List<JsonObject> restricted = new ArrayList<>();

    /**
     * Whether the file exists but this version cannot read it: not readable at
     * all, not valid JSON, or a shape it does not recognise. Such a file is never
     * saved over - an unreadable file is not an empty one, and a save would
     * discard clients the user still has.
     */
    private boolean unreadable;

    public ClientStore(Path directory)
    {
        this(directory, Instant::now);
    }

    public ClientStore(Path directory, Supplier<Instant> clock)
    {
        this.file = directory.resolve(FILE_NAME);
        this.clock = clock;
        load();
    }

    /** mints a token for a client surviving restarts; returns the plaintext token exactly once */
    public synchronized String addPersistentClient(String name)
    {
        return add(name, false);
    }

    /** mints a token valid until the application quits; returns the plaintext token exactly once */
    public synchronized String addSessionClient(String name)
    {
        return add(name, true);
    }

    private String add(String rawName, boolean session)
    {
        var name = sanitizeName(rawName);
        if (name.isEmpty())
            throw new IllegalArgumentException("client name must not be blank"); //$NON-NLS-1$

        var bytes = new byte[32];
        random.nextBytes(bytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        var entry = new Entry(UUID.randomUUID().toString(), hash(token), name, clock.get(), null, session);
        entries.add(entry);

        // persisted before the token is handed out: it exists exactly once. A write
        // that failed cannot outlive this run, so the entry is recorded as a session
        // client - the UI says session-only rather than implying a token that
        // survives a restart, which is the one claim it cannot make here.
        if (!session && !save())
        {
            entry.session = true;
            PortfolioLog.warning("Client \"" + name + "\" was added but not persisted; its token dies with " //$NON-NLS-1$ //$NON-NLS-2$
                            + "this application run"); //$NON-NLS-1$
        }
        return token;
    }

    /** validates a presented token and tracks its use; empty if the token is unknown or revoked */
    public synchronized Optional<ApiClient> authenticate(String token)
    {
        var presented = hash(token).getBytes(StandardCharsets.UTF_8);

        for (var entry : entries)
        {
            if (!MessageDigest.isEqual(presented, entry.tokenHash.getBytes(StandardCharsets.UTF_8)))
                continue;

            var now = clock.get();
            entry.lastUsed = now;
            if (!entry.session && (entry.lastUsedPersisted == null
                            || !now.isBefore(entry.lastUsedPersisted.plus(LAST_USED_WRITE_THROTTLE))))
            {
                entry.lastUsedPersisted = now;
                save();
            }
            return Optional.of(entry.toApiClient());
        }

        return Optional.empty();
    }

    public synchronized List<ApiClient> listClients()
    {
        return entries.stream().map(Entry::toApiClient).toList();
    }

    /**
     * Whether the client holding this token survives a restart: false for a
     * session client, and for a persistent one whose entry could not be written
     * to the file. Changes nothing - unlike {@link #authenticate}, it does not
     * count as a use of the token.
     */
    public synchronized boolean survivesRestart(String token)
    {
        var tokenHash = hash(token);
        return entries.stream().anyMatch(entry -> entry.tokenHash.equals(tokenHash) && !entry.session);
    }

    /**
     * Removes a client, answering whether the removal reached the file. False
     * means the entry is gone from memory while the file still holds its token
     * hash, so the token authenticates again after a restart - the caller has to
     * say that rather than presenting the client as revoked.
     */
    public synchronized boolean revoke(String id)
    {
        var removedPersistent = false;
        for (var iterator = entries.iterator(); iterator.hasNext();)
        {
            var entry = iterator.next();
            if (entry.id.equals(id))
            {
                removedPersistent |= !entry.session;
                iterator.remove();
            }
        }

        if (!removedPersistent)
            return true;

        if (!save())
        {
            PortfolioLog.warning("A revoked client is gone for this run only; its token still authenticates after a " //$NON-NLS-1$
                            + "restart because the client file could not be written"); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    /**
     * Strips control characters, invisible format characters and surrounding
     * whitespace; null becomes empty. The name is chosen by whoever asks for
     * access and shown to the user in the approval dialog, so a right-to-left
     * override or a zero-width character must not be able to make it read as
     * another client's name.
     */
    public static String sanitizeName(String name)
    {
        if (name == null)
            return ""; //$NON-NLS-1$
        return name.codePoints() //
                        .filter(cp -> !Character.isISOControl(cp)) //
                        .filter(cp -> !isInvisible(cp)) //
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append) //
                        .toString().strip();
    }

    /** bidi controls, zero-width characters and line or paragraph separators */
    private static boolean isInvisible(int codePoint)
    {
        var type = Character.getType(codePoint);
        return type == Character.FORMAT || type == Character.LINE_SEPARATOR
                        || type == Character.PARAGRAPH_SEPARATOR;
    }

    private static String hash(String token)
    {
        try
        {
            var digest = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
            return Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void load()
    {
        if (!Files.exists(file))
            return;

        String raw;
        try
        {
            raw = Files.readString(file, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            // Cannot read it at all - which is not the same as it being empty.
            // Nothing here may overwrite what we could not see.
            PortfolioLog.error(e);
            unreadable = true;
            return;
        }

        JsonElement root;
        try
        {
            root = JsonParser.parseString(raw);
        }
        catch (JsonSyntaxException e)
        {
            // not valid JSON at all: log, start empty, and keep the content so
            // that a save does not write over something the user still needs
            PortfolioLog.error(e);
            unreadable = true;
            return;
        }

        var clients = root.isJsonObject() ? root.getAsJsonObject().get("clients") : null; //$NON-NLS-1$
        if (clients == null || !clients.isJsonArray())
        {
            // a file this version does not recognise: not an empty one. Reading
            // it as empty and then saving would destroy tokens we never saw.
            PortfolioLog.warning("The API client file is not a client list this version understands; its content " //$NON-NLS-1$
                            + "is kept and will not be overwritten"); //$NON-NLS-1$
            unreadable = true;
            return;
        }

        // parse each entry defensively: a single malformed entry is skipped, not
        // fatal, so one bad record cannot revoke every other client
        for (var element : clients.getAsJsonArray())
        {
            if (element.isJsonObject() && element.getAsJsonObject().has("scopes")) //$NON-NLS-1$
            {
                restricted.add(element.getAsJsonObject());
                continue;
            }

            var entry = parseEntry(element);
            if (entry != null)
                entries.add(entry);
        }
    }

    /** parses one persisted entry; returns null (skip) if any required field is missing or malformed */
    private static Entry parseEntry(JsonElement element)
    {
        if (!element.isJsonObject())
            return null;
        var json = element.getAsJsonObject();

        var id = asString(json, "id"); //$NON-NLS-1$
        var tokenHash = asString(json, "tokenHash"); //$NON-NLS-1$
        var name = asString(json, "name"); //$NON-NLS-1$
        var created = asInstant(json, "created"); //$NON-NLS-1$
        if (id == null || tokenHash == null || name == null || created == null)
            return null;

        return new Entry(id, tokenHash, name, created, asInstant(json, "lastUsed"), false); //$NON-NLS-1$
    }

    /** the string member, or null if absent or not a string */
    private static String asString(JsonObject json, String member)
    {
        var element = json.get(member);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                        ? element.getAsString() : null;
    }

    /** the member parsed as an instant, or null if absent, not a string, or not a valid timestamp */
    private static Instant asInstant(JsonObject json, String member)
    {
        var raw = asString(json, member);
        if (raw == null)
            return null;
        try
        {
            return Instant.parse(raw);
        }
        catch (DateTimeParseException e)
        {
            return null;
        }
    }

    /**
     * Writes the persistent clients. Returns whether the file was written.
     * <p/>
     * A failure is logged and not thrown: {@link #authenticate} runs on an HTTP
     * worker after the token has already been accepted, and an unwritable file
     * must not turn a working request into a 500. Content this version could not
     * read is never written over.
     */
    private boolean save()
    {
        if (unreadable)
        {
            PortfolioLog.warning("Not writing " + FILE_NAME + ": its content is not a client list this version " //$NON-NLS-1$ //$NON-NLS-2$
                            + "understands, so saving would discard what is in it"); //$NON-NLS-1$
            return false;
        }

        var clients = new JsonArray();
        for (var entry : entries)
        {
            if (entry.session)
                continue;
            var json = new JsonObject();
            json.addProperty("id", entry.id); //$NON-NLS-1$
            json.addProperty("tokenHash", entry.tokenHash); //$NON-NLS-1$
            json.addProperty("name", entry.name); //$NON-NLS-1$
            json.addProperty("created", entry.created.toString()); //$NON-NLS-1$
            if (entry.lastUsed != null)
                json.addProperty("lastUsed", entry.lastUsed.toString()); //$NON-NLS-1$
            clients.add(json);
        }
        restricted.forEach(clients::add);
        var root = new JsonObject();
        root.add("clients", clients); //$NON-NLS-1$

        try
        {
            Files.createDirectories(file.getParent());

            // written next to the file and moved over it: a crash halfway
            // through leaves the previous file intact rather than a truncated
            // one, and the permissions are tightened before the content is
            // visible under the real name
            // a move replaces an empty directory where the file belongs, which
            // writing in place never did - that is not a file we may overwrite
            if (Files.isDirectory(file))
                throw new IOException(file + " is a directory"); //$NON-NLS-1$

            var temp = file.resolveSibling(file.getFileName() + ".tmp"); //$NON-NLS-1$
            try
            {
                Files.writeString(temp, root.toString(), StandardCharsets.UTF_8);
                restrictToOwner(temp);
                try
                {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException e)
                {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            finally
            {
                Files.deleteIfExists(temp);
            }
            return true;
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
            return false;
        }
    }

    private static void restrictToOwner(Path target) throws IOException
    {
        try
        {
            Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------")); //$NON-NLS-1$
        }
        catch (UnsupportedOperationException e)
        {
            // non-POSIX file system (Windows): rely on user profile ACLs
        }
    }
}
