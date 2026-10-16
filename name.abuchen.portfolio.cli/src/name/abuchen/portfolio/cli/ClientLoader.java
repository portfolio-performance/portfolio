package name.abuchen.portfolio.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.eclipse.core.runtime.NullProgressMonitor;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/**
 * Loads a client file through core. Every load runs the version guard, for
 * every command.
 */
final class ClientLoader
{
    private ClientLoader()
    {
    }

    static LoadedClient load(String path, Supplier<char[]> passwords) throws CliException
    {
        var file = new File(path);

        if (!Files.isRegularFile(file.toPath()))
            throw CliException.failure("file-not-found", "Client file not found",
                            "No client file at " + file.getAbsolutePath(), 404);

        char[] password = null;
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

            var client = ClientFactory.load(file, password, new NullProgressMonitor());

            guard(OptionalInt.of(client.getFileVersionAfterRead()));

            return new LoadedClient(client, format);
        }
        catch (IOException | RuntimeException e)
        {
            throw CliException.failure("file-unreadable", "Client file could not be read",
                            "The client file could not be read: " + e.getMessage(), 422, e);
        }
        finally
        {
            if (password != null)
                Arrays.fill(password, '\0');
        }
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
