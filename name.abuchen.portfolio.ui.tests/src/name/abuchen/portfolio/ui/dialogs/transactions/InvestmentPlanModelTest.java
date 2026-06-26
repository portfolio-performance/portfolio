package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.jobs.CreateInvestmentPlanTxJob;

@SuppressWarnings("nls")
public class InvestmentPlanModelTest
{

    @Test
    public void testNewRemovalPlan()
    {
        Client client = new Client();
        InvestmentPlanModel model = new InvestmentPlanModel(client, InvestmentPlan.Type.REMOVAL);
        model.setAccount(new Account());
        model.setAmount(100L);
        model.setInterval(1);

        model.applyChanges();

        List<InvestmentPlan> plans = client.getPlans();
        assertThat(plans.size(), is(1));

        InvestmentPlan investmentPlan = plans.get(0);
        assertThat(investmentPlan.getAmount(), is(100L));
        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.REMOVAL));
    }

    @Test
    public void testEditRemovalPlan()
    {
        InvestmentPlan investmentPlan = new InvestmentPlan("Test Plan");
        investmentPlan.setStart(LocalDateTime.parse("2022-03-29T00:00:00"));
        investmentPlan.setAccount(new Account("Test Account"));
        investmentPlan.setAmount(100L);
        investmentPlan.setType(InvestmentPlan.Type.REMOVAL);

        InvestmentPlanModel model = new InvestmentPlanModel(new Client(), InvestmentPlan.Type.REMOVAL);
        model.setSource(investmentPlan);

        assertThat(model.getAmount(), is(100L));
        assertThat(model.getCalculationStatus(), is(Status.OK_STATUS));
    }

    @Test
    public void testAutoGenerationJobHandlesUnsupportedLedgerPlanWithoutMutation() throws InterruptedException
    {
        Client client = new Client();
        Account account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        InvestmentPlan investmentPlan = new InvestmentPlan("Unsupported Plan");
        investmentPlan.setType(InvestmentPlan.Type.REMOVAL);
        investmentPlan.setAccount(account);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setTaxes(Values.Amount.factorize(10));
        investmentPlan.setStart(LocalDateTime.now().minusMonths(1));
        investmentPlan.setInterval(12);
        investmentPlan.setAutoGenerate(true);
        client.addPlan(investmentPlan);

        var job = new CreateInvestmentPlanTxJob(client, new ExchangeRateProviderFactory(client));
        job.schedule();
        job.join();

        assertThat(job.getResult().isOK(), is(true));
        assertThat(account.getTransactions().size(), is(0));
        assertThat(investmentPlan.getLedgerExecutionRefs().size(), is(0));
    }
}
