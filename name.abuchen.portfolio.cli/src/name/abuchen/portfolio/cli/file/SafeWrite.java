package name.abuchen.portfolio.cli.file;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.cli.command.WarningSpec;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/**
 * Changes a client file the safe way: lock, load, change, back up, write a
 * temp file and rename it over the original. Any failure before the rename
 * leaves the client file untouched. A dry run stops before any side effect.
 */
public final class SafeWrite
{
    private static final String BACKUP_FOLDER = "backups";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    static final OptionSpec BACKUP_DIR = new OptionSpec("backup-dir", "path",
                    "Folder for the backup of the original file. Default: a backups folder next to the client file. Created if missing.");
    static final OptionSpec KEEP = OptionSpec.integer("keep", "n",
                    "How many of the newest pp-cli backups of this file to keep in the backup folder; older ones are deleted after a successful write. 0 keeps all. Only backups named like the ones pp-cli writes are ever deleted; the app's own .backup file is never touched.",
                    20, 0, (long) Integer.MAX_VALUE);

    static final OptionSpec FORCE = OptionSpec.flag("force",
                    "Write even if the file seems to be open in another program. Only macOS can tell: pp-cli asks lsof, and without --force refuses a file that is open.");

    static final WarningSpec OPEN_IN_APP = new WarningSpec("file-open-in-app",
                    "The file seemed to be open in another program, probably the app, and was written because of --force.");

    static final WarningSpec MIGRATED = new WarningSpec("file-migrated",
                    "The file was written by an older version and is saved at the current file version, which an older desktop app can no longer open. With --dry-run this is what a write would do.");

    /** The options every command that writes the client file declares. */
    public static final List<OptionSpec> OPTIONS = List.of(BACKUP_DIR, KEEP, FORCE);

    /** The warnings every command that writes the client file can emit. */
    public static final List<WarningSpec> WARNINGS = List.of(MIGRATED, ConsistencyCheck.PRE_EXISTING, OPEN_IN_APP);

    /**
     * @param backupDir
     *            where backups go, or null for the default
     * @param keep
     *            how many backups to keep; 0 for all
     * @param force
     *            write even if the file seems to be open in the app
     */
    public record Settings(Clock clock, boolean dryRun, Path backupDir, int keep, boolean force, String osName,
                    WriteEnvironment environment)
    {
    }

    /** Reads the options of the write sequence; <code>dryRun</code> is the command's own switch. */
    public static Settings settings(CommandContext context, boolean dryRun) throws CliException
    {
        Path backupDir = null;
        var raw = context.text(BACKUP_DIR);
        if (raw.isPresent())
        {
            try
            {
                if (raw.get().isBlank())
                    throw BACKUP_DIR.invalid(raw.get(), "a folder path");
                backupDir = Path.of(raw.get()).toAbsolutePath();
            }
            catch (InvalidPathException e)
            {
                throw BACKUP_DIR.invalid(raw.get(), "a valid folder path");
            }
        }

        var keep = (int) context.integer(KEEP);
        return new Settings(context.clock(), dryRun, backupDir, keep, context.flag(FORCE), context.osName(),
                        context.writeEnvironment());
    }

    /** The change to make to the loaded model; it fails with a {@link CliException} to refuse the write. */
    @FunctionalInterface
    public interface Mutation<R>
    {
        R apply(Client client) throws CliException;
    }

    /**
     * Decides what the client file becomes: the loaded model, changed, or
     * another model altogether.
     */
    @FunctionalInterface
    public interface Plan<R>
    {
        Planned<R> apply(Client loaded) throws CliException;
    }

    /**
     * @param client
     *            the model to write
     * @param baseline
     *            the file whose consistency issues are the ones the write may
     *            not add to: the file as it was, or the file restored from
     */
    public record Planned<R>(Client client, R result, File baseline)
    {
    }

    /**
     * @param backup
     *            the backup that was written, or that a real write would
     *            write
     * @param warnings
     *            conditions that didn't stop the write
     */
    public record Outcome<R>(R result, Path backup, int fileVersionBefore, int fileVersionAfter, boolean dryRun,
                    List<Warning> warnings)
    {
    }

    /** What identifies the state of a file on disk: any difference means someone else wrote it. */
    private record Fingerprint(long size, FileTime modified, String hash)
    {
        static Fingerprint of(Path file) throws IOException
        {
            try
            {
                var digest = MessageDigest.getInstance("SHA-256");
                try (var input = new DigestInputStream(Files.newInputStream(file), digest))
                {
                    input.transferTo(OutputStream.nullOutputStream());
                }
                return new Fingerprint(Files.size(file), Files.getLastModifiedTime(file),
                                HexFormat.of().formatHex(digest.digest()));
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new IllegalStateException(e);
            }
        }
    }

    private SafeWrite()
    {
    }

    /**
     * @param opened
     *            a file that passed the version guard
     */
    public static <R> Outcome<R> run(ClientLoader.OpenedFile opened, Settings settings, Mutation<R> change)
                    throws CliException
    {
        return replace(opened, settings, client -> new Planned<>(client, change.apply(client), opened.file()));
    }

