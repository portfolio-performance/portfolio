package name.abuchen.portfolio.rest.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * One response as a handler returns it: a body written at once, or - for a
 * streamed MCP answer - a {@link Stream} that writes it piece by piece while
 * the request runs. {@code stream} is null for every response with a body.
 * <p/>
 * {@code inProcess} is for a caller inside this process only - the MCP
 * endpoint dispatching into the routes - and is never written to the wire: what
 * a route can say about its own work that the HTTP answer does not carry, such
 * as the record a delete removed, under the name that route gives it.
 */
public record Response(int status, String contentType, byte[] body, Map<String, String> headers, Stream stream,
                JsonElement inProcess)
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
        this(status, contentType, body, headers, null, null);
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
        return new Response(status, contentType, new byte[0], Map.of(), stream, null);
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
        return new Response(status, contentType, body, Map.copyOf(merged), stream, inProcess);
    }

    /**
     * the same response, telling an in-process caller what it may need besides:
     * one record under the name the route gives it, so that an answer can say
     * what a delete removed without the endpoint knowing what each of its routes
     * deletes
     */
    public Response withInProcess(String name, JsonElement record)
    {
        var named = new JsonObject();
        named.add(name, record == null ? JsonNull.INSTANCE : record);
        return new Response(status, contentType, body, headers, stream, named);
    }
}
