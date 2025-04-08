package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

@SuppressWarnings("nls")
public class InvestmentPlanTest
{
    Client client;
    Security security;
    Account account;
    Portfolio portfolio;
    InvestmentPlan investmentPlan;

    @Before
    public void setup()
    {
        this.client = new Client();
        this.security = new SecurityBuilder().addPrice("2015-01-01", Values.Quote.factorize(10)).addTo(client);
        this.account = new AccountBuilder().addTo(client);
        this.portfolio = new PortfolioBuilder(account).addTo(client);
        this.investmentPlan = new InvestmentPlan();
        this.investmentPlan.setAmount(Values.Amount.factorize(100));
        this.investmentPlan.setInterval(1);
    }

    @Test
    public void testGenerationOfBuyTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account); // set both account and portfolio
        investmentPlan.setPortfolio(portfolio); // causes securities to be
                                                // bought
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.PURCHASE_OR_DELIVERY));

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

        List<TransactionPair<?>> newlyGenerated = investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(newlyGenerated.isEmpty(), is(false));
        assertThat(newlyGenerated.get(0).getTransaction(), instanceOf(PortfolioTransaction.class));
        assertThat(newlyGenerated.get(0).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
    }

    @Test
    public void testGenerationOfDeliveryTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        // set portfolio only causes securities to be delivered in
        investmentPlan.setPortfolio(portfolio);

        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.PURCHASE_OR_DELIVERY));

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
    public void testGenerationOfDepositTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.DEPOSIT);

        investmentPlan.setAccount(account);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.DEPOSIT));

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
    public void testGenerationOfRemovalTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.REMOVAL);

        investmentPlan.setAmount(Values.Amount.factorize(100));

        investmentPlan.setAccount(account);
        investmentPlan.setStart(LocalDateTime.parse("2022-03-29T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2022 && t.getDateTime().getMonth() == Month.MARCH)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.REMOVAL));

        assertThat(tx.isEmpty(), is(false));
        assertThat(tx.size(), is(1));

        assertThat(tx.get(0), instanceOf(AccountTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2022-03-29T00:00")));
        assertThat(((AccountTransaction) tx.get(0)).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(((AccountTransaction) tx.get(0)).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100))));
    }

    @Test
    public void testGenerationOfInterestTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.INTEREST);

        investmentPlan.setAmount(Values.Amount.factorize(200));
        investmentPlan.setTaxes(Values.Amount.factorize(10));

        investmentPlan.setAccount(account);
        investmentPlan.setStart(LocalDateTime.parse("2022-03-29T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2022 && t.getDateTime().getMonth() == Month.MARCH)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.INTEREST));

        assertThat(tx.isEmpty(), is(false));
        assertThat(tx.size(), is(1));

        assertThat(tx.get(0), instanceOf(AccountTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2022-03-29T00:00")));
        assertThat(((AccountTransaction) tx.get(0)).getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(((AccountTransaction) tx.get(0)).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200))));
        assertThat(((AccountTransaction) tx.get(0)).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));
    }

    @Test
    public void testNoGenerationWithStartInFuture() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.DEPOSIT);

        investmentPlan.setAccount(account);
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

    @Test(expected = IOException.class)
    public void testErrorMessageWhenNoQuotesExist() throws IOException
    {
        security.removeAllPrices();

        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());
    }

    @Test
    public void testGenerationOfWeeklyBuyTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account); // set both account and portfolio
        investmentPlan.setPortfolio(portfolio); // causes securities to be
                                                // bought
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-01T00:00:00"));

        investmentPlan.setInterval(101); // 101 is weekly, 201 bi-weekly

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        var june2016 = LocalDateTime.parse("2016-06-01T00:00");
        assertThat(investmentPlan.getTransactions().stream().filter(t -> t.getDateTime().isBefore(june2016)).count(),
                        is(22L));

        // Friday 1st January 2016 is a holiday : all transaction shall be on
        // Fridays except on holiday.
        // The real first transaction is Monday 4 January 2016
        var tx = investmentPlan.getTransactions().stream().toList();
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-01-04T00:00")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.BUY));

        // check that the second date is reverted back to Friday
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-01-08T00:00")));

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.PURCHASE_OR_DELIVERY));

        // March 2016 has Friday 25 and Monday 28 as holidays :
        // the March 25th transaction should happen on March 29th
        var txMarch = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MARCH)
                        .toList();
        assertThat(txMarch.size(), is(4));
        assertThat(txMarch.get(0).getDateTime(), is(LocalDateTime.parse("2016-03-04T00:00")));
        assertThat(txMarch.get(1).getDateTime(), is(LocalDateTime.parse("2016-03-11T00:00")));
        assertThat(txMarch.get(2).getDateTime(), is(LocalDateTime.parse("2016-03-18T00:00")));
        assertThat(txMarch.get(3).getDateTime(), is(LocalDateTime.parse("2016-03-29T00:00")));

        // There are 5 Fridays in April 2016 : April should have 5 transaction.
        // Check that the weekly plan is back on Friday 1st April 2016 after the
        // Tuesday 29 March 2016 transaction.
        var txApril = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.APRIL)
                        .toList();
        assertThat(txApril.size(), is(5));
        assertThat(txApril.get(0).getDateTime(), is(LocalDateTime.parse("2016-04-01T00:00")));
        assertThat(txApril.get(1).getDateTime(), is(LocalDateTime.parse("2016-04-08T00:00")));
        assertThat(txApril.get(2).getDateTime(), is(LocalDateTime.parse("2016-04-15T00:00")));
        assertThat(txApril.get(3).getDateTime(), is(LocalDateTime.parse("2016-04-22T00:00")));
        assertThat(txApril.get(4).getDateTime(), is(LocalDateTime.parse("2016-04-29T00:00")));

        // check that generation of transactions also takes into account
        // the Calendar even when regenerated
        investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isAfter(LocalDateTime.parse("2016-03-20T00:00"))).toList()
                        .forEach(t -> investmentPlan.removeTransaction((PortfolioTransaction) t));

        List<TransactionPair<?>> newlyGenerated = investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(newlyGenerated.isEmpty(), is(false));
        assertThat(newlyGenerated.get(0).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-03-29T00:00")));
        assertThat(newlyGenerated.get(1).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-04-01T00:00")));
        assertThat(newlyGenerated.get(2).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-04-08T00:00")));
    }

    @Test
    public void testWeeklyFrom18March2024() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2024-03-18T00:00:00"));

        investmentPlan.setInterval(101); // 101 is weekly, 201 bi-weekly

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        var tx = investmentPlan.getTransactions().stream().toList();
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2024-03-18T00:00")));
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2024-03-25T00:00")));
        assertThat(tx.get(2).getDateTime(), is(LocalDateTime.parse("2024-04-02T00:00")));
        assertThat(tx.get(3).getDateTime(), is(LocalDateTime.parse("2024-04-08T00:00")));
    }

}
