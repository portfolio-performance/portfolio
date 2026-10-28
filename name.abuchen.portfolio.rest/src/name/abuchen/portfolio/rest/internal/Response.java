package name.abuchen.portfolio.rest.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;

/**
 * One response as a handler returns it: a body written at once, or - for a
 * streamed MCP answer - a {@link Stream} that writes it piece by piece while
 * the request runs. {@code stream} is null for every response with a body.
 */
public record Response(int status, String contentType, byte[] body, Map<String, String> headers, Stream stream)
{
    /** where a streamed response writes its pieces; each one goes out to the client at once */
    @FunctionalInterface
    public interface Sink
    {
        void send(String text) throws IOException;
    }

    /** writes a streamed response; returning ends it */
    @FunctionalInterface
    public interface Stream
    {
        void writeTo(Sink sink) throws IOException;
    }

    public Response(int status, String contentType, byte[] body, Map<String, String> headers)
    {
        this(status, contentType, body, headers, null);
    }

    public static Response json(int status, JsonElement element)
    {
        return new Response(status, "application/json", element.toString().getBytes(StandardCharsets.UTF_8), Map.of()); //$NON-NLS-1$
    }

    /** A response with a caller-supplied content type and raw body, e.g. the OpenAPI document. */
    public static Response of(int status, String contentType, byte[] body)
    {
        return new Response(status, contentType, body, Map.of());
    }

    public static Response noContent()
    {
        return new Response(204, null, new byte[0], Map.of());
    }

    /** a response written piece by piece, with no body of its own */
    public static Response streamed(int status, String contentType, Stream stream)
    {
        return new Response(status, contentType, new byte[0], Map.of(), stream);
    }

    public boolean isStreamed()
    {
        return stream != null;
    }

    /** the same response with one more header */
    public Response withHeader(String name, String value)
    {
        var merged = new LinkedHashMap<String, String>(headers);
        merged.put(name, value);
        return new Response(status, contentType, body, Map.copyOf(merged), stream);
    }
}
