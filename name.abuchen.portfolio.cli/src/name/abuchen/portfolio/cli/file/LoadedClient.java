package name.abuchen.portfolio.cli.file;

import name.abuchen.portfolio.model.Client;

/** A client file that passed the version guard. */
public record LoadedClient(Client client, FileFormat format)
{
}
