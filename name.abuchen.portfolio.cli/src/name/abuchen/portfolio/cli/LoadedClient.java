package name.abuchen.portfolio.cli;

import name.abuchen.portfolio.model.Client;

/** A client file that passed the version guard. */
record LoadedClient(Client client, FileFormat format)
{
}
