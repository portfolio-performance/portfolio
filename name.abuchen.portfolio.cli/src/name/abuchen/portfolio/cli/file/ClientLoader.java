package name.abuchen.portfolio.cli.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.eclipse.core.runtime.NullProgressMonitor;

import name.abuchen.portfolio.cli.CliException;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/**
 * Loads a client file through core. Every load runs the version guard, for
 * every command.
 */
public final class ClientLoader
{
    private ClientLoader()
    {
    }

    /**
     * A client file that passed the version guard but is not loaded yet, so a
     * writer can take its lock in between. Holds the password until closed.
     */
    public static final class OpenedFile implements AutoCloseable
    {
        private final File file;
        private final FileFormat format;
        private final char[] password;

        private OpenedFile(File file, FileFormat format, char[] password)
        {
            this.file = file;
            this.format = format;
            this.password = password;
        }

        public File file()
        {
            return file;
        }

        public LoadedClient load() throws CliException
        {
            return load(file);
        }

        /**
         * Loads another file as this one: same format, same password. For
         * reading back what was written.
         */
        LoadedClient load(File other) throws CliException
        {
            try
            {
                var client = ClientFactory.load(other, password, new NullProgressMonitor());

                guard(OptionalInt.of(client.getFileVersionAfterRead()));

                return new LoadedClient(client, format);
            }
            catch (IOException | RuntimeException e)
            {
                throw unreadable(e);
            }
        }

        /**
         * Opens another file with this one's password, so that an encrypted
         * file and its backups need the password only once. The other file
         * passes the version guard too.
         */
        public OpenedFile openRelated(String path) throws CliException
        {
            return open(path, () -> password == null ? null : password.clone());
        }

        @Override
        public void close()
        {
            wipe(password);
        }
    }

    public static LoadedClient load(String path, Supplier<char[]> passwords) throws CliException
    {
        try (var opened = open(path, passwords))
        {
            return opened.load();
        }
    }

    /**
     * Runs the checks that need no loading: the file exists, the password is
     * given and the version stored in the file is supported.
     */
    public static OpenedFile open(String path, Supplier<char[]> passwords) throws CliException
    {
        var file = new File(path);

        if (!Files.isRegularFile(file.toPath()))
            throw CliException.failure("file-not-found", "Client file not found",
                            "No client file at " + file.getAbsolutePath(), 404);

        char[] password = null;
        var handedOver = false;
        try
        {
            FileFormat format;

            if (FileFormat.isEncrypted(file))
            {
                password = passwords.get();
                if (password == null || password.length == 0)
                    throw CliException.failure("password-required", "Password required",
                                    "The client file is encrypted. Set the PP_PASSWORD environment variable or"
                                                    + " provide the password on stdin.",
                                    401);

                var header = EncryptedHeader.read(file, password);
                guard(OptionalInt.of(header.version()));
                format = header.format();
            }
            else
            {
                format = FileFormat.detect(file);
                guard(FileVersionProbe.read(file, format));
            }

            handedOver = true;
            return new OpenedFile(file, format, password);
        }
        catch (IOException | RuntimeException e)
        {
            throw unreadable(e);
        }
        finally
        {
            if (!handedOver)
                wipe(password);
        }
    }

    private static void wipe(char[] password)
    {
        if (password != null)
            Arrays.fill(password, '\0');
    }

    private static CliException unreadable(Exception e)
    {
        return CliException.failure("file-unreadable", "Client file could not be read",
                        "The client file could not be read: " + e.getMessage(), 422, e);
    }

    /**
     * Refuses files newer than core's current version. Core already does so
     * for XML, but unencrypted binary files slip through.
     */
    private static void guard(OptionalInt fileVersion) throws CliException
    {
        if (fileVersion.isPresent() && fileVersion.getAsInt() > Client.CURRENT_VERSION)
            throw CliException.failure("file-version-unsupported", "Client file version is not supported",
                            "The client file has version " + fileVersion.getAsInt()
                                            + " but this pp-cli only supports up to version " + Client.CURRENT_VERSION
                                            + ". Use a pp-cli built from the same release as the desktop app that saved the file.",
                            422)
                                            .with("fileVersion", fileVersion.getAsInt())
                                            .with("maxSupportedVersion", Client.CURRENT_VERSION);
    }
}
