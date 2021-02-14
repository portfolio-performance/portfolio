package name.abuchen.portfolio.server;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.glassfish.hk2.api.Factory;

import name.abuchen.portfolio.model.Client;

public class ClientContextFactory implements Factory<Client>
{
    private HttpServletRequest request;

    @Inject
    public ClientContextFactory(HttpServletRequest request)
    {
        this.request = request;
    }

    @Override
    public Client provide()
    {
        return (Client) request.getAttribute(Client.class.getName());
    }

    @Override
    public void dispose(Client client) // NOSONAR
    {}
}
