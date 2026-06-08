package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Verifies that all relevant object references are remapped to reused target
 * securities before imported objects are appended to the merged client. These
 * tests focus on reference integrity across portfolios, watchlists, investment
 * plans, and intentionally ignored out-of-scope structures.
 */

public class ClientMergeServiceReferenceRemappingTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Verifies that portfolio transactions are remapped to the reused target
     * security.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemapPortfolioTransactionToReusedSecurity() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        Portfolio sourcePortfolio = createPortfolio("Source Portfolio"); //$NON-NLS-1$
        PortfolioTransaction tx = new PortfolioTransaction();
        tx.setType(PortfolioTransaction.Type.BUY);
        tx.setSecurity(sourceSecurity);
        sourcePortfolio.addTransaction(tx);
        source.addPortfolio(sourcePortfolio);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Portfolio mergedPortfolio = result.getClient().getPortfolios().get(0);
        PortfolioTransaction mergedTransaction = mergedPortfolio.getTransactions().get(0);

        assertSame(baseSecurity, mergedTransaction.getSecurity());
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
    }

    /**
     * Verifies that watchlist entries are remapped to the reused target
     * security.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemapWatchlistEntryToReusedSecurity() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        Watchlist watchlist = createWatchlist("Source Watchlist"); //$NON-NLS-1$
        watchlist.addSecurity(sourceSecurity);
        source.addWatchlist(watchlist);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Watchlist mergedWatchlist = result.getClient().getWatchlists().get(0);

        assertThat(mergedWatchlist.getSecurities().size(), is(1));
        assertSame(baseSecurity, mergedWatchlist.getSecurities().get(0));
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
    }

    /**
     * Verifies that the main security reference of an investment plan is
     * remapped to the reused target security.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemapInvestmentPlanSecurityToReusedSecurity() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        InvestmentPlan plan = createInvestmentPlan("Plan A", sourceSecurity); //$NON-NLS-1$
        source.addPlan(plan);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        InvestmentPlan mergedPlan = result.getClient().getPlans().get(0);

        assertSame(baseSecurity, mergedPlan.getSecurity());
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
    }

    /**
     * Verifies that security references inside investment plan transactions are
     * also remapped to the reused target security.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemapInvestmentPlanTransactionsToReusedSecurity() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        InvestmentPlan plan = createInvestmentPlan("Plan A", sourceSecurity); //$NON-NLS-1$

        Transaction planTransaction = new AccountTransaction();
        planTransaction.setSecurity(sourceSecurity);
        plan.getTransactions().add(planTransaction);

        source.addPlan(plan);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        InvestmentPlan mergedPlan = result.getClient().getPlans().get(0);
        Transaction mergedPlanTransaction = mergedPlan.getTransactions().get(0);

        assertSame(baseSecurity, mergedPlan.getSecurity());
        assertSame(baseSecurity, mergedPlanTransaction.getSecurity());
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
    }

    /**
     * Verifies that taxonomies from additional files are intentionally ignored.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void shouldIgnoreTaxonomiesFromAdditionalFiles() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Taxonomy taxonomy = new Taxonomy();
        taxonomy.setName("Ignored Taxonomy"); //$NON-NLS-1$
        source.addTaxonomy(taxonomy);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getClient().getTaxonomies().size(), is(0));
    }

    /**
     * Verifies that dashboards from additional files are intentionally ignored.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void shouldIgnoreDashboardsFromAdditionalFiles() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Dashboard dashboard = new Dashboard();
        dashboard.setName("Ignored Dashboard"); //$NON-NLS-1$
        source.addDashboard(dashboard);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getClient().getDashboards().count(), is(0L));
    }

    private Security createSecurity(String name, String isin, String wkn, String currency)
    {
        Security security = new Security();
        security.setName(name);
        security.setIsin(isin);
        security.setWkn(wkn);
        security.setCurrencyCode(currency);
        return security;
    }

    private Portfolio createPortfolio(String name)
    {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        return portfolio;
    }

    private Watchlist createWatchlist(String name)
    {
        Watchlist watchlist = new Watchlist();
        watchlist.setName(name);
        return watchlist;
    }

    private InvestmentPlan createInvestmentPlan(String name, Security security)
    {
        InvestmentPlan plan = new InvestmentPlan();
        plan.setName(name);
        plan.setSecurity(security);
        return plan;
    }
}
