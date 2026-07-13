package name.abuchen.portfolio.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Persists the API bearer token as an owner-only file in the plugin state
 * location. The token is stable across restarts; regenerating it revokes
 * access for all existing clients.
 */
public class TokenStore
{
    private static final String FILE_NAME = "api-token"; //$NON-NLS-1$

    private final Path file;
    private final SecureRandom random = new SecureRandom();

    public TokenStore(Path directory)
    {
        this.file = directory.resolve(FILE_NAME);
    }

    public synchronized String getOrCreate()
    {
        try
        {
            if (Files.exists(file))
                return Files.readString(file, StandardCharsets.UTF_8).trim();
            return regenerate();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized String regenerate()
    {
        try
        {
            var bytes = new byte[32];
            random.nextBytes(bytes);
            var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            Files.createDirectories(file.getParent());
            Files.writeString(file, token, StandardCharsets.UTF_8);
            restrictToOwner();
            return token;
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
