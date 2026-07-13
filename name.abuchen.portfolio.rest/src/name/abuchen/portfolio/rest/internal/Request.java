package name.abuchen.portfolio.rest.internal;

import java.util.Map;

public record Request(String method, String path, Map<String, String> pathParams, byte[] body)
{
    public String pathParam(String name)
    {
        return pathParams.get(name);
    }
}
