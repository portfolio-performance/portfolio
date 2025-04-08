package name.abuchen.portfolio.events;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class SecurityCreatedEvent extends ChangeEvent
{
    public SecurityCreatedEvent(Client client, Security security)
    {
        super(client, security);
    }

    public Security getSecurity()
    {
        return (Security) getSubject();
    }
}
