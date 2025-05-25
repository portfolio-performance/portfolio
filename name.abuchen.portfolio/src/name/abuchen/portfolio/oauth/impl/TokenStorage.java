package name.abuchen.portfolio.oauth.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.oauth.AccessToken;

public class TokenStorage
{
    private static final String TOKEN_STORAGE = "token_storage"; //$NON-NLS-1$
    private static final String ID_TOKEN = "id_token"; //$NON-NLS-1$
    private static final String ACCESS_TOKENS = "access_tokens"; //$NON-NLS-1$
    private static final String REFRESH_TOKEN = "refresh_token"; //$NON-NLS-1$

    private ISecurePreferences preferences;

    private String idToken;
    private Map<String, AccessToken> accessTokenMap = new HashMap<>();
    private String refreshToken;

    private void init()
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

        ISecurePreferences node = preferences.node(TOKEN_STORAGE);
        try
        {
            this.idToken = node.get(ID_TOKEN, null);
            this.refreshToken = node.get(REFRESH_TOKEN, null);
            String accessTokensJson = node.get(ACCESS_TOKENS, null);
            if (accessTokensJson != null)
            {
                this.accessTokenMap = new Gson().fromJson(accessTokensJson, new TypeToken<Map<String, AccessToken>>()
                {
                }.getType());
            }
            else
            {
                this.accessTokenMap = new HashMap<>();
            }
        }
        catch (StorageException e)
        {
            PortfolioLog.error(e);
        }
    }

    public String getIdToken()
    {
        if (preferences == null)
            init();
        return idToken;
    }

    public void setIdToken(String idToken)
    {
        if (preferences == null)
            init();

        this.idToken = idToken;

        ISecurePreferences node = preferences.node(TOKEN_STORAGE);
        try
        {
            node.put(REFRESH_TOKEN, refreshToken, true);
            node.flush();
        }
        catch (StorageException | IOException e)
        {
            PortfolioLog.error(e);
        }
    }

    public String getRefreshToken()
    {
        if (preferences == null)
            init();
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        if (preferences == null)
            init();

        this.refreshToken = refreshToken;

        ISecurePreferences node = preferences.node(TOKEN_STORAGE);
        try
        {
            node.put(REFRESH_TOKEN, refreshToken, true);
            node.flush();
        }
        catch (StorageException | IOException e)
        {
            PortfolioLog.error(e);
        }
    }

    public Optional<AccessToken> getAccessToken(String resource)
    {
        if (preferences == null)
            init();

        var token = accessTokenMap.get(resource == null ? "" : resource); //$NON-NLS-1$

        if (token == null)
            return Optional.empty();

        var isExpired = token.getExpiresAt() < System.currentTimeMillis();
        return isExpired ? Optional.empty() : Optional.of(token);
    }

    public void setAccessToken(String resource, AccessToken accessToken)
    {
        if (preferences == null)
            init();

        accessTokenMap.put(resource == null ? "" : resource, accessToken); //$NON-NLS-1$

        ISecurePreferences node = preferences.node(TOKEN_STORAGE);
        try
        {
            node.put(ACCESS_TOKENS, new Gson().toJson(accessTokenMap), true);
            node.flush();
        }
        catch (StorageException | IOException e)
        {
            PortfolioLog.error(e);
        }
    }

    public void deleteAccessToken(String resource)
    {
        if (preferences == null)
            init();

        accessTokenMap.remove(resource == null ? "" : resource); //$NON-NLS-1$

        ISecurePreferences node = preferences.node(TOKEN_STORAGE);
        try
        {
            node.put(ACCESS_TOKENS, new Gson().toJson(accessTokenMap), true);
            node.flush();
        }
        catch (StorageException | IOException e)
        {
            PortfolioLog.error(e);
        }
    }

    public void save(String idToken, AccessToken accessToken, String refreshToken)
    {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.accessTokenMap.put(accessToken.getScopes(), accessToken);

        if (preferences == null)
            preferences = SecurePreferencesFactory.getDefault();

        ISecurePreferences node = preferences.node(TOKEN_STORAGE);
        try
        {
            node.put(ID_TOKEN, idToken, true);
            node.put(ACCESS_TOKENS, new Gson().toJson(accessTokenMap), true);
            node.put(REFRESH_TOKEN, refreshToken, true);

            node.flush();
        }
        catch (StorageException | IOException e)
        {
            PortfolioLog.error(e);
        }
    }

    public void clear()
    {
        this.idToken = null;
        this.refreshToken = null;
        this.accessTokenMap.clear();

        if (preferences == null)
            preferences = SecurePreferencesFactory.getDefault();

        ISecurePreferences node = preferences.node(TOKEN_STORAGE);
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
