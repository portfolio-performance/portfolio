package name.abuchen.portfolio.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import name.abuchen.portfolio.model.Client;

public interface ClientPersister
{
    Client load(InputStream input) throws IOException;

    void save(Client client, OutputStream output) throws IOException;
}
