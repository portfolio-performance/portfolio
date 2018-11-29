package name.abuchen.portfolio.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.money.CurrencyUnit;

public class Client
{
    /* package */static final int MAJOR_VERSION = 1;

    public static final int CURRENT_VERSION = 39;
    public static final int VERSION_WITH_CURRENCY_SUPPORT = 29;

    private transient PropertyChangeSupport propertyChangeSupport;

    /**
     * The (minor) version of the file format. If it is lower than the current
     * version, then {@link ClientFactory#upgradeModel} will upgrade the model
     * and set the version number to the current version.
     */
    private int version = CURRENT_VERSION;

    /**
     * The (minor) version of the file format as it has been read from file.
     */
    private transient int fileVersionAfterRead = CURRENT_VERSION;

    private String baseCurrency = CurrencyUnit.EUR;

    private List<Security> securities = new ArrayList<>();
    private List<Watchlist> watchlists;

    // keep typo -> xstream deserialization
    private List<ConsumerPriceIndex> consumerPriceIndeces;

    private List<Account> accounts = new ArrayList<>();
    private List<Portfolio> portfolios = new ArrayList<>();
    private List<InvestmentPlan> plans;
    private List<Taxonomy> taxonomies;
    private List<Dashboard> dashboards;

    private Map<String, String> properties;
    private ClientSettings settings;

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
            watchlists = new ArrayList<>();

        if (consumerPriceIndeces == null)
            consumerPriceIndeces = new ArrayList<>();

        if (properties == null)
            properties = new HashMap<>();

        if (propertyChangeSupport == null)
            propertyChangeSupport = new PropertyChangeSupport(this);

        if (plans == null)
            plans = new ArrayList<>();

        if (taxonomies == null)
            taxonomies = new ArrayList<>();

        if (dashboards == null)
            dashboards = new ArrayList<>();

