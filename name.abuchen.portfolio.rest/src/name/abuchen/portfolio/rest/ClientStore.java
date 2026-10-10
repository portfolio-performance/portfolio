package name.abuchen.portfolio.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        private final boolean session;
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

        entries.add(new Entry(UUID.randomUUID().toString(), hash(token), name, clock.get(), null, session));
        if (!session)
            save();
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

    public synchronized void revoke(String id)
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
        if (removedPersistent)
            save();
    }

    /** strips control characters and surrounding whitespace; null becomes empty */
    public static String sanitizeName(String name)
    {
        if (name == null)
            return ""; //$NON-NLS-1$
        return name.codePoints() //
                        .filter(cp -> !Character.isISOControl(cp)) //
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append) //
                        .toString().strip();
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

        JsonElement root;
        try
        {
            root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (JsonSyntaxException e)
        {
            // not valid JSON at all: log and start empty rather than leaving the
            // API dead, but do not swallow it silently
            PortfolioLog.error(e);
            return;
        }

        if (!root.isJsonObject())
            return;
        var clients = root.getAsJsonObject().get("clients"); //$NON-NLS-1$
        if (clients == null || !clients.isJsonArray())
            return;

        // parse each entry defensively: a single malformed entry is skipped, not
        // fatal, so one bad record cannot revoke every other client
        for (var element : clients.getAsJsonArray())
        {
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

    private void save()
    {
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
        var root = new JsonObject();
        root.add("clients", clients); //$NON-NLS-1$

        try
        {
            Files.createDirectories(file.getParent());
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
            restrictToOwner();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void restrictToOwner() throws IOException
    {
        try
        {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")); //$NON-NLS-1$
        }
        catch (UnsupportedOperationException e)
        {
            // non-POSIX file system (Windows): rely on user profile ACLs
        }
    }
}
