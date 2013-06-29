package name.abuchen.portfolio.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;

public class Client
{
    /* package */static final int CURRENT_VERSION = 14;

    private transient PropertyChangeSupport propertyChangeSupport;

    private int version = CURRENT_VERSION;

    private List<Security> securities = new ArrayList<Security>();
    private List<Watchlist> watchlists;
    private List<ConsumerPriceIndex> consumerPriceIndeces;

    private List<Account> accounts = new ArrayList<Account>();
    private List<Portfolio> portfolios = new ArrayList<Portfolio>();
    private List<InvestmentPlan> plans;
    private Category rootCategory = new Category(Messages.LabelPortfolio, 100);

    private Map<String, String> properties; // old versions!

    private String industryTaxonomyId;

    private List<Taxonomy> taxonomies;

    public Client()
    {
        doPostLoadInitialization();
    }

    /* package */final void doPostLoadInitialization()
    {
        // when loading the Client from XML, attributes that are not (yet)
        // persisted in that version are not initialized

        if (watchlists == null)
            watchlists = new ArrayList<Watchlist>();

        if (consumerPriceIndeces == null)
            consumerPriceIndeces = new ArrayList<ConsumerPriceIndex>();

        if (properties == null)
            properties = new HashMap<String, String>();

        if (propertyChangeSupport == null)
            propertyChangeSupport = new PropertyChangeSupport(this);

        if (plans == null)
            plans = new ArrayList<InvestmentPlan>();

        if (taxonomies == null)
            taxonomies = new ArrayList<Taxonomy>();
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public List<InvestmentPlan> getPlans()
    {
        return plans;
    }

    public void addPlan(InvestmentPlan plan)
    {
        plans.add(plan);
    }

    public void removePlan(InvestmentPlan plan)
    {
        plans.remove(plan);
    }

    public List<Security> getSecurities()
    {
        return securities;
    }

    public void addSecurity(Security security)
    {
        securities.add(security);
    }

    public void addSecurities(Collection<Security> sec)
    {
        securities.addAll(sec);
    }

    public void removeSecurity(Security security)
    {
        securities.remove(security);
        for (Watchlist w : watchlists)
            w.getSecurities().remove(security);
        deleteInvestmentPlans(security);
        // FIXME possibly remove transactions and category assignments as well
    }

    public List<Watchlist> getWatchlists()
    {
        return watchlists;
    }

    public List<ConsumerPriceIndex> getConsumerPriceIndeces()
    {
        return consumerPriceIndeces;
    }

    public void setConsumerPriceIndeces(List<ConsumerPriceIndex> prices)
    {
        this.consumerPriceIndeces = prices;
        Collections.sort(this.consumerPriceIndeces);
    }

    public void addConsumerPriceIndex(ConsumerPriceIndex record)
    {
        consumerPriceIndeces.add(record);
    }

    public void addAccount(Account account)
    {
        accounts.add(account);
    }

    public void removeAccount(Account account)
    {
        deleteCrossEntries(account.getTransactions());
        deleteInvestmentPlans(account);
        accounts.remove(account);
    }

    public List<Account> getAccounts()
    {
        return accounts;
    }

    public void addPortfolio(Portfolio portfolio)
    {
        portfolios.add(portfolio);
    }

    public void removePortfolio(Portfolio portfolio)
    {
        deleteCrossEntries(portfolio.getTransactions());
        deleteInvestmentPlans(portfolio);
        portfolios.remove(portfolio);
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

    public void setIndustryTaxonomy(IndustryClassification taxonomy)
    {
        this.industryTaxonomyId = taxonomy != null ? taxonomy.getIdentifier() : null;
    }

    public IndustryClassification getIndustryTaxonomy()
    {
        return IndustryClassification.lookup(industryTaxonomyId);
    }

    public List<Taxonomy> getTaxonomies()
    {
        return Collections.unmodifiableList(taxonomies);
    }

    public void addTaxonomy(Taxonomy taxonomy)
    {
        taxonomies.add(taxonomy);
    }

    public void removeTaxonomy(Taxonomy taxonomy)
    {
        taxonomies.remove(taxonomy);
    }

    public void setProperty(String key, String value)
    {
        String oldValue = properties.put(key, value);
        propertyChangeSupport.firePropertyChange("properties", oldValue, value); //$NON-NLS-1$
    }

    public void removeProperity(String key)
    {
        String oldValue = properties.remove(key);
        propertyChangeSupport.firePropertyChange("properties", oldValue, null); //$NON-NLS-1$
    }

    public String getProperty(String key)
    {
        return properties.get(key);
    }

    private void deleteCrossEntries(List<? extends Transaction> transactions)
    {
        // crossEntry.delete modifies list
        for (Transaction t : new ArrayList<Transaction>(transactions))
        {
            if (t.getCrossEntry() != null)
                t.getCrossEntry().delete();
        }
    }

    private void deleteInvestmentPlans(Portfolio portfolio)
    {
        for (InvestmentPlan plan : plans)
        {
            if (plan.getPortfolio().equals(portfolio))
                removePlan(plan);
        }
    }

    private void deleteInvestmentPlans(Account account)
    {
        for (InvestmentPlan plan : plans)
        {
            if (plan.getAccount().equals(account))
                removePlan(plan);
        }
    }

    private void deleteInvestmentPlans(Security security)
    {
        for (InvestmentPlan plan : plans)
        {
            if (plan.getSecurity().equals(security))
                removePlan(plan);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