        if (settings == null)
            settings = new ClientSettings();
        else
            settings.doPostLoadInitialization();
    }

    /* package */int getVersion()
    {
        return version;
    }

    /* package */void setVersion(int version)
    {
        this.version = version;
    }

    public int getFileVersionAfterRead()
    {
        return fileVersionAfterRead;
    }

    /* package */
    void setFileVersionAfterRead(int fileVersionAfterRead)
    {
        this.fileVersionAfterRead = fileVersionAfterRead;
    }

    public String getBaseCurrency()
    {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency)
    {
        propertyChangeSupport.firePropertyChange("baseCurrency", this.baseCurrency, this.baseCurrency = baseCurrency); //$NON-NLS-1$
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

    /**
     * Returns a sorted list of active securities, i.e. securities that are not
     * marked as retired.
     */
    public List<Security> getActiveSecurities()
    {
        return securities.stream() //
                        .filter(s -> s.getCurrencyCode() != null) //
                        .filter(s -> !s.isRetired()) //
                        .sorted(new Security.ByName()) //
                        .collect(Collectors.toList());
    }

    public void addSecurity(Security security)
    {
        Objects.requireNonNull(security);

        securities.add(security);

        propertyChangeSupport.firePropertyChange("securities", null, security); //$NON-NLS-1$
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

        propertyChangeSupport.firePropertyChange("securities", security, null); //$NON-NLS-1$
    }

    /**
     * Gets a list of used {@link CurrencyUnit}s.
     * 
     * @return list
     */
    public List<CurrencyUnit> getUsedCurrencies()
    {
        // collect all used currency codes
        HashSet<String> hsUsedCodes = new HashSet<String>();
        // first client and all accounts
        hsUsedCodes.add(baseCurrency);
        for (Account account : accounts)
        {
            hsUsedCodes.add(account.getCurrencyCode());
        }
        // then portfolios
        for (Portfolio portfolio : portfolios)
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                hsUsedCodes.add(t.getCurrencyCode());
            }
        }
        // then from all securities
        for (Security security : securities)
        {
            hsUsedCodes.add(security.getCurrencyCode());
        }
        // now get the currency units
        List<CurrencyUnit> lUnits = new ArrayList<CurrencyUnit>();
        for (String code : hsUsedCodes)
        {
            CurrencyUnit unit = CurrencyUnit.getInstance(code);
            if (unit != null)
            {
                lUnits.add(unit);
            }
        }
        // sort list to allow using it as a favorite list
        Collections.sort(lUnits);
        return lUnits;
    }

    public List<Watchlist> getWatchlists()
    {
        return watchlists;
    }

    public List<ConsumerPriceIndex> getConsumerPriceIndices()
    {
        return Collections.unmodifiableList(consumerPriceIndeces);
    }

    /**
     * Sets the consumer price indices.
     * 
     * @return true if the indices are modified.
     */
    public boolean setConsumerPriceIndices(List<ConsumerPriceIndex> indices)
    {
        if (indices == null)
            throw new IllegalArgumentException();

        List<ConsumerPriceIndex> newValues = new ArrayList<>(indices);
        Collections.sort(newValues, new ConsumerPriceIndex.ByDate());

        if (consumerPriceIndeces == null || !consumerPriceIndeces.equals(newValues))
        {
            // only assign list if indices have actually changed because UI
            // elements keep a reference which is not updated if no 'dirty'
            // event is fired
            this.consumerPriceIndeces = newValues;
            return true;
        }
        else
        {
            return false;
        }
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
        deleteReferenceAccount(account);
        deleteTransactions(account);
        deleteInvestmentPlans(account);
        deleteTaxonomyAssignments(account);
        accounts.remove(account);
    }

    public List<Account> getAccounts()
    {
        return Collections.unmodifiableList(accounts);
    }

    /**
     * Returns a sorted list of active accounts, i.e. accounts that are not
     * marked as retired.
     */
    public List<Account> getActiveAccounts()
    {
        return accounts.stream() //
                        .filter(a -> !a.isRetired()) //
                        .sorted(new Account.ByName()) //
                        .collect(Collectors.toList());
    }

    public void addPortfolio(Portfolio portfolio)
    {
        portfolios.add(portfolio);
    }

    public void removePortfolio(Portfolio portfolio)
    {
        deleteTransactions(portfolio);
        deleteInvestmentPlans(portfolio);
        portfolios.remove(portfolio);
    }

    public List<Portfolio> getPortfolios()
    {
        return Collections.unmodifiableList(portfolios);
    }

    /**
     * Returns a sorted list of active portfolios, i.e. portfolios that are not
     * marked as retired.
     */
    public List<Portfolio> getActivePortfolios()
    {
        return portfolios.stream() //
                        .filter(p -> !p.isRetired()) //
                        .sorted(new Portfolio.ByName()) //
                        .collect(Collectors.toList());
    }

    @Deprecated
    /* package */
    Category getRootCategory()
    {
        return this.rootCategory;
    }

    @Deprecated
    /* package */
    void setRootCategory(Category rootCategory)
    {
        this.rootCategory = rootCategory;
    }

    @Deprecated
    /* package */
    String getIndustryTaxonomy()
    {
        return industryTaxonomyId;
    }

    @Deprecated
    /* package */
    void setIndustryTaxonomy(String industryTaxonomyId)
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

    public void addTaxonomy(int index, Taxonomy taxonomy)
    {
        taxonomies.add(index, taxonomy);
    }

    public void removeTaxonomy(Taxonomy taxonomy)
    {
        taxonomies.remove(taxonomy);
    }

    public Taxonomy getTaxonomy(String id)
    {
        return taxonomies.stream() //
                        .filter(t -> id.equals(t.getId())) //
                        .findAny().orElse(null);
    }

    public Stream<Dashboard> getDashboards()
    {
        return dashboards.stream();
    }

    public void addDashboard(Dashboard dashboard)
    {
        this.dashboards.add(dashboard);
    }

    public void addDashboard(int index, Dashboard dashboard)
    {
        this.dashboards.add(index, dashboard);
    }

    public void removeDashboard(Dashboard dashboard)
    {
        this.dashboards.remove(dashboard);
    }

    public ClientSettings getSettings()
    {
        return settings;
    }

    public void setProperty(String key, String value)
    {
        String oldValue = properties.put(key, value);
        propertyChangeSupport.firePropertyChange("properties", oldValue, value); //$NON-NLS-1$
    }

    public String removeProperty(String key)
    {
        String oldValue = properties.remove(key);
        propertyChangeSupport.firePropertyChange("properties", oldValue, null); //$NON-NLS-1$
        return oldValue;
    }

    public String getProperty(String key)
    {
        return properties.get(key);
    }

    /* package */void clearProperties()
    {
        properties.clear();
    }

    /* package */
    SecretKey getSecret()
    {
        return secret;
    }

    /* package */
    void setSecret(SecretKey secret)
    {
        this.secret = secret;
    }

    /**
     * Removes the given account as reference account from any portfolios. As
     * the model expects that there is always a reference account, an arbitrary
     * other account is picked as reference account instead. Or, if no other
     * account exists, a new account is created and used as reference account.
     */
    private void deleteReferenceAccount(Account account)
    {
        for (Portfolio portfolio : portfolios)
        {
            if (account.equals(portfolio.getReferenceAccount()))
            {
                portfolio.setReferenceAccount(null);

                accounts.stream().filter(a -> !account.equals(a)).findAny().ifPresent(portfolio::setReferenceAccount);

                if (portfolio.getReferenceAccount() == null)
                {
                    Account referenceAccount = new Account();
                    referenceAccount.setName(MessageFormat.format(Messages.LabelDefaultReferenceAccountName,
                                    portfolio.getName()));
                    addAccount(referenceAccount);
                    portfolio.setReferenceAccount(referenceAccount);
                }
            }
        }
    }

    /**
     * Delete all transactions including cross entries and transactions created
     * by an investment plan.
     */
    private <T extends Transaction> void deleteTransactions(TransactionOwner<T> owner)
    {
        // use a copy because #removeTransaction modifies the list
        for (T t : new ArrayList<T>(owner.getTransactions()))
            owner.deleteTransaction(t, this);
    }

    private void deleteInvestmentPlans(Portfolio portfolio)
    {
        for (InvestmentPlan plan : new ArrayList<InvestmentPlan>(plans))
        {
            if (portfolio.equals(plan.getPortfolio()))
                removePlan(plan);
        }
    }

    private void deleteInvestmentPlans(Account account)
    {
        for (InvestmentPlan plan : new ArrayList<InvestmentPlan>(plans))
        {
            if (account.equals(plan.getAccount()))
                removePlan(plan);
        }
    }

    private void deleteInvestmentPlans(Security security)
    {
        for (InvestmentPlan plan : new ArrayList<InvestmentPlan>(plans))
        {
            if (security.equals(plan.getSecurity()))
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

                account.deleteTransaction(t, this);
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

                portfolio.deleteTransaction(t, this);
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

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public String debugTransactionsToString()
    {
        StringBuilder answer = new StringBuilder();

        for (Portfolio portfolio : portfolios)
        {
            answer.append(portfolio.getName()).append('\n');
            portfolio.getTransactions().stream().sorted(new Transaction.ByDate())
                            .forEach(t -> answer.append(t).append('\n'));
        }

        for (Account account : accounts)
        {
            answer.append(account.getName()).append('\n');
            account.getTransactions().stream().sorted(new Transaction.ByDate())
                            .forEach(t -> answer.append(t).append('\n'));
        }

        return answer.toString();
    }

}
