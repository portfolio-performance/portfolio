package name.abuchen.portfolio.ui.selection;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class SecuritySelection
{
    private final Client client;
    private final List<Security> securities;

    public SecuritySelection(Client client, Security security)
    {
        this.client = Objects.requireNonNull(client);
        this.securities = Arrays.asList(Objects.requireNonNull(security));
    }

    public SecuritySelection(Client client, List<Security> securities)
    {
        this.client = Objects.requireNonNull(client);
        this.securities = Objects.requireNonNull(securities);
    }

    public Client getClient()
    {
        return client;
    }

    public Security getSecurity()
    {
        return securities.get(0);
    }

    public List<Security> getSecurities()
    {
        return securities;
    }
}
