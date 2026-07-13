package name.abuchen.portfolio.rest.internal;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.JsonElement;

public record Response(int status, String contentType, byte[] body, Map<String, String> headers)
{
    public static Response json(int status, JsonElement element)
    {
        return new Response(status, "application/json", element.toString().getBytes(StandardCharsets.UTF_8), Map.of()); //$NON-NLS-1$
    }

    public static Response noContent()
    {
        return new Response(204, null, new byte[0], Map.of());
    }
}
