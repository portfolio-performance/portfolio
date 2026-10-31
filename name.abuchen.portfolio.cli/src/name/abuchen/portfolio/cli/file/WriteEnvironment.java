package name.abuchen.portfolio.cli.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * What the safe-write sequence needs from the file system and from other
 * processes beyond plain reads and writes. The real implementation is
 * {@link #SYSTEM}; tests substitute one to reach failures that arguments and
 * files can't produce.
 */
public interface WriteEnvironment
{
    WriteEnvironment SYSTEM = new WriteEnvironment()
    {
    };

    /**
     * Moves the temp file over the client file.
     *
     * @param atomic
     *            true for an atomic move, which may throw
     *            {@link java.nio.file.AtomicMoveNotSupportedException}; false
     *            for a plain replace
     */
    default void move(Path source, Path target, boolean atomic) throws IOException
    {
        if (atomic)
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        else
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Whether another process, presumably the desktop app, has the file open. Best effort. */
    default boolean isOpenInApp(Path file)
    {
        try
        {
            var lsof = new ProcessBuilder("lsof", "-t", "--", file.toString())
                            .redirectError(ProcessBuilder.Redirect.DISCARD).start();

            // -t prints PIDs only, far less than a pipe buffer, so waiting before reading can't block lsof
            if (!lsof.waitFor(5, TimeUnit.SECONDS))
            {
                lsof.destroyForcibly();
                return false;
            }

            var output = new String(lsof.getInputStream().readAllBytes());
            var self = Long.toString(ProcessHandle.current().pid());
            return output.lines().map(String::trim).anyMatch(pid -> !pid.isEmpty() && !pid.equals(self));
        }
        catch (IOException e)
        {
            return false;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Called after the backup is written and right before the client file is
     * checked for changes and replaced. A seam for tests that change the file
     * in that window.
     */
    default void beforeRecheck(Path file)
    {
        // nothing happens in between in real life
    }
}
