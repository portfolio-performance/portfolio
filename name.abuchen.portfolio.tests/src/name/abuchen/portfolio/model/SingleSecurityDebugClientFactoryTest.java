package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.support.SingleSecurityDebugClientFactory;

@SuppressWarnings("nls")
public class SingleSecurityDebugClientFactoryTest
{
    private Client client;
    private Security securityA;
    private Security securityB;
    private Account accountA;
    private Portfolio portfolio;
    private AttributeType attributeType;

    @Before
    public void setup()
    {
        client = new Client();

        securityA = new SecurityBuilder(CurrencyUnit.EUR) //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        securityA.setName("Alpha AG");
        securityA.setWkn("WKN_A");
        securityA.setNote("private note on security");

        securityB = new SecurityBuilder(CurrencyUnit.EUR) //
                        .addPrice("2020-01-01", Values.Quote.factorize(50)) //
                        .addTo(client);
        securityB.setName("Beta Inc");

        // custom attribute on the kept security
        attributeType = new AttributeType("attr-1");
        attributeType.setName("Broker");
        attributeType.setColumnLabel("Broker");
        attributeType.setType(String.class);
        attributeType.setConverter(AttributeType.StringConverter.class);
        attributeType.setTarget(Security.class);
        client.getSettings().addAttributeType(attributeType);
        securityA.getAttributes().put(attributeType, "My Broker");

        accountA = new AccountBuilder(CurrencyUnit.EUR) //
                        .deposit_("2020-01-01", Values.Amount.factorize(10000)) //
                        .interest("2020-03-01", Values.Amount.factorize(5)) //
                        .dividend("2020-04-01", Values.Amount.factorize(20), Values.Amount.factorize(3), securityA) //
                        .addTo(client);
        accountA.setName("My Real Account");

        new AccountBuilder(CurrencyUnit.EUR) //
                        .deposit_("2020-01-01", Values.Amount.factorize(5000)) //
                        .dividend("2020-04-01", Values.Amount.factorize(10), securityB) //
                        .addTo(client);

        portfolio = new PortfolioBuilder(accountA) //
                        .buy(securityA, "2020-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .buy(securityB, "2020-01-15", Values.Share.factorize(10), Values.Amount.factorize(500)) //
                        .addTo(client);
        portfolio.setName("My Real Portfolio");

        // taxonomy, watchlist, dashboard, investment plan, property, bookmark
        TaxonomyBuilder taxonomy = new TaxonomyBuilder();
        taxonomy.addTo(client);

        Watchlist watchlist = new Watchlist();
        watchlist.setName("Favorites");
        watchlist.getSecurities().add(securityA);
        watchlist.getSecurities().add(securityB);
        client.addWatchlist(watchlist);

        Dashboard dashboard = new Dashboard("dashboard-1");
        dashboard.setName("My Dashboard");
        client.addDashboard(dashboard);

        InvestmentPlan plan = new InvestmentPlan("Savings Plan");
        plan.setSecurity(securityA);
        plan.setPortfolio(portfolio);
        plan.setAccount(accountA);
        client.addPlan(plan);

        client.setProperty("some-key", "some-value");
        client.getSettings().insertBookmark(0, new Bookmark("My Bookmark", "https://example.com/{tickerSymbol}"));

        // table/view column configuration - kept on purpose
        client.getSettings().getConfigurationSet("securities-view-columns")
                        .add(new ConfigurationSet.Configuration("My Columns", "col1,col2,col3"));

        // notes and sources on transactions
        client.getAccounts().forEach(a -> a.getTransactions().forEach(t -> {
            t.setNote("account transaction note");
            t.setSource("broker-export.pdf");
        }));
        client.getPortfolios().forEach(p -> p.getTransactions().forEach(t -> {
            t.setNote("portfolio transaction note");
            t.setSource("broker-export.pdf");
        }));
    }

    @Test
    public void testOnlyTheChosenSecuritySurvives() throws IOException
    {
        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        assertThat(copy.getSecurities(), hasSize(1));
        Security kept = copy.getSecurities().get(0);
        assertThat(kept.getName(), is("Alpha AG"));
        assertThat(kept.getIsin(), is(securityA.getIsin()));
        assertThat(kept.getWkn(), is("WKN_A"));
        assertThat(kept.getTickerSymbol(), is(securityA.getTickerSymbol()));
        assertThat(kept.getPrices(), hasSize(2));
    }

    @Test
    public void testSecurityIdentifiersAreStripped() throws IOException
    {
        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        Security kept = copy.getSecurities().get(0);
        assertThat(kept.getNote(), is(nullValue()));
        assertThat(kept.getAttributes().isEmpty(), is(true));
    }

    @Test
    public void testAccountsAndPortfoliosAreRenamedAndPruned() throws IOException
    {
        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        // accountB was only used by securityB (plus a deposit) -> pruned
        assertThat(copy.getAccounts(), hasSize(1));
        assertThat(copy.getPortfolios(), hasSize(1));

        assertThat(copy.getAccounts().get(0).getName(), is("Account 1"));
        assertThat(copy.getPortfolios().get(0).getName(), is("Portfolio 1"));

        assertThat(copy.getAccounts().get(0).getNote(), is(nullValue()));
        assertThat(copy.getPortfolios().get(0).getNote(), is(nullValue()));
    }

    @Test
    public void testOnlySecurityRelatedTransactionsRemain() throws IOException
    {
        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        // portfolio: only the buy of securityA
        List<PortfolioTransaction> portfolioTx = copy.getPortfolios().get(0).getTransactions();
        assertThat(portfolioTx, hasSize(1));
        assertThat(portfolioTx.get(0).getSecurity().getName(), is("Alpha AG"));

        // account: the buy cross entry + the dividend of securityA;
        // deposit and interest are stripped
        List<AccountTransaction> accountTx = copy.getAccounts().get(0).getTransactions();
        assertThat(accountTx, hasSize(2));
        accountTx.forEach(t -> assertThat(t.getSecurity().getName(), is("Alpha AG")));
    }

    @Test
    public void testTransactionValuesArePreservedExactly() throws IOException
    {
        PortfolioTransaction original = client.getPortfolios().get(0).getTransactions().stream()
                        .filter(t -> securityA.equals(t.getSecurity())).findFirst().orElseThrow();

        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        PortfolioTransaction result = copy.getPortfolios().get(0).getTransactions().get(0);
        assertThat(result.getType(), is(original.getType()));
        assertThat(result.getShares(), is(original.getShares()));
        assertThat(result.getAmount(), is(original.getAmount()));
        assertThat(result.getDateTime(), is(original.getDateTime()));
    }

    @Test
    public void testNotesAndSourcesAreStripped() throws IOException
    {
        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        copy.getAccounts().forEach(a -> a.getTransactions().forEach(t -> {
            assertThat(t.getNote(), is(nullValue()));
            assertThat(t.getSource(), is(nullValue()));
        }));
        copy.getPortfolios().forEach(p -> p.getTransactions().forEach(t -> {
            assertThat(t.getNote(), is(nullValue()));
            assertThat(t.getSource(), is(nullValue()));
        }));
    }

    @Test
    public void testClientConfigurationIsWiped() throws IOException
    {
        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        assertThat(copy.getTaxonomies(), empty());
        assertThat(copy.getWatchlists(), empty());
        assertThat(copy.getDashboards().count(), is(0L));
        assertThat(copy.getPlans(), empty());
        assertThat(copy.getProperty("some-key"), is(nullValue()));
        assertThat(copy.getSettings().getBookmarks(), empty());
        assertThat(copy.getSettings().getAttributeTypes().count(), is(0L));
        assertThat(copy.getBaseCurrency(), is(CurrencyUnit.EUR));
    }

    @Test
    public void testColumnConfigurationsAreKept() throws IOException
    {
        Client copy = SingleSecurityDebugClientFactory.create(client, securityA);

        assertThat(copy.getSettings().hasConfigurationSet("securities-view-columns"), is(true));

        List<ConfigurationSet.Configuration> configs = copy.getSettings()
                        .getConfigurationSet("securities-view-columns").getConfigurations().toList();
        assertThat(configs, hasSize(1));
        // data is preserved, but the user-given name is anonymized
        assertThat(configs.get(0).getData(), is("col1,col2,col3"));
        assertThat(configs.get(0).getName(), is("#1"));

        // original is untouched
        assertThat(client.getSettings().getConfigurationSet("securities-view-columns").getConfigurations()
                        .toList().get(0).getName(), is("My Columns"));
    }

    @Test
    public void testOriginalClientIsNotModified() throws IOException
    {
        SingleSecurityDebugClientFactory.create(client, securityA);

        assertThat(client.getSecurities(), hasSize(2));
        assertThat(securityA.getNote(), is(notNullValue()));
        assertThat(securityA.getAttributes().isEmpty(), is(false));
        assertThat(client.getAccounts(), hasSize(2));
        assertThat(client.getPortfolios(), hasSize(1));
        assertThat(client.getTaxonomies(), hasSize(1));
        assertThat(client.getPlans(), hasSize(1));
        assertThat(client.getAccounts().get(0).getName(), is("My Real Account"));
        // original transactions still carry their notes
        assertThat(client.getPortfolios().get(0).getTransactions().get(0).getNote(), is(notNullValue()));
    }
}
