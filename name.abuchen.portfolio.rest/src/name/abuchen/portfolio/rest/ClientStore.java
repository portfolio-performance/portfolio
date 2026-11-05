package name.abuchen.portfolio.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
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
import com.google.gson.JsonParseException;
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
        /**
         * A persistent entry whose save failed while the file could not be read:
         * it may or may not be in the file, so it is not reported as surviving a
         * restart until a save works.
         */
        private boolean unwritten;
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
            return new ApiClient(id, name, created, lastUsed, session || unwritten);
        }
    }

    private static final String FILE_NAME = "api-clients.json"; //$NON-NLS-1$

    /** how many links in a row are followed to the client file, as a file system would */
    private static final int MAX_LINK_HOPS = 40;

    /** the longest a client name may be, once sanitized */
    public static final int MAX_NAME_LENGTH = 64;

    /**
     * successful authentications update the file at most this often, and after
     * a write that failed, the next is tried no sooner than this
     */
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
     * Entries this version cannot parse - written by a later version, or
     * damaged. Never authenticated, but written back as they were, so that a
     * save does not delete what a later version may still read.
     */
    private final List<JsonElement> unparsed = new ArrayList<>();

    /**
     * Whether the file exists but this version cannot read it: not readable at
     * all, not valid JSON, or a shape it does not recognise. Such a file is never
     * saved over - an unreadable file is not an empty one, and a save would
     * discard clients the user still has.
     */
    private boolean unreadable;

    /**
     * Whether the file still holds a client that was revoked in memory, because
     * the write failed - or may lack a client added since. Any later write that works removes it; until then an
     * authentication - also one with that client's token - tries again, once a
     * minute at most, so that a lasting failure does not log every request.
     */
    private boolean unsaved;

    /** when the last write failed; null after one that worked */
    private Instant lastFailedSave;

    public ClientStore(Path directory)
    {
        this(directory, Instant::now);
    }

    public ClientStore(Path directory, Supplier<Instant> clock)
    {
        // absolute, so that the file always has a parent to create
        this.file = directory.toAbsolutePath().resolve(FILE_NAME);
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
        // survives a restart, which is the one claim it cannot make here. A
        // failure reported after the content reached the file is not one: what
        // the file holds decides.
        if (!session && !save())
        {
            // a file protected as unreadable since it was loaded is never
            // written, so it cannot hold the new token
            var holds = unreadable ? Holds.NO : fileHolds(entry.tokenHash);
            switch (holds)
            {
                case YES -> PortfolioLog.warning("Saving client \"" + name //$NON-NLS-1$
                                + "\" reported an error, but the client file holds it"); //$NON-NLS-1$
                case UNKNOWN -> {
                    // the file may hold the token: kept as persistent, so that
                    // a revoke writes the file rather than taking a session
                    // client's shortcut, and written again with a later request.
                    // Until then it is not reported as surviving a restart.
                    entry.unwritten = true;
                    unsaved = true;
                    PortfolioLog.warning("Saving client \"" + name //$NON-NLS-1$
                                    + "\" reported an error and the client file cannot be read; it is kept as " //$NON-NLS-1$
                                    + "persistent and written again with a later request"); //$NON-NLS-1$
                }
                default -> {
                    // NO: the file does not hold it, so it dies with this run.
                    // A write that failed halfway may also have cut the earlier
                    // clients from the file: a later request writes it again -
                    // unless the file is protected, which is never written
                    entry.session = true;
                    if (!unreadable)
                        unsaved = true;
                    PortfolioLog.warning("Client \"" + name + "\" was added but not persisted; its token dies with " //$NON-NLS-1$ //$NON-NLS-2$
                                    + "this application run"); //$NON-NLS-1$
                }
            }
        }
        return token;
    }

    /** what the client file says about a token hash */
    private enum Holds
    {
        YES, NO,
        /** the file cannot be read now; a later read - the next start's - may find the hash */
        UNKNOWN
    }

    /**
     * Whether the client file, as it is now, lists a client with this token
     * hash - as the next start would read it. A file cut short by a write that
     * failed halfway does not, even if the hash made it into the file; nor
     * does a missing file or a directory in its place. A file that exists but
     * cannot be read is no proof either way.
     */
    private Holds fileHolds(String tokenHash)
    {
        // read directly: Files.exists also answers false when it cannot tell
        if (Files.isDirectory(file))
            return Holds.NO;

        String content;
        try
        {
            content = Files.readString(file, StandardCharsets.UTF_8);
        }
        catch (NoSuchFileException e)
        {
            return Holds.NO;
        }
        catch (IOException e)
        {
            return Holds.UNKNOWN;
        }

        try
        {
            var root = JsonParser.parseString(content);
            var clients = root.isJsonObject() ? root.getAsJsonObject().get("clients") : null; //$NON-NLS-1$
            if (clients == null || !clients.isJsonArray())
                return Holds.NO;
            for (var element : clients.getAsJsonArray())
            {
                if (element.isJsonObject() && tokenHash.equals(asString(element.getAsJsonObject(), "tokenHash"))) //$NON-NLS-1$
                    return Holds.YES;
            }
            return Holds.NO;
        }
        catch (JsonParseException e)
        {
            return Holds.NO;
        }
    }

    /** validates a presented token and tracks its use; empty if the token is unknown or revoked */
    public synchronized Optional<ApiClient> authenticate(String token)
    {
        var presented = hash(token).getBytes(StandardCharsets.UTF_8);
        var now = clock.get();

        for (var entry : entries)
        {
            if (!MessageDigest.isEqual(presented, entry.tokenHash.getBytes(StandardCharsets.UTF_8)))
                continue;

            entry.lastUsed = now;
            var lastUsedDue = !entry.session && due(entry.lastUsedPersisted, now);
            // the time is taken as written only when the write worked; a failed
            // one is tried again with a later use
            if ((lastUsedDue || unsaved) && retryDue(now) && save() && !entry.session)
                entry.lastUsedPersisted = now;
            return Optional.of(entry.toApiClient());
        }

        // no client holds the token - perhaps one revoked while the file could
        // not be written, which still lists it
        if (unsaved && retryDue(now))
            save();

        return Optional.empty();
    }

    private boolean retryDue(Instant now)
    {
        return due(lastFailedSave, now);
    }

    /**
     * Whether a minute has passed since - or the clock was set back behind it,
     * which would otherwise hold off the write until it caught up again.
     */
    private static boolean due(Instant since, Instant now)
    {
        return since == null || now.isBefore(since) || !now.isBefore(since.plus(LAST_USED_WRITE_THROTTLE));
    }

    public synchronized List<ApiClient> listClients()
    {
        return entries.stream().map(Entry::toApiClient).toList();
    }

    /**
     * Whether the client holding this token is confirmed to survive a restart:
     * false for a session client, and for a persistent one whose entry is not
     * known to be in the file - which may still be there. Changes nothing - unlike {@link #authenticate}, it does not
     * count as a use of the token.
     */
    public synchronized boolean survivesRestart(String token)
    {
        var tokenHash = hash(token);
        return entries.stream()
                        .anyMatch(entry -> entry.tokenHash.equals(tokenHash) && !entry.session && !entry.unwritten);
    }

    /**
     * Removes a client, answering whether the removal reached the file. False
     * means the entry is gone from memory but the change was not written. The
     * file may still hold its token hash, and then the token authenticates
     * again after a restart unless a later write works - the caller has to say
     * that rather than presenting the client as revoked. The write is tried
     * again with later authentications.
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
            unsaved = true;
            PortfolioLog.warning("A revoked client is gone for this run only; its token may still authenticate after a " //$NON-NLS-1$
                            + "restart unless a later write of the client file works"); //$NON-NLS-1$
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
        // notExists, not !exists: a file whose existence cannot be determined
        // is read, fails, and is then protected as unreadable
        if (Files.notExists(file))
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

        // an empty file holds no tokens, so there is nothing a save could
        // destroy - an interrupted write of an older version can leave one
        if (raw.isBlank())
            return;

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
            else
                unparsed.add(element);
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
        var saved = write();
        if (saved)
        {
            unsaved = false;
            lastFailedSave = null;
            entries.forEach(entry -> entry.unwritten = false);
        }
        else
        {
            lastFailedSave = clock.get();
        }
        return saved;
    }

    private boolean write()
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
        unparsed.forEach(clients::add);
        var root = new JsonObject();
        root.add("clients", clients); //$NON-NLS-1$

        try
        {
            var target = target();
            Files.createDirectories(target.getParent());

            // a move replaces an empty directory where the file belongs, which
            // writing in place never did - that is not a file we may overwrite
            if (Files.isDirectory(target))
                throw new IOException(target + " is a directory"); //$NON-NLS-1$

            var content = root.toString();

            // a file that is not this name's alone is written in place, as
            // before: the target of a link keeps its own permissions, and a
            // file with further hard links shows the new content under each
            if (Files.exists(target) && (!target.equals(file) || hasOtherNames(target)))
            {
                writeInPlace(target, content);
                return true;
            }

            // otherwise written next to the file and moved over it: a crash
            // halfway through leaves the previous file intact rather than a
            // truncated one, and the permissions are tightened before the
            // content is visible under the real name. A fresh name, created
            // exclusively - on POSIX already owner-only - in the same
            // directory, so that the move stays on one file system.
            Path temp;
            try
            {
                temp = Files.createTempFile(target.getParent(), FILE_NAME, ".tmp"); //$NON-NLS-1$
            }
            catch (AccessDeniedException e)
            {
                // a directory that takes no new file can still hold a client
                // file that may be written. Only a refusal, though: any other
                // failure - a full disk, say - would hit the in-place write as
                // well, and that one could leave the file truncated.
                if (!Files.isWritable(target))
                    throw e;
                writeInPlace(target, content);
                return true;
            }

            try
            {
                // a file whose ACL cannot be carried over is written in place
                // rather than replaced by one that is readable more widely. The
                // ACL comes first, so the content is never in a file that is.
                if (!carryOverAcl(target, temp))
                {
                    writeInPlace(target, content);
                    return true;
                }

                Files.writeString(temp, content, StandardCharsets.UTF_8);
                restrictToOwner(temp);

                try
                {
                    replace(temp, target);
                }
                catch (FileSystemException e)
                {
                    // a file that may not be replaced - on Windows one another
                    // program holds open, on POSIX one in a sticky directory
                    // owned by someone else - can still be one that may be
                    // written, as before. The temporary file goes first: the
                    // space it frees is at least what the write needs, so a
                    // nearly full disk does not leave the file cut short.
                    if (e instanceof NoSuchFileException || !Files.isWritable(target))
                        throw e;
                    deleteQuietly(temp);
                    writeInPlace(target, content);
                }
            }
            finally
            {
                deleteQuietly(temp);
            }
            return true;
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
            return false;
        }
    }

    private static void replace(Path temp, Path target) throws IOException
    {
        try
        {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * The file a write goes to: the client file, or the file it links to - also
     * one that does not exist yet. A move replaces a link rather than what it
     * points at, which would leave the linked file with the clients it had -
     * revoked ones included - so a link is written through.
     */
    private Path target() throws IOException
    {
        var path = file;
        for (var hops = 0; Files.isSymbolicLink(path); hops++)
        {
            if (hops == MAX_LINK_HOPS)
                throw new IOException(file + ": too many levels of symbolic links"); //$NON-NLS-1$
            // a relative link counts from the directory that holds it
            path = path.resolveSibling(Files.readSymbolicLink(path));
        }
        return path;
    }

    /**
     * Writes over the existing file, keeping every name it has - and on a file
     * system without POSIX permissions its ACL. Not atomic - a crash halfway through can leave it truncated - so it
     * is used only where replacing the file would lose more.
     */
    private static void writeInPlace(Path target, String content) throws IOException
    {
        Files.writeString(target, content, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);

        // owner-only on POSIX, as a replaced file is; elsewhere its ACL stays.
        // The content is written by now, so a file whose permissions are not
        // ours to change - a link target owned by someone else - is logged,
        // not reported as a failed save
        try
        {
            restrictToOwner(target);
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }
    }

    /**
     * Without POSIX permissions the ACL is what keeps the client file private,
     * and a move would swap it for the one the new file inherited from the
     * directory. Copies the ACL of the existing file over to the new one;
     * false where that fails. Nothing to do on POSIX, for a file that does not
     * exist yet, or where the file system has no ACLs.
     */
    private static boolean carryOverAcl(Path existing, Path replacement)
    {
        // notExists: a file whose existence cannot be told still gets its ACL carried over
        if (Files.notExists(existing) || Files.getFileAttributeView(existing, PosixFileAttributeView.class) != null)
            return true;

        var from = Files.getFileAttributeView(existing, AclFileAttributeView.class);
        if (from == null)
            return true;

        var to = Files.getFileAttributeView(replacement, AclFileAttributeView.class);
        try
        {
            if (to == null)
                return false;
            to.setAcl(from.getAcl());
            return true;
        }
        catch (IOException | UnsupportedOperationException | SecurityException e)
        {
            PortfolioLog.warning("The ACL of the client file cannot be carried over, it is written in place: " //$NON-NLS-1$
                            + e);
            return false;
        }
    }

    /**
     * Whether the file has hard links besides this name; false where the file
     * system does not say - Windows has no way to tell, so a file with further
     * names is replaced there (documented in the README).
     */
    private static boolean hasOtherNames(Path path)
    {
        try
        {
            return ((Number) Files.getAttribute(path, "unix:nlink")).intValue() > 1; //$NON-NLS-1$
        }
        catch (UnsupportedOperationException | IllegalArgumentException | IOException e)
        {
            return false;
        }
    }

    /**
     * Removes a temporary file that is left over only when the move failed.
     * Failing to remove it is logged, not reported: whether the save worked is
     * decided by the move alone.
     */
    private static void deleteQuietly(Path temp)
    {
        try
        {
            Files.deleteIfExists(temp);
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
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
