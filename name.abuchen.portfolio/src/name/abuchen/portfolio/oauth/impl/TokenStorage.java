package name.abuchen.portfolio.oauth.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.oauth.AccessToken;

public class TokenStorage
{
    private static final String TOKEN_STORAGE = "token_storage"; //$NON-NLS-1$
    private static final String REFRESH_TOKEN = "refresh_token"; //$NON-NLS-1$

    private ISecurePreferences preferences;
    private Path tokenFile;

    private String idToken;
    private Map<String, AccessToken> accessTokenMap = new HashMap<>();
    private String refreshToken;

    private void init()
    {
        try
        {
            var url = FileLocator.resolve(new URI("platform:/meta/name.abuchen.portfolio/token_storage").toURL()); //$NON-NLS-1$ //NOSONAR
            tokenFile = Path.of(new URI(url.getProtocol(), url.getHost(), url.getPath(), null));

            this.refreshToken = loadRefreshTokenFromFile();

            if (this.refreshToken == null)
            {
                loadFromSecurePreferences();

                if (this.refreshToken != null)
                {
                    saveRefreshTokenToFile(this.refreshToken);
                    clearSecurePreferences();
                }
            }

            this.accessTokenMap = new HashMap<>();
        }
        catch (URISyntaxException | IOException e)
        {
            PortfolioLog.error(e);
        }
    }

    public String getIdToken()
    {
        if (tokenFile == null)
            init();
        return idToken;
    }

    public void setIdToken(String idToken)
    {
        if (tokenFile == null)
            init();

        this.idToken = idToken;
    }

    public String getRefreshToken()
    {
        if (tokenFile == null)
            init();
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        if (tokenFile == null)
            init();

        this.refreshToken = refreshToken;
        saveRefreshTokenToFile(refreshToken);
    }

    public Optional<AccessToken> getAccessToken(String resource)
    {
        if (tokenFile == null)
            init();

        var token = accessTokenMap.get(resource == null ? "" : resource); //$NON-NLS-1$

        if (token == null)
            return Optional.empty();

        var isExpired = token.getExpiresAt() < System.currentTimeMillis();
        return isExpired ? Optional.empty() : Optional.of(token);
    }

    public void setAccessToken(String resource, AccessToken accessToken)
    {
        if (tokenFile == null)
            init();

        accessTokenMap.put(resource == null ? "" : resource, accessToken); //$NON-NLS-1$
    }

    public void deleteAccessToken(String resource)
    {
        if (tokenFile == null)
            init();

        accessTokenMap.remove(resource == null ? "" : resource); //$NON-NLS-1$
    }

    public void save(String idToken, AccessToken accessToken, String refreshToken)
    {
        if (tokenFile == null)
            init();

        this.idToken = idToken;
        this.accessTokenMap.put(accessToken.getScopes(), accessToken);

        this.refreshToken = refreshToken;
        saveRefreshTokenToFile(refreshToken);
    }

    public void clear()
    {
        this.idToken = null;
        this.refreshToken = null;
        this.accessTokenMap.clear();

        if (tokenFile != null && Files.exists(tokenFile))
        {
            try
            {
                Files.delete(tokenFile);
            }
            catch (Exception e)
            {
                PortfolioLog.error(e);
            }
        }

        clearSecurePreferences();
    }

    private String loadRefreshTokenFromFile()
    {
        if (tokenFile == null || !Files.exists(tokenFile))
            return null;

        try
        {
            var lines = Files.readAllLines(tokenFile);
            if (!lines.isEmpty())
                return new String(Base64.getDecoder().decode(lines.get(0)), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }
        return null;
    }

    private void saveRefreshTokenToFile(String token)
    {
        if (tokenFile == null || token == null)
            return;

        try
        {
            Files.createDirectories(tokenFile.getParent());
            Files.write(tokenFile, List.of(Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8))),
                            StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }

        try
        {
            var perms = PosixFilePermissions.fromString("rw-------"); //$NON-NLS-1$
            Files.setPosixFilePermissions(tokenFile, perms);
        }
        catch (IOException | UnsupportedOperationException | SecurityException ignore)
        {
            // ignore unsupported OS or file system
        }
    }

    private void loadFromSecurePreferences()
    {
        try
        {
            var url = FileLocator.resolve(new URI("platform:/meta/name.abuchen.portfolio/secure_storage").toURL()); //$NON-NLS-1$ //NOSONAR
            preferences = SecurePreferencesFactory.open(url, null);
        }
        catch (URISyntaxException | IOException e)
        {
            PortfolioLog.error(e);
            return;
        }

        var node = preferences.node(TOKEN_STORAGE);
        try
        {
            this.refreshToken = node.get(REFRESH_TOKEN, null);
        }
        catch (StorageException e)
        {
            PortfolioLog.error(e);
        }
    }

    private void clearSecurePreferences()
    {
        if (preferences == null)
            return;

        var node = preferences.node(TOKEN_STORAGE);
        try
        {
            node.clear();
            node.flush();
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }
    }
}
