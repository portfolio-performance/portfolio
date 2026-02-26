package name.abuchen.portfolio.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.oauth.impl.AuthorizationCode;
import name.abuchen.portfolio.oauth.impl.CallbackServer;
import name.abuchen.portfolio.oauth.impl.CodeTokenResponse;
import name.abuchen.portfolio.oauth.impl.OAuthConfig;
import name.abuchen.portfolio.oauth.impl.PKCE;
import name.abuchen.portfolio.oauth.impl.TokenStorage;

public class OAuthClient // NOSONAR
{
    public static final OAuthClient INSTANCE = new OAuthClient();

    private final OAuthConfig config = OAuthConfig.load();

    private final TokenStorage tokenStorage = new TokenStorage();
    private final CallbackServer callbackServer = new CallbackServer();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final List<Runnable> listeners = new ArrayList<>();

    private CompletableFuture<AccessToken> ongoingAuthentication = null;

    public void addStatusListener(Runnable listener)
    {
        listeners.add(listener);
    }

    public void removeStatusListener(Runnable listener)
    {
        listeners.remove(listener);
    }

    private void informListeners()
    {
        for (var listener : new ArrayList<>(listeners))
            listener.run();
    }

    public boolean isAuthenticationOngoing()
    {
        return ongoingAuthentication != null && !ongoingAuthentication.isDone();
    }

    public boolean isAuthenticated()
    {
        return tokenStorage.getRefreshToken() != null;
    }

    public Optional<UserInfo> getUserInfo() throws AuthenticationException
    {
        return getAPIAccessToken().map(token -> new UserInfo(token.getClaims().getSub(), token.getClaims().getEmail()));
    }

    public Optional<AccessToken> getAccessToken(String resource) throws AuthenticationException
    {
        var accessToken = tokenStorage.getAccessToken(resource);

        if (accessToken.isPresent())
            return accessToken;

        return Optional.of(getAccessTokenByRefreshToken(resource));
    }

    public Optional<AccessToken> getAPIAccessToken() throws AuthenticationException
    {
        if (config == null)
            throw new AuthenticationException(Messages.OAuthNotConfigured);
        return getAccessToken(config.apiResource);
    }

    public void clearAccessToken(String resource)
    {
        tokenStorage.deleteAccessToken(resource);
    }

    public void clearAPIAccessToken() throws AuthenticationException
    {
        if (config == null)
            throw new AuthenticationException(Messages.OAuthNotConfigured);
        clearAccessToken(config.apiResource);
    }

    public void signIn(Consumer<String> browser) throws AuthenticationException
    {
        if (ongoingAuthentication != null && !ongoingAuthentication.isDone())
            throw new AuthenticationException(Messages.OAuthOngoingAuthentication);

        if (config == null)
            throw new AuthenticationException(Messages.OAuthNotConfigured);

        ongoingAuthentication = new CompletableFuture<>();
        var pkce = PKCE.generate();
        var state = UUID.randomUUID().toString();

        try
        {
            callbackServer.start();
            var redirectUri = callbackServer.getSuccessEndpoint();
            callbackServer.setCallbackHandler(code -> handleSignInCallback(code, pkce, state, redirectUri));
        }
        catch (IOException e)
        {
            ongoingAuthentication.completeExceptionally(e);
            throw new AuthenticationException(Messages.OAuthFailedToStartCallbackServer, e);
        }

        // inform on ongoing authentication
        informListeners();

        @SuppressWarnings("nls")
        String authzUrl = config.baseUrl + config.authEndpoint //
                        + "?response_type=code" //
                        + "&prompt=" + URLEncoder.encode("login consent", StandardCharsets.UTF_8) //
                        + "&code_challenge=" + pkce.getCodeChallenge() //
                        + "&code_challenge_method=" + PKCE.CODE_CHALLENGE_METHOD //
                        + "&client_id=" + config.clientId //
                        + "&redirect_uri="
                        + URLEncoder.encode(callbackServer.getSuccessEndpoint(), StandardCharsets.UTF_8) //
                        + "&scope=" + URLEncoder.encode(config.authScope, StandardCharsets.UTF_8) //
                        + "&state=" + state;

        browser.accept(authzUrl);

        // setup error handling

        ongoingAuthentication.exceptionally(ex -> {
            if (!(ex instanceof CancellationException))
                PortfolioLog.error(ex);
            return null;
        });

        // inform listeners
        ongoingAuthentication.thenRun(this::informListeners);

        // cancel ongoing authentication after 2 minutes
        scheduler.schedule(() -> {
            if (ongoingAuthentication != null && !ongoingAuthentication.isDone())
            {
                ongoingAuthentication.cancel(true);
                informListeners();
            }
        }, 1, TimeUnit.MINUTES);

    }

