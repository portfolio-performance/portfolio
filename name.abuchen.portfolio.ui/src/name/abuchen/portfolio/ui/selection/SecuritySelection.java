package name.abuchen.portfolio.ui.selection;

import java.util.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class SecuritySelection
{
    private final Client client;
    private final Security security;

    public SecuritySelection(Client client, Security security)
    {
        this.client = Objects.requireNonNull(client);
        this.security = Objects.requireNonNull(security);
    }

    public Client getClient()
    {
        return client;
    }

    public Security getSecurity()
    {
        return security;
    }
}
