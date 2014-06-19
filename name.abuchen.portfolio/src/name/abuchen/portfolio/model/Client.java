package name.abuchen.portfolio.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import name.abuchen.portfolio.model.Classification.Assignment;

public class Client
{
    /* package */static final int MAJOR_VERSION = 1;
    /* package */static final int CURRENT_VERSION = 21;

    private transient PropertyChangeSupport propertyChangeSupport;

    private int version = CURRENT_VERSION;

    private List<Security> securities = new ArrayList<Security>();
    private List<Watchlist> watchlists;
    private List<ConsumerPriceIndex> consumerPriceIndeces;

    private List<Account> accounts = new ArrayList<Account>();
    private List<Portfolio> portfolios = new ArrayList<Portfolio>();
    private List<InvestmentPlan> plans;
    private List<Taxonomy> taxonomies;

    private Map<String, String> properties; // old versions!

    @Deprecated
    private String industryTaxonomyId;

    @Deprecated
    private Category rootCategory;

    private transient SecretKey secret;

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
        return Collections.unmodifiableList(plans);
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
        return Collections.unmodifiableList(securities);
    }

    public void addSecurity(Security security)
    {
        securities.add(security);
    }

    public void addSecurities(Collection<Security> sec)
    {
        securities.addAll(sec);
    }

    public void removeSecurity(final Security security)
    {
        for (Watchlist w : watchlists)
            w.getSecurities().remove(security);
        deleteInvestmentPlans(security);
        deleteTaxonomyAssignments(security);
        deleteAccountTransactions(security);
        deletePortfolioTransactions(security);
        securities.remove(security);
    }

    public List<Watchlist> getWatchlists()
    {
        return watchlists;
    }

    public List<ConsumerPriceIndex> getConsumerPriceIndeces()
    {
        return Collections.unmodifiableList(consumerPriceIndeces);
    }

    public void setConsumerPriceIndeces(List<ConsumerPriceIndex> prices)
    {
        this.consumerPriceIndeces = prices;
        Collections.sort(this.consumerPriceIndeces, new ConsumerPriceIndex.ByDate());
    }

    public void addConsumerPriceIndex(ConsumerPriceIndex record)
    {
        consumerPriceIndeces.add(record);
    }

    public void removeConsumerPriceIndex(ConsumerPriceIndex record)
    {
        consumerPriceIndeces.remove(record);
    }

    public void addAccount(Account account)
    {
        accounts.add(account);
    }

    public void removeAccount(Account account)
    {
        deleteCrossEntries(account.getTransactions());
        deleteInvestmentPlans(account);
        deleteTaxonomyAssignments(account);
        accounts.remove(account);
    }

    public List<Account> getAccounts()
    {
        return Collections.unmodifiableList(accounts);
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
        return Collections.unmodifiableList(portfolios);
    }

    @Deprecated
    /* package */Category getRootCategory()
    {
        return this.rootCategory;
    }

    @Deprecated
    /* package */void setRootCategory(Category rootCategory)
    {
        this.rootCategory = rootCategory;
    }

    @Deprecated
    /* package */String getIndustryTaxonomy()
    {
        return industryTaxonomyId;
    }

    @Deprecated
    /* package */void setIndustryTaxonomy(String industryTaxonomyId)
    {
        this.industryTaxonomyId = industryTaxonomyId;
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

    public Taxonomy getTaxonomy(String id)
    {
        for (Taxonomy t : taxonomies)
        {
            if (id.equals(t.getId()))
                return t;
        }
        return null;
    }

    public void setProperty(String key, String value)
    {
        String oldValue = properties.put(key, value);
        propertyChangeSupport.firePropertyChange("properties", oldValue, value); //$NON-NLS-1$
    }

    public void removeProperty(String key)
    {
        String oldValue = properties.remove(key);
        propertyChangeSupport.firePropertyChange("properties", oldValue, null); //$NON-NLS-1$
    }

    public String getProperty(String key)
    {
        return properties.get(key);
    }

    /* package */SecretKey getSecret()
    {
        return secret;
    }

    /* package */void setSecret(SecretKey secret)
    {
        this.secret = secret;
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

    private void deleteTaxonomyAssignments(final InvestmentVehicle vehicle)
    {
        for (Taxonomy taxonomy : taxonomies)
        {
            taxonomy.foreach(new Taxonomy.Visitor()
            {
                @Override
                public void visit(Classification classification, Assignment assignment)
                {
                    if (vehicle.equals(assignment.getInvestmentVehicle()))
                        classification.removeAssignment(assignment);
                }
            });
        }
    }

    private void deleteAccountTransactions(Security security)
    {
        for (Account account : accounts)
        {
            for (AccountTransaction t : new ArrayList<AccountTransaction>(account.getTransactions()))
            {
                if (t.getSecurity() == null || !security.equals(t.getSecurity()))
                    continue;

                if (t.getCrossEntry() != null)
                    t.getCrossEntry().delete();
                else
                    account.getTransactions().remove(t);
            }

        }
    }

    private void deletePortfolioTransactions(Security security)
    {
        for (Portfolio portfolio : portfolios)
        {
            for (PortfolioTransaction t : new ArrayList<PortfolioTransaction>(portfolio.getTransactions()))
            {
                if (!security.equals(t.getSecurity()))
                    continue;

                if (t.getCrossEntry() != null)
                    t.getCrossEntry().delete();
                else
                    portfolio.getTransactions().remove(t);
            }

        }
    }

    public void markDirty()
    {
        propertyChangeSupport.firePropertyChange("dirty", false, true); //$NON-NLS-1$
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
