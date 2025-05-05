package name.abuchen.portfolio.oauth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

public class AccessToken
{
    public static class Claims
    {
        private final String sub;
        private final String email;
        private final String plan;

        public Claims(String sub, String email, String plan)
        {
            this.sub = sub;
            this.email = email;
            this.plan = plan;
        }

        public String getSub()
        {
            return sub;
        }

        public String getEmail()
        {
            return email;
        }

        public String getPlan()
        {
            return plan;
        }
    }

    private final String token;
    private final String scopes;
    private final long expiresAt;

    private Claims claims;

    public AccessToken(String token, String scope, long expiresAt)
    {
        this.token = token;
        this.scopes = scope;
        this.expiresAt = expiresAt;
    }

    public String getToken()
    {
        return token;
    }

    public String getScopes()
    {
        return scopes;
    }

    public long getExpiresAt()
    {
        return expiresAt;
    }

    public Claims getClaims()
    {
        if (claims != null)
            return claims;

        DecodedJWT jwt = JWT.decode(token);
        var sub = jwt.getSubject();
        var email = jwt.getClaim("email"); //$NON-NLS-1$
        var plan = jwt.getClaim("plan"); //$NON-NLS-1$

        claims = new Claims(sub, //
                        email != null ? email.asString() : null, //
                        plan != null ? plan.asString() : "none"); //$NON-NLS-1$

        return claims;
    }
}