    private void handleSignInCallback(AuthorizationCode authorizationCode, PKCE pkce, String state, String redirectUri)
    {
        callbackServer.stop();

        if (!state.equals(authorizationCode.getState()))
        {
            ongoingAuthentication.completeExceptionally(
                            new AuthenticationException(Messages.OAuthAuthenticationFailedDueToStateMismatch));
            return;
        }

        try
        {
            var response = fetchTokenByAuthorizationCode(authorizationCode, pkce, redirectUri);

            var idToken = response.getIdToken();
            var accessToken = response.getAccessToken();
            var refreshToken = response.getRefreshToken();

            tokenStorage.save(idToken, accessToken, refreshToken);

            ongoingAuthentication.complete(accessToken);
        }
        catch (IOException e)
        {
            ongoingAuthentication.completeExceptionally(
                            new AuthenticationException(Messages.OAuthFailedToRequestAccessToken, e));
        }
    }

    public void signOut() throws AuthenticationException
    {
        if (config == null)
            throw new AuthenticationException("Not configured"); //$NON-NLS-1$

        var idToken = tokenStorage.getIdToken();
        if (idToken == null)
            throw new AuthenticationException(Messages.OAuthNotAuthenticated);

        var refreshToken = tokenStorage.getRefreshToken();
        if (refreshToken != null)
        {
            var payload = new StringBuilder() //
                            .append("client_id=").append(config.clientId) //$NON-NLS-1$
                            .append("&token=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)); //$NON-NLS-1$

            HttpPost request = new HttpPost(config.baseUrl + config.revocationEndpoint);
            request.setHeader("Content-Type", "application/x-www-form-urlencoded"); //$NON-NLS-1$ //$NON-NLS-2$
            request.setEntity(new StringEntity(payload.toString()));

            try (CloseableHttpClient client = HttpClientBuilder.create().useSystemProperties().build())
            {
                client.execute(request, new BasicHttpClientResponseHandler());
            }
            catch (Exception ignore)
            {
                // ignore silently
            }
        }

        tokenStorage.clear();

        informListeners();
    }

    private AccessToken getAccessTokenByRefreshToken(String resource) throws AuthenticationException
    {
        var refreshToken = tokenStorage.getRefreshToken();
        if (refreshToken == null)
            throw new AuthenticationException(Messages.OAuthNotAuthenticated);

        try
        {
            var codeTokenResponse = fetchTokenByRefreshToken(refreshToken, resource);

            var accessToken = codeTokenResponse.getAccessToken();

            tokenStorage.setAccessToken(resource, accessToken);

            return accessToken;
        }
        catch (HttpResponseException e)
        {
            // if the refresh token expired, then clear all tokens to allow to
            // sign-in again
            if (e.getStatusCode() == HttpStatus.SC_BAD_REQUEST)
            {
                tokenStorage.clear();
                informListeners();
            }

            throw new AuthenticationException(Messages.OAuthFailedToRequestAccessToken, e);
        }
        catch (IOException e)
        {
            throw new AuthenticationException(Messages.OAuthFailedToRequestAccessToken, e);
        }
    }

    private CodeTokenResponse fetchTokenByAuthorizationCode(AuthorizationCode authorizationCode, PKCE pkce,
                    String redirectUri) throws IOException
    {
        var parameter = new HashMap<String, String>();
        parameter.put("client_id", config.clientId); //$NON-NLS-1$
        parameter.put("code", authorizationCode.getCode()); //$NON-NLS-1$
        parameter.put("code_verifier", pkce.getCodeVerifier()); //$NON-NLS-1$
        parameter.put("redirect_uri", redirectUri); //$NON-NLS-1$
        parameter.put("grant_type", "authorization_code"); //$NON-NLS-1$ //$NON-NLS-2$

        return fetchToken(parameter);
    }

    private CodeTokenResponse fetchTokenByRefreshToken(String refreshToken, String resource) throws IOException
    {
        var parameter = new HashMap<String, String>();
        parameter.put("grant_type", "refresh_token"); //$NON-NLS-1$ //$NON-NLS-2$
        parameter.put("client_id", config.clientId); //$NON-NLS-1$
        parameter.put("refresh_token", refreshToken); //$NON-NLS-1$

        if (resource != null)
            parameter.put("resource", resource); //$NON-NLS-1$

        var response = fetchToken(parameter);

        // renew refresh token
        if (response.getRefreshToken() != null)
            tokenStorage.setRefreshToken(response.getRefreshToken());

        // store id_token if not null
        if (response.getIdToken() != null)
            tokenStorage.setIdToken(response.getIdToken());

        return response;
    }

    private CodeTokenResponse fetchToken(Map<String, String> parameter) throws IOException
    {
        StringBuilder payload = new StringBuilder();
        for (var entry : parameter.entrySet())
        {
            if (payload.length() > 0)
                payload.append("&"); //$NON-NLS-1$

            payload.append(entry.getKey());
            payload.append("="); //$NON-NLS-1$
            payload.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        HttpPost request = new HttpPost(config.baseUrl + config.tokenEndpoint);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded"); //$NON-NLS-1$ //$NON-NLS-2$
        request.setEntity(new StringEntity(payload.toString()));

        try (CloseableHttpClient client = HttpClientBuilder.create().useSystemProperties().build())
        {
            String response = client.execute(request, new BasicHttpClientResponseHandler());
            return CodeTokenResponse.fromJson(response);
        }
    }
}
