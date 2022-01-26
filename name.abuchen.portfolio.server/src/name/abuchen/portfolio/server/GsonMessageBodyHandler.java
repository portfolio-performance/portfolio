package name.abuchen.portfolio.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import name.abuchen.portfolio.json.JClient;

@Produces(MediaType.APPLICATION_JSON)
public class GsonMessageBodyHandler implements MessageBodyWriter<Object>, MessageBodyReader<Object>
{
    @Override
    public boolean isReadable(Class<?> type, Type genericType, java.lang.annotation.Annotation[] annotations,
                    MediaType mediaType)
    {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                    MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException
    {
        try (InputStreamReader streamReader = new InputStreamReader(entityStream, StandardCharsets.UTF_8))
        {
            return JClient.GSON.fromJson(streamReader, genericType);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return true;
    }

    @Override
    public long getSize(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                    MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
    {
        try (OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8))
        {
            JClient.GSON.toJson(object, genericType, writer);
        }
    }
}
