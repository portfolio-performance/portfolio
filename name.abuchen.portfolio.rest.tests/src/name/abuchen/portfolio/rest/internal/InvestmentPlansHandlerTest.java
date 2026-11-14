package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.time.LocalDateTime;

import org.junit.Test;

import com.google.gson.JsonArray;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * The savings plans of a file: rate and rhythm, the accounts a plan moves money
 * between, and the settlement account a buying plan never names itself.
 */
public class InvestmentPlansHandlerTest
{
    private static final String START = "2021-01-04";

    private Account cash;
    private Portfolio portfolio;
    private Security security;

    /** one cash account, one depot settling through it, one instrument */
    private Client client()
    {
        var client = new Client();

        cash = new Account();
        cash.setName("Giro");
        cash.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(cash);

        portfolio = new Portfolio();
        portfolio.setName("Depot");
        portfolio.setReferenceAccount(cash);
        client.addPortfolio(portfolio);

        security = new Security();
        security.setName("Aktie");
        security.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(security);

        return client;
    }

    private InvestmentPlan plan(Client client, String name, InvestmentPlan.Type type, Portfolio portfolio,
                    Account account, int interval, long amount)
    {
        var plan = new InvestmentPlan(name);
        plan.setType(type);
        plan.setPortfolio(portfolio);
        plan.setAccount(account);
        plan.setSecurity(portfolio != null ? security : null);
        plan.setStart(LocalDateTime.of(2021, 1, 4, 0, 0));
        plan.setInterval(interval);
        plan.setAmount(amount);
        plan.setFees(10_00L);
        client.addPlan(plan);
        return plan;
    }

    /** the file keeps a weekly plan as a hundred and more; that is not the reader's business */
    @Test
    public void testTheRhythmArrivesNormalized()
    {
        var client = client();
        plan(client, "monatlich", InvestmentPlan.Type.PURCHASE_OR_DELIVERY, portfolio, null, 1, 500_00L);
        plan(client, "wöchentlich", InvestmentPlan.Type.PURCHASE_OR_DELIVERY, portfolio, null, 102, 100_00L);

        var items = items(client, null, null, null);
        assertThat(items.size(), is(2));

        assertThat(items.get(0).getAsJsonObject().get("interval").getAsInt(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("intervalUnit").getAsString(), is("month"));
        assertThat(items.get(1).getAsJsonObject().get("interval").getAsInt(), is(2));
        assertThat(items.get(1).getAsJsonObject().get("intervalUnit").getAsString(), is("week"));
    }

    /** a plan that buys names no cash account: the answer states the settlement account */
    @Test
    public void testABuyingPlanStatesTheSettlementAccount()
    {
        var client = client();
        plan(client, "Sparplan", InvestmentPlan.Type.PURCHASE_OR_DELIVERY, portfolio, null, 1, 500_00L);

        var item = items(client, null, null, null).get(0).getAsJsonObject();

        assertThat(item.get("type").getAsString(), is("PURCHASE_OR_DELIVERY"));
        assertThat(item.getAsJsonObject("investmentAccount").get("uuid").getAsString(), is(portfolio.getUUID()));
        assertThat(item.getAsJsonObject("cashAccount").get("uuid").getAsString(), is(cash.getUUID()));
        assertThat(item.getAsJsonObject("instrument").get("uuid").getAsString(), is(security.getUUID()));
        assertThat(item.getAsJsonObject("amount").get("value").getAsDouble(), is(500.0d));
        assertThat(item.getAsJsonObject("amount").get("currency").getAsString(), is(CurrencyUnit.EUR));
        assertThat(item.getAsJsonObject("fees").get("value").getAsDouble(), is(10.0d));
        assertThat(item.has("taxes"), is(false));
    }

    /** a plan that only moves money has no securities side to name */
    @Test
    public void testADepositPlanHasNoSecuritiesSide()
    {
        var client = client();
        plan(client, "Freibetrag", InvestmentPlan.Type.DEPOSIT, null, cash, 1, 100_00L);

        var item = items(client, null, null, null).get(0).getAsJsonObject();

        assertThat(item.get("type").getAsString(), is("DEPOSIT"));
        assertThat(item.getAsJsonObject("cashAccount").get("uuid").getAsString(), is(cash.getUUID()));
        assertThat(item.has("investmentAccount"), is(false));
        assertThat(item.has("instrument"), is(false));
    }

    /** the same plan narrows from either side, and a uuid of no use is refused */
    @Test
    public void testTheFiltersNarrowFromEitherSide()
    {
        var client = client();
        plan(client, "Sparplan", InvestmentPlan.Type.PURCHASE_OR_DELIVERY, portfolio, null, 1, 500_00L);
        plan(client, "Freibetrag", InvestmentPlan.Type.DEPOSIT, null, cash, 1, 100_00L);

        assertThat(items(client, portfolio.getUUID(), null, null).size(), is(1));
        assertThat(items(client, portfolio.getUUID(), null, null).get(0).getAsJsonObject().get("name").getAsString(),
                        is("Sparplan"));
        assertThat(items(client, null, cash.getUUID(), null).size(), is(2));
        assertThat(items(client, null, null, security.getUUID()).size(), is(1));

        try
        {
            InvestmentPlansHandler.list(client, null, null, "no-such-instrument"); //$NON-NLS-1$
            fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("instrument"));
        }
    }

    /** a plan that never ran counts no executions and names no last date, but states when it is due */
    @Test
    public void testAPlanThatNeverRanIsStillScheduled()
    {
        var client = client();
        plan(client, "Sparplan", InvestmentPlan.Type.PURCHASE_OR_DELIVERY, portfolio, null, 1, 500_00L);

        var item = items(client, null, null, null).get(0).getAsJsonObject();

        assertThat(item.get("executions").getAsInt(), is(0));
        assertThat(item.has("lastExecution"), is(false));
        assertThat(item.get("nextExecution").getAsString(), is(START));
        assertThat(item.get("start").getAsString(), is(START));
        assertThat(item.get("autoGenerate").getAsBoolean(), is(false));
    }

    /** nothing to answer is two different answers, and an empty array cannot tell them apart */
    @Test
    public void testAnEmptyAnswerSaysWhichKindOfEmptyItIs()
    {
        var client = client();

        var none = InvestmentPlansHandler.list(client, null, null, null);
        assertThat(none.getAsJsonArray("items").size(), is(0));
        assertThat(none.get("empty").getAsString(), is("this file has no investment plans"));

        plan(client, "Sparplan", InvestmentPlan.Type.PURCHASE_OR_DELIVERY, portfolio, null, 1, 500_00L);

        var other = new Security();
        other.setName("Andere");
        other.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(other);

        var unmatched = InvestmentPlansHandler.list(client, null, null, other.getUUID());
        assertThat(unmatched.getAsJsonArray("items").size(), is(0));
        assertThat(unmatched.get("empty").getAsString(),
                        is("no plan of this file matches the account or instrument asked for"));

        // with plans in it and nothing asked for, there is nothing to explain
        assertThat(InvestmentPlansHandler.list(client, null, null, null).has("empty"), is(false));
    }

    private static JsonArray items(Client client, String investmentAccount, String cashAccount, String instrument)
    {
        return InvestmentPlansHandler.list(client, investmentAccount, cashAccount, instrument)
                        .getAsJsonArray("items");
    }
}
