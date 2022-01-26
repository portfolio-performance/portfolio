package name.abuchen.portfolio.server;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.server.resources.AccountResource;
import name.abuchen.portfolio.server.resources.PortfolioResource;
import name.abuchen.portfolio.server.resources.SecuritiesResource;
import name.abuchen.portfolio.server.resources.TaxonomiesResource;
import name.abuchen.portfolio.server.resources.TransactionResource;

@SuppressWarnings("restriction")
public class JerseyApplication extends ResourceConfig
{
    public JerseyApplication()
    {
        register(GsonMessageBodyHandler.class);
        register(LocalDateParameterProvider.class);

        register(TransactionResource.class);
        register(SecuritiesResource.class);
        register(AccountResource.class);
        register(PortfolioResource.class);
        register(TaxonomiesResource.class);

        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(ClientContextFactory.class).to(Client.class).proxy(true).proxyForSameScope(false)
                                .in(RequestScoped.class);
            }
        });
    }
}
