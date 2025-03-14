package name.abuchen.portfolio.oauth.impl;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class OAuthConfig
{
    public String clientId;
    public String baseUrl;
    public String authEndpoint;
    public String tokenEndpoint;
    public String revocationEndpoint;
    public String authScope;
    public String apiResource;

    public static OAuthConfig load() throws JsonSyntaxException
    {
        var config = OAuthConfig.class.getResourceAsStream("config.json"); //$NON-NLS-1$
        if (config == null)
            return null;

        try (Scanner scanner = new Scanner(config, StandardCharsets.UTF_8.name()))
        {
            String json = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
            Gson gson = new Gson();
            return gson.fromJson(json, OAuthConfig.class);
        }
    }
}
