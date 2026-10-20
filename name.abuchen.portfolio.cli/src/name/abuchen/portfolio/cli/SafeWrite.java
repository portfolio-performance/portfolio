package name.abuchen.portfolio.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/**
 * Changes a client file the safe way: lock, load, change, back up, write a
 * temp file and rename it over the original. Any failure before the rename
 * leaves the client file untouched. A dry run stops before any side effect.
 */
final class SafeWrite
{
    private static final String BACKUP_FOLDER = "backups";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** The change to make to the loaded model; it fails with a {@link CliException} to refuse the write. */
    @FunctionalInterface
    interface Change<R>
    {
        R apply(Client client) throws CliException;
    }

    /**
     * @param backup
     *            the backup that was written, or that a real write would
     *            write
     */
    record Outcome<R>(R result, Path backup, int fileVersionBefore, int fileVersionAfter, boolean dryRun)
    {
    }

    private SafeWrite()
    {
    }

    /**
     * @param opened
     *            a file that passed the version guard
     */
    static <R> Outcome<R> run(ClientLoader.OpenedFile opened, Clock clock, boolean dryRun, Change<R> change)
                    throws CliException
    {
        var file = opened.file().toPath().toAbsolutePath();

        if (dryRun)
        {
            var loaded = opened.load();
            var result = change.apply(loaded.client());
            return new Outcome<>(result, backupPath(file, clock), loaded.client().getFileVersionAfterRead(),
                            Client.CURRENT_VERSION, true);
        }

        try (var lock = Lock.acquire(file))
        {
            var loaded = opened.load();
            var client = loaded.client();
            var result = change.apply(client);

            var backup = backupPath(file, clock);
            var temp = sibling(file, ".", ".pp-cli-tmp");
            try
            {
                Files.createDirectories(backup.getParent());
                Files.copy(file, backup);

                ClientFactory.save(client, temp.toFile());
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException | RuntimeException e)
            {
                throw CliException.failure("write-failed", "Client file could not be written",
                                "The client file was not changed: " + e.getMessage(), 500, e);
            }
            finally
            {
                deleteQuietly(temp);
            }

            return new Outcome<>(result, backup, client.getFileVersionAfterRead(), Client.CURRENT_VERSION, false);
        }
    }

    /**
     * <code>backups/&lt;name&gt;.&lt;yyyyMMdd-HHmmss&gt;.pp-cli&lt;ext&gt;</code>
     * next to the client file. Another backup within the same second gets a
     * counter, so no backup is ever overwritten.
     */
    private static Path backupPath(Path file, Clock clock)
    {
        var name = file.getFileName().toString();
        var dot = name.lastIndexOf('.');
        var stem = dot > 0 ? name.substring(0, dot) : name;
        var extension = dot > 0 ? name.substring(dot) : "";
        var timestamp = TIMESTAMP.format(LocalDateTime.now(clock));

        var folder = file.resolveSibling(BACKUP_FOLDER);
        var backup = folder.resolve(stem + "." + timestamp + ".pp-cli" + extension);
        for (int counter = 2; Files.exists(backup); counter++)
            backup = folder.resolve(stem + "." + timestamp + "-" + counter + ".pp-cli" + extension);

        return backup;
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
                throw CliException.failure("write-failed", "Client file could not be locked",
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
                throw CliException.failure("write-failed", "Client file could not be locked",
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

            var problem = CliException.failure("file-locked", "Client file is locked",
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