    /**
     * Like {@link #run}, but the plan may name a different model to write
     * than the one loaded.
     *
     * @param opened
     *            a file that passed the version guard
     */
    public static <R> Outcome<R> replace(ClientLoader.OpenedFile opened, Settings settings, Plan<R> plan)
                    throws CliException
    {
        var file = opened.file().toPath().toAbsolutePath();

        if (settings.dryRun())
        {
            var loaded = opened.load();
            var planned = plan.apply(loaded.client());

            // nothing is written, so the planned model itself stands in for the written file
            var warnings = new ArrayList<>(migrationWarnings(planned.client()));
            warnings.addAll(ConsistencyCheck.verify(ConsistencyCheck.run(opened.load(planned.baseline()).client()),
                            ConsistencyCheck.run(planned.client())));
            return new Outcome<>(planned.result(), backupPath(file, settings),
                            loaded.client().getFileVersionAfterRead(), Client.CURRENT_VERSION, true, warnings);
        }

        try (var lock = Lock.acquire(file))
        {
            var warnings = new ArrayList<Warning>();
            checkNotOpenInApp(file, settings, warnings);

            Fingerprint fingerprint;
            try
            {
                fingerprint = Fingerprint.of(file);
            }
            catch (IOException e)
            {
                throw writeFailed(e);
            }

            var loaded = opened.load();
            var planned = plan.apply(loaded.client());
            warnings.addAll(migrationWarnings(planned.client()));

            var backup = backupPath(file, settings);
            var temp = sibling(file, ".", ".pp-cli-tmp");
            var backupFolderExisted = Files.isDirectory(backup.getParent());
            var committed = false;
            var backedUp = false;
            var moveAttempted = false;
            try
            {
                ClientFactory.save(planned.client(), temp.toFile());

                // the file as it would be read back, against the file as it was; both are scratch copies
                warnings.addAll(ConsistencyCheck.verify(ConsistencyCheck.run(opened.load(planned.baseline()).client()),
                                ConsistencyCheck.run(readBack(opened, temp))));

                Files.createDirectories(backup.getParent());
                Files.copy(file, backup);
                backedUp = true;

                settings.environment().beforeRecheck(file);
                if (!fingerprint.equals(Fingerprint.of(file)))
                    throw CliException.failure(ErrorCode.FILE_CHANGED, "Client file changed while it was being written",
                                    "The client file was changed by someone else after it was read, for example"
                                                    + " by a save in the app. Nothing was written. Run the command again.",
                                    409).with("file", file.toString());

                moveAttempted = true;
                move(settings.environment(), temp, file);
                committed = true;
            }
            catch (IOException | RuntimeException e)
            {
                throw writeFailed(e);
            }
            finally
            {
                deleteQuietly(temp);
                if (!committed)
                {
                    // once a move was tried, the backup may be the only intact copy
                    if (backedUp && !moveAttempted)
                        deleteQuietly(backup);
                    if (!backupFolderExisted)
                        deleteQuietly(backup.getParent()); // only succeeds while empty
                }
            }

            prune(file, backup.getParent(), settings.keep());

            return new Outcome<>(planned.result(), backup, loaded.client().getFileVersionAfterRead(),
                            Client.CURRENT_VERSION, false, warnings);
        }
    }

    private static CliException writeFailed(Exception e)
    {
        return CliException.failure(ErrorCode.WRITE_FAILED, "Client file could not be written",
                        "The client file was not changed: " + e.getMessage(), 500, e);
    }

    /** The warning for a file that is saved at a newer version than it was read at. */
    private static List<Warning> migrationWarnings(Client client)
    {
        var before = client.getFileVersionAfterRead();
        if (before >= Client.CURRENT_VERSION)
            return List.of();

        var detail = "The file has version " + before + " and is saved at version " + Client.CURRENT_VERSION
                        + ", which an older desktop app can no longer open.";
        return List.of(new Warning(MIGRATED.code(), detail).with("fileVersionBefore", before)
                        .with("fileVersionAfter", Client.CURRENT_VERSION));
    }

    /** Loads what was written, to check it is what the model says. */
    private static Client readBack(ClientLoader.OpenedFile opened, Path temp) throws CliException
    {
        try
        {
            return opened.load(temp.toFile()).client();
        }
        catch (CliException e)
        {
            throw writeFailed(new IOException("the written file could not be read back: " + e.getMessage(), e));
        }
    }

    /** An atomic move, and only if the file system doesn't support one, a plain replace. */
    private static void move(WriteEnvironment environment, Path temp, Path file) throws IOException
    {
        try
        {
            environment.move(temp, file, true);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            environment.move(temp, file, false);
        }
    }

