package name.abuchen.portfolio.snapshot.filter;

import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

public class ReadOnlyClient extends Client
{
    private final Client source;

    ReadOnlyClient(Client source)
    {
        super();

        this.source = Objects.requireNonNull(source);

        super.setBaseCurrency(source.getBaseCurrency());
    }

    @Override
    public void addSecurity(Security security)
    {
        throw new UnsupportedOperationException();
    }

    void internalAddSecurity(Security security)
    {
        super.addSecurity(security);
    }

    @Override
    public void removeSecurity(Security security)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAccount(Account account)
    {
        throw new UnsupportedOperationException();
    }

    void internalAddAccount(Account account)
    {
        super.addAccount(account);
    }

    @Override
    public void removeAccount(Account account)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPortfolio(Portfolio portfolio)
    {
        throw new UnsupportedOperationException();
    }

    void internalAddPortfolio(Portfolio portfolio)
    {
        super.addPortfolio(portfolio);
    }

    @Override
    public void removePortfolio(Portfolio portfolio)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String key, String value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBaseCurrency(String baseCurrency)
    {
        throw new UnsupportedOperationException();
    }

    public static Client unwrap(Client client)
    {
        return client instanceof ReadOnlyClient readOnly ? unwrap(readOnly.source) : client;
    }
}
