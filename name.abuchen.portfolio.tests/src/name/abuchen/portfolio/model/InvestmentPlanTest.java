package name.abuchen.portfolio.model;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

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
        investmentPlan.setAccount(account); // set both account and portfolio
        investmentPlan.setPortfolio(portfolio); // causes securities to be
                                                // bought
        investmentPlan.setSecurity(security);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setInterval(1);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.size(), is(2));
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(1), instanceOf(PortfolioTransaction.class));

        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-05-02T00:00")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
        assertThat(((PortfolioTransaction) tx.get(1)).getType(), is(PortfolioTransaction.Type.BUY));

        // check that delta generation of transactions also takes into account
        // the transaction "spilled over" into the next month

        investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isAfter(LocalDateTime.parse("2016-05-10T00:00")))
                        .collect(Collectors.toList())
                        .forEach(t -> investmentPlan.removeTransaction((PortfolioTransaction) t));

        List<Transaction> newlyGenerated = investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(newlyGenerated.isEmpty(), is(false));
        assertThat(newlyGenerated.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(newlyGenerated.get(0).getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
    }

    @Test
    public void testGenerationOfDeliveryTransaction()
    {
        Client client = new Client();
        Security security = new SecurityBuilder().addTo(client);
        Account account = new AccountBuilder().addTo(client);
        Portfolio portfolio = new PortfolioBuilder(account).addTo(client);

        InvestmentPlan investmentPlan = new InvestmentPlan();
        // investmentPlan.setAccount(account); // set portfolio only
        investmentPlan.setPortfolio(portfolio); // causes securities to be
                                                // delivered in
        investmentPlan.setSecurity(security);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setInterval(1);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.size(), is(2));
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-05-02T00:00")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(tx.get(1), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
        assertThat(((PortfolioTransaction) tx.get(1)).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
    }

    @Test
    public void testGenerationOfDepositTransaction()
    {
        Client client = new Client();
        Account account = new AccountBuilder().addTo(client);

        InvestmentPlan investmentPlan = new InvestmentPlan();
        investmentPlan.setAccount(account);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setInterval(1);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.isEmpty(), is(false));
        assertThat(tx.size(), is(2));

        assertThat(tx.get(0), instanceOf(AccountTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-05-02T00:00")));
        assertThat(((AccountTransaction) tx.get(0)).getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(tx.get(1), instanceOf(AccountTransaction.class));
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
        assertThat(((AccountTransaction) tx.get(1)).getType(), is(AccountTransaction.Type.DEPOSIT));
    }

    @Test
    public void testNoGenerationWithStartInFuture()
    {
        Client client = new Client();
        Account account = new AccountBuilder().addTo(client);

        InvestmentPlan investmentPlan = new InvestmentPlan();
        investmentPlan.setAccount(account);
        investmentPlan.setAmount(Values.Amount.factorize(100));
        investmentPlan.setInterval(1);
        investmentPlan.setStart(LocalDate.now().minusMonths(6));
        investmentPlan.setInterval(12);

        investmentPlan.generateTransactions(new TestCurrencyConverter());
        int previousTransactionCount = investmentPlan.getTransactions().size();

        // given is an investment plan with existing transactions,
        // the user changes the start date to be in the future

        investmentPlan.setStart(LocalDate.now().plusMonths(12));
        investmentPlan.setInterval(1);
        investmentPlan.generateTransactions(new TestCurrencyConverter());

        // no new transactions should be created until this date
        assertThat(investmentPlan.getTransactions(), hasSize(previousTransactionCount));

        // generation resumes at start date
        LocalDate resumeDate = LocalDate.now().minusMonths(1).minusDays(10);
        investmentPlan.setStart(resumeDate);
        investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(investmentPlan.getTransactions(), hasSize(previousTransactionCount + 2));

        LocalDate firstNewDate = investmentPlan.getTransactions().get(previousTransactionCount).getDateTime()
                        .toLocalDate();

        // if resume date is on a bank holiday, move the resume date to the next
        // non-holiday
        TradeCalendar tradeCalendar = TradeCalendarManager.getDefaultInstance();
        while (tradeCalendar.isHoliday(resumeDate))
            resumeDate = resumeDate.plusDays(1);

        assertThat(firstNewDate, is(resumeDate));
    }

}