    /**
     * The probe only exists on macOS. A file the app has open is refused
     * unless forced, because the app would overwrite the change with its own
     * save.
     */
    private static void checkNotOpenInApp(Path file, Settings settings, List<Warning> warnings) throws CliException
    {
        if (!settings.osName().toLowerCase(Locale.ROOT).contains("mac") || !settings.environment().isOpenInApp(file))
            return;

        if (!settings.force())
            throw CliException.failure(ErrorCode.FILE_OPEN_IN_APP, "Client file is open in another program",
                            "Another process has the file open, probably the Portfolio Performance app. Close it there first, or pass --force to write anyway; unsaved changes in the app would then overwrite this change when saved.",
                            409).with("file", file.toString());

        warnings.add(new Warning(OPEN_IN_APP.code(),
                        "Another process has the file open, probably the Portfolio Performance app. Written anyway because of --force.")
                                        .with("file", file.toString()));
    }

    /**
     * <code>backups/&lt;name&gt;.&lt;yyyyMMdd-HHmmss&gt;.pp-cli&lt;ext&gt;</code>
     * next to the client file, or in the backup folder given. Another backup
     * within the same second gets a counter, so no backup is ever overwritten.
     */
    private static Path backupPath(Path file, Settings settings)
    {
        var name = BackupName.of(file);
        var timestamp = TIMESTAMP.format(LocalDateTime.now(settings.clock()));

        var folder = settings.backupDir() != null ? settings.backupDir() : file.resolveSibling(BACKUP_FOLDER);
        var backup = folder.resolve(name.stem() + "." + timestamp + ".pp-cli" + name.extension());
        for (int counter = 2; Files.exists(backup); counter++)
            backup = folder.resolve(name.stem() + "." + timestamp + "-" + counter + ".pp-cli" + name.extension());

        return backup;
    }

    /**
     * Deletes the oldest pp-cli backups of this client file beyond
     * <code>keep</code>. Age is read from the timestamp in the name, never from
     * the file system. Nothing else in the folder is touched, and a failure to
     * delete is no reason to fail a write that succeeded.
     */
    private static void prune(Path file, Path folder, int keep)
    {
        if (keep == 0)
            return;

        var name = BackupName.of(file);
        var pattern = Pattern.compile(Pattern.quote(name.stem()) + "\\.(\\d{8}-\\d{6})(?:-(\\d+))?\\.pp-cli"
                        + Pattern.quote(name.extension()));

        record Candidate(Path path, String timestamp, int counter)
        {
        }

        var candidates = new ArrayList<Candidate>();
        try (var entries = Files.list(folder))
        {
            entries.forEach(path -> {
                var matcher = pattern.matcher(path.getFileName().toString());
                if (matcher.matches() && Files.isRegularFile(path))
                    candidates.add(new Candidate(path, matcher.group(1),
                                    matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2))));
            });
        }
        catch (IOException | RuntimeException e)
        {
            return;
        }

        candidates.sort(Comparator.comparing(Candidate::timestamp).thenComparingInt(Candidate::counter));
        for (var oldest : candidates.subList(0, Math.max(0, candidates.size() - keep)))
            deleteQuietly(oldest.path());
    }

    private record BackupName(String stem, String extension)
    {
        static BackupName of(Path file)
        {
            var name = file.getFileName().toString();
            var dot = name.lastIndexOf('.');
            return new BackupName(dot > 0 ? name.substring(0, dot) : name, dot > 0 ? name.substring(dot) : "");
        }
    }

    private static Path sibling(Path file, String prefix, String suffix)
    {
        return file.resolveSibling(prefix + file.getFileName() + suffix);
    }

    private static void deleteQuietly(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException e)
        {
            // nothing more to do; the next write overwrites a left-over file
        }
    }

    /** An exclusive lock file holding the PID of the writer. Reads never take it. */
    private static final class Lock implements AutoCloseable
    {
        private final Path path;

        private Lock(Path path)
        {
            this.path = path;
        }

        static Lock acquire(Path file) throws CliException
        {
            var path = sibling(file, ".", ".pp-cli.lock");
            try
            {
                Files.createFile(path);
            }
            catch (FileAlreadyExistsException e)
            {
                throw locked(path);
            }
            catch (IOException e)
            {
                throw CliException.failure(ErrorCode.WRITE_FAILED, "Client file could not be locked",
                                "The lock file " + path + " could not be created: " + e.getMessage(), 500, e);
            }

            var lock = new Lock(path);
            try
            {
                Files.writeString(path, Long.toString(ProcessHandle.current().pid()), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                lock.close();
                throw CliException.failure(ErrorCode.WRITE_FAILED, "Client file could not be locked",
                                "The lock file " + path + " could not be written: " + e.getMessage(), 500, e);
            }
            return lock;
        }

        private static CliException locked(Path path)
        {
            String holder;
            try
            {
                holder = Files.readString(path, StandardCharsets.UTF_8).trim();
            }
            catch (IOException e)
            {
                holder = "";
            }

            var problem = CliException.failure(ErrorCode.FILE_LOCKED, "Client file is locked",
                            "Another pp-cli is writing this file, or crashed while doing so. Lock file: " + path
                                            + (holder.isEmpty() ? "" : " (PID " + holder + ")")
                                            + ". Delete it if no other pp-cli is running.",
                            409).with("lockFile", path.toString());
            if (!holder.isEmpty())
                problem.with("pid", holder);
            return problem;
        }

        @Override
        public void close()
        {
            deleteQuietly(path);
        }
    }
}
