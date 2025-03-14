package name.abuchen.portfolio.oauth.impl;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthorizationCode
{
    private final String state;
    private final String code;

    AuthorizationCode(String state, String code)
    {
        this.state = state;
        this.code = code;
    }

    public String getState()
    {
        return state;
    }

    public String getCode()
    {
        return code;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AuthorizationCode other = (AuthorizationCode) obj;
        return Objects.equals(code, other.code) && Objects.equals(state, other.state);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(code, state);
    }

    public static AuthorizationCode parse(String query)
    {
        var parameters = Stream.of(query.split("&")) //$NON-NLS-1$
                        .filter(s -> !s.isEmpty()) //
                        .map(kv -> kv.split("=", 2)) //$NON-NLS-1$
                        .filter(s -> s.length == 2) //
                        .collect(Collectors.toMap(x -> x[0].toLowerCase(), x -> x[1]));

        return new AuthorizationCode(parameters.get("state"), parameters.get("code")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
