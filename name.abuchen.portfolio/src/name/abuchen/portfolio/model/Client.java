package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.Messages;

public class Client
{
    private int version = 2;

    private List<Security> securities = new ArrayList<Security>();
    private List<ConsumerPriceIndex> consumerPriceIndeces = new ArrayList<ConsumerPriceIndex>();

    private List<Account> accounts = new ArrayList<Account>();
    private List<Portfolio> portfolios = new ArrayList<Portfolio>();
    private Category rootCategory = new Category(Messages.LabelPortfolio, 100);

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public List<Security> getSecurities()
    {
        return securities;
    }

    public void addSecurity(Security security)
    {
        securities.add(security);
    }

    public List<ConsumerPriceIndex> getConsumerPriceIndeces()
    {
        if (consumerPriceIndeces == null)
            consumerPriceIndeces = new ArrayList<ConsumerPriceIndex>();
        return consumerPriceIndeces;
    }

    public void setConsumerPriceIndeces(List<ConsumerPriceIndex> prices)
    {
        this.consumerPriceIndeces = prices;
        Collections.sort(this.consumerPriceIndeces);
    }

    public void addConsumerPriceIndex(ConsumerPriceIndex record)
    {
        if (consumerPriceIndeces == null)
            consumerPriceIndeces = new ArrayList<ConsumerPriceIndex>();
        consumerPriceIndeces.add(record);
    }

    public void addAccount(Account account)
    {
        accounts.add(account);
    }

    public List<Account> getAccounts()
    {
        return accounts;
    }

    public void addPortfolio(Portfolio portfolio)
    {
        portfolios.add(portfolio);
    }

    public List<Portfolio> getPortfolios()
    {
        return portfolios;
    }

    public void setRootCategory(Category root)
    {
        this.rootCategory = root;
    }

    public Category getRootCategory()
    {
        return this.rootCategory;
    }
}
