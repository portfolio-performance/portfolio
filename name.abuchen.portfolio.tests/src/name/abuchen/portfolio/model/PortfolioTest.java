package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import name.abuchen.portfolio.model.PortfolioTransaction.Type;

import org.junit.Before;
import org.junit.Test;

public class PortfolioTest
{
    private Client client;
    private Portfolio portfolio;

    private PortfolioTransaction transactionWithPlan;
    private PortfolioTransaction transaction;

    @Before
    public void createClient()
    {
        client = new Client();
        portfolio = new Portfolio();
        client.addPortfolio(portfolio);

        Security security = new Security();
        security.setName("Some security"); //$NON-NLS-1$
        client.addSecurity(security);

        InvestmentPlan plan = new InvestmentPlan("Some plan"); //$NON-NLS-1$
        client.addPlan(plan);

        buildTransaction(security);
        buildTransactionWithPlan(security, plan);
    }

    private void buildTransaction(Security security)
    {
        transaction = new PortfolioTransaction();
        transaction.setType(Type.DELIVERY_INBOUND);
        transaction.setDateTime(LocalDate.now().atStartOfDay());
        transaction.setSecurity(security);

        portfolio.addTransaction(transaction);
    }

    private void buildTransactionWithPlan(Security security, InvestmentPlan plan)
    {
        transactionWithPlan = new PortfolioTransaction();
        transactionWithPlan.setType(Type.DELIVERY_INBOUND);
        transactionWithPlan.setDateTime(LocalDate.now().atStartOfDay());
        transactionWithPlan.setSecurity(security);

        plan.getTransactions().add(transactionWithPlan);
        portfolio.addTransaction(transactionWithPlan);
    }

    @Test
    public void testShallowDelete()
    {
        portfolio.deleteTransaction(transaction, client);

        assertThat(portfolio.getTransactions(), not(hasItem(transaction)));

        assertThat(portfolio.getTransactions(), hasItem(transactionWithPlan));
        assertThat(client.getPlans().get(0).getTransactions().size(), is(1));
    }

    @Test
    public void testShallowDeleteWithTransaction()
    {
        portfolio.deleteTransaction(transactionWithPlan, client);

        assertThat(portfolio.getTransactions(), not(hasItem(transactionWithPlan)));
        assertThat(client.getPlans().get(0).getTransactions(), not(hasItem(transactionWithPlan)));

        assertThat(portfolio.getTransactions(), hasItem(transaction));
    }

}
