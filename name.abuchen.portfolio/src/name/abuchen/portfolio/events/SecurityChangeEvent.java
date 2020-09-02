package name.abuchen.portfolio.events;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class SecurityChangeEvent extends ChangeEvent
{

    public SecurityChangeEvent(Client client, Security security)
    {
        super(client, security);
    }

    public Security getSecurity()
    {
        return (Security) getSubject();
    }
}
