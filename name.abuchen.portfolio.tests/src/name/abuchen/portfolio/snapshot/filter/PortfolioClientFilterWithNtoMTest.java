package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.SecurityUtil;

@SuppressWarnings("nls")
public class PortfolioClientFilterWithNtoMTest
{
    private Client client;

    /**
     * Creates three portfolios (A-C) with a reference account each.
     */
    @Before
    public void setupClient()
    {
        client = new Client();

        Security secA = new SecurityBuilder().addTo(client);
        secA.setName("SecA");
        
        Security secB = new SecurityBuilder().addTo(client);
        secB.setName("SecB");
        
        Security secC = new SecurityBuilder().addTo(client);
        secC.setName("SecC");

        SecurityUtil.addDividendEvent(secA, LocalDate.of(2016, 1, 3));
        SecurityUtil.addDividendEvent(secB, LocalDate.of(2016, 1, 3));
        SecurityUtil.addDividendEvent(secC, LocalDate.of(2016, 1, 3));
        
        // accounts
        Account accountA1 = new AccountBuilder() //
                        .deposit_("2016-01-01", Values.Amount.factorize(1000))
                        .dividend("2016-01-03", Values.Amount.factorize(10), Values.Amount.factorize(4), Values.Amount.factorize(2), secA) // dividend without shares count
                        .dividend("2016-01-03", Values.Amount.factorize(12), Values.Share.factorize(12), Values.Amount.factorize(4), Values.Amount.factorize(2), secB) // dividend with shares count
                        .fees____("2016-01-05", Values.Amount.factorize(20), secA) // security fee without shares count
                        .fees____("2016-01-05", Values.Amount.factorize(22), Values.Share.factorize(11), secB) // security fee with shares count
                        .fees____("2016-01-05", Values.Amount.factorize(24)) // fee without a security
                        .fees_refund("2016-01-07", Values.Amount.factorize(40), secA) // security fee refund without shares count
                        .fees_refund("2016-01-07", Values.Amount.factorize(44), Values.Share.factorize(11), secB) // security fee refund with shares count
                        .fees_refund("2016-01-07", Values.Amount.factorize(48)) // fee refund without a security
                        .interest("2016-01-09", Values.Amount.factorize(50), secA) // security interest without shares count
                        .interest("2016-01-09", Values.Amount.factorize(55), Values.Share.factorize(11), secB) // security interest with shares count
                        .interest("2016-01-09", Values.Amount.factorize(58)) // interest without a security
                        .interest_charge("2016-01-11", Values.Amount.factorize(50), secA) // security interest charge without shares count
                        .interest_charge("2016-01-11", Values.Amount.factorize(55), Values.Share.factorize(11), secB) // security interest charge with shares count
                        .interest_charge("2016-01-11", Values.Amount.factorize(58)) // interest charge without a security
                        .addTo(client);
        accountA1.setName("A1");
        
        Account accountA2 = new AccountBuilder() //
                        .deposit_("2016-01-01", Values.Amount.factorize(500))
                        .dividend("2016-01-13", Values.Amount.factorize(10), 15, secC) //
                        .addTo(client);
        accountA2.setName("A2");
        
        // portfolios
        Portfolio portfolioP1 = new PortfolioBuilder(accountA1) //
                        .buy(secA, "2016-01-02", Values.Share.factorize(6), Values.Amount.factorize(60), Values.Amount.factorize(12), Values.Amount.factorize(6))
                        .buy(secA, "2016-01-03", Values.Share.factorize(10), Values.Amount.factorize(60), Values.Amount.factorize(12), Values.Amount.factorize(6))
                        .buy(secB, "2016-01-02", Values.Share.factorize(8), Values.Amount.factorize(110), Values.Amount.factorize(15), Values.Amount.factorize(9))
                        .buy(secB, "2016-01-03", Values.Share.factorize(8), Values.Amount.factorize(110), Values.Amount.factorize(15), Values.Amount.factorize(9))
                        .buy(secC, "2016-01-02", Values.Share.factorize(15), Values.Amount.factorize(150), Values.Amount.factorize(9), Values.Amount.factorize(4), accountA2)
                        .addTo(client);
        portfolioP1.setName("P1");
        
        Portfolio portfolioP2 = new PortfolioBuilder(accountA1) //
                        .buy(secA, "2016-01-02", Values.Share.factorize(4), Values.Amount.factorize(40), Values.Amount.factorize(8), Values.Amount.factorize(4))
                        .buy(secB, "2016-01-02", Values.Share.factorize(4), Values.Amount.factorize(40), Values.Amount.factorize(8), Values.Amount.factorize(4))
                        .addTo(client);
        portfolioP2.setName("P2");
    }

    @Test
    public void testCorrectAdjustmentsOfTransactionsWith2PortfoliosForOneAccount()
    {
        Portfolio portfolio = client.getPortfolios().get(0);

        Client result = new PortfolioClientFilter(portfolio).filter(client);

        assertThat(result.getPortfolios().size(), is(1));
        assertThat(result.getAccounts().size(), is(2));

        Account acc1 = result.getAccounts().get(0);
        Account acc2 = result.getAccounts().get(1);
        
        // only dividends, fees & fee refunds are kept
        assertThat(acc1.getTransactions().size(), is(12));
        assertThat(acc2.getTransactions().size(), is(2));
        
        List<AccountTransaction> dividendTransactionsAcc1 = acc1.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS).collect(Collectors.toList());
        assertThat(dividendTransactionsAcc1.size(), is(2));
        AccountTransaction dividendSecA = dividendTransactionsAcc1.get(0);
        assertThat(dividendSecA.getSecurity().getName(), is("SecA"));
        assertThat(dividendSecA.getAmount(), is(Values.Amount.factorize(6)));
        assertThat(dividendSecA.getUnitSum(Unit.Type.FEE), is(Money.of(dividendSecA.getCurrencyCode(), Values.Amount.factorize(2.40))));
        assertThat(dividendSecA.getUnitSum(Unit.Type.TAX), is(Money.of(dividendSecA.getCurrencyCode(), Values.Amount.factorize(1.20))));
        AccountTransaction dividendSecB = dividendTransactionsAcc1.get(1);
        assertThat(dividendSecB.getSecurity().getName(), is("SecB"));
        assertThat(dividendSecB.getAmount(), is(Values.Amount.factorize(8)));
        assertThat(dividendSecB.getUnitSum(Unit.Type.FEE), is(Money.of(dividendSecA.getCurrencyCode(), Values.Amount.factorize(2.67))));
        assertThat(dividendSecB.getUnitSum(Unit.Type.TAX), is(Money.of(dividendSecA.getCurrencyCode(), Values.Amount.factorize(1.33))));
        
        List<AccountTransaction> dividendTransactionsAcc2 = acc2.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS).collect(Collectors.toList());
        assertThat(dividendTransactionsAcc2.size(), is(1));
        AccountTransaction dividendSecC = dividendTransactionsAcc2.get(0);
        assertThat(dividendSecC.getSecurity().getName(), is("SecC"));
        assertThat(dividendSecC.getAmount(), is(Values.Amount.factorize(10)));
    }
}
