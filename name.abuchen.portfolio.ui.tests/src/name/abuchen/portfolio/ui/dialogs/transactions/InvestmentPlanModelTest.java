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
}
