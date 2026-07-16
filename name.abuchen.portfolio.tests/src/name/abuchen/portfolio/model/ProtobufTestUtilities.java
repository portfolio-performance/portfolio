package name.abuchen.portfolio.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ProtobufTestUtilities
{
    private ProtobufTestUtilities()
    {
    }

    public static byte[] save(Client client) throws IOException
    {
        var stream = new ByteArrayOutputStream();
        new ProtobufWriter().save(client, stream);
        return stream.toByteArray();
    }

    public static Client load(byte[] bytes) throws IOException
    {
        return new ProtobufWriter().load(new ByteArrayInputStream(bytes));
    }
}
