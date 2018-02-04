package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class InvestmentPlanTest
{

    @Test
    public void testGenerationOfBuyTransaction()
    {
        Client client = new Client();
        Security security = new SecurityBuilder().addTo(client);
        Account account = new AccountBuilder().addTo(client);
        Portfolio portfolio = new PortfolioBuilder(account).addTo(client);

        InvestmentPlan investmentPlan = new InvestmentPlan();
        investmentPlan.setAccount(account);       // set both account and portfolio
        investmentPlan.setPortfolio(portfolio);   // causes securities to be bought
        investmentPlan.setSecurity(security);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setInterval(1);
        investmentPlan.setStart(LocalDate.parse("2016-01-31"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDate().isBefore(LocalDate.parse("2017-04-10"))).collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDate().getYear() == 2016 && t.getDate().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.size(), is(2));
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(1), instanceOf(PortfolioTransaction.class));

        assertThat(tx.get(0).getDate(), is(LocalDate.parse("2016-05-02")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(tx.get(1).getDate(), is(LocalDate.parse("2016-05-31")));
        assertThat(((PortfolioTransaction) tx.get(1)).getType(), is(PortfolioTransaction.Type.BUY));

        // check that delta generation of transactions also takes into account
        // the transaction "spilled over" into the next month

        investmentPlan.getTransactions().stream().filter(t -> t.getDate().isAfter(LocalDate.parse("2016-05-10")))
                        .collect(Collectors.toList()).forEach(t -> investmentPlan.removeTransaction((PortfolioTransaction) t));

        List<Transaction> newlyGenerated = investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(newlyGenerated.isEmpty(), is(false));
        assertThat(newlyGenerated.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(newlyGenerated.get(0).getDate(), is(LocalDate.parse("2016-05-31")));
    }

    @Test
    public void testGenerationOfDeliveryTransaction()
    {
        Client client = new Client();
        Security security = new SecurityBuilder().addTo(client);
        Account account = new AccountBuilder().addTo(client);
        Portfolio portfolio = new PortfolioBuilder(account).addTo(client);

        InvestmentPlan investmentPlan = new InvestmentPlan();
        //investmentPlan.setAccount(account);     // set portfolio only
        investmentPlan.setPortfolio(portfolio);   //  causes securities to be delivered in
        investmentPlan.setSecurity(security);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setInterval(1);
        investmentPlan.setStart(LocalDate.parse("2016-01-31"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDate().isBefore(LocalDate.parse("2017-04-10"))).collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDate().getYear() == 2016 && t.getDate().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.size(), is(2));
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(0).getDate(), is(LocalDate.parse("2016-05-02")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(tx.get(1), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(1).getDate(), is(LocalDate.parse("2016-05-31")));
        assertThat(((PortfolioTransaction) tx.get(1)).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
    }

    @Test
    public void testGenerationOfDepositTransaction()
    {
        Client client = new Client();
        Security security = new SecurityBuilder().addTo(client);
        Account account = new AccountBuilder().addTo(client);
        Portfolio portfolio = new PortfolioBuilder(account).addTo(client);

        InvestmentPlan investmentPlan = new InvestmentPlan();
        investmentPlan.setAccount(account);        // set portfolio only
        //investmentPlan.setPortfolio(portfolio);  //  causes deposit to be made
        investmentPlan.setSecurity(security);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setInterval(1);
        investmentPlan.setStart(LocalDate.parse("2016-01-31"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDate().isBefore(LocalDate.parse("2017-04-10"))).collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDate().getYear() == 2016 && t.getDate().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.isEmpty(), is(false));
        assertThat(tx.size(), is(2));

        assertThat(tx.get(0), instanceOf(AccountTransaction.class));
        assertThat(tx.get(0).getDate(), is(LocalDate.parse("2016-05-02")));
        assertThat(((AccountTransaction) tx.get(0)).getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(tx.get(1), instanceOf(AccountTransaction.class));
        assertThat(tx.get(1).getDate(), is(LocalDate.parse("2016-05-31")));
        assertThat(((AccountTransaction) tx.get(1)).getType(), is(AccountTransaction.Type.DEPOSIT));
    }
}
