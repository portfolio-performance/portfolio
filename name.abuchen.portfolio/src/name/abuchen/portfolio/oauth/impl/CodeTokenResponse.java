package name.abuchen.portfolio.oauth.impl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import name.abuchen.portfolio.oauth.AccessToken;

public class CodeTokenResponse
{
    @SerializedName("access_token")
    private final String accessToken;

    @SerializedName("refresh_token")
    private final String refreshToken;

    @SerializedName("id_token")
    private final String idToken;

    @SerializedName("scope")
    private final String scopes;

    @SerializedName("expires_in")
    private final int expiresIn;

    public CodeTokenResponse(String accessToken, String refreshToken, String idToken, String scopes, int expiresIn)
    {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.scopes = scopes;
        this.expiresIn = expiresIn;
    }

    public static CodeTokenResponse fromJson(String json) throws JsonSyntaxException
    {
        Gson gson = new Gson();
        return gson.fromJson(json, CodeTokenResponse.class);
    }

    public AccessToken getAccessToken()
    {
        long expiresAt = System.currentTimeMillis() + expiresIn * 1000;
        return new AccessToken(accessToken, scopes, expiresAt);
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public String getIdToken()
    {
        return idToken;
    }
}
