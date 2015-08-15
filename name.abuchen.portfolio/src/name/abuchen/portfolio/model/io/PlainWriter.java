package name.abuchen.portfolio.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory.XmlSerialization;

public class PlainWriter implements ClientPersister
{
    @Override
    public Client load(InputStream input) throws IOException
    {
        return new XmlSerialization().load(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    @Override
    public void save(Client client, int method, OutputStream output) throws IOException
    {
        new XmlSerialization().save(client, output);
    }
}
