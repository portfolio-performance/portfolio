package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;

@SuppressWarnings("nls")
public class WithoutTaxesFilterTest
{
    private Client client;

    /**
     * Creates three portfolios (A-C) with a reference account each.
     */
    @Before
    public void setupClient()
    {
        client = new Client();

        Security security1 = new SecurityBuilder().addTo(client);
        Security security2 = new SecurityBuilder().addTo(client);

        Arrays.asList("A", "B", "C").forEach(index -> {
            Account a = new AccountBuilder() //
                            .deposit_("2016-01-01", Values.Amount.factorize(100))
                            .tax_____("2016-01-02", Values.Amount.factorize(5))
                            .taxrefnd("2016-02-02", Values.Amount.factorize(5))
                            .dividend("2016-03-01", Values.Amount.factorize(10), Values.Amount.factorize(1), security1) //
                            .addTo(client);
            a.setName(index);

            Portfolio p = new PortfolioBuilder(a) //
                            .buy(security1, "2016-02-01", Values.Share.factorize(1), Values.Amount.factorize(100),
                                            Values.Amount.factorize(10), Values.Amount.factorize(5))
                            .outbound_delivery(security1, "2016-02-02", Values.Share.factorize(1),
                                            Values.Amount.factorize(200), Values.Amount.factorize(20),
                                            Values.Amount.factorize(10))
                            .inbound_delivery(security2, "2016-03-01", Values.Share.factorize(2),
                                            Values.Amount.factorize(500), Values.Amount.factorize(9),
                                            Values.Amount.factorize(4))
                            .sell(security2, "2016-03-02", Values.Share.factorize(1), Values.Amount.factorize(250),
                                            Values.Amount.factorize(6), Values.Amount.factorize(3))
                            .addTo(client);
            p.setName(index);
        });
    }

    @Test
    public void testThatTaxAndTaxRefundIsNotIncluded()
    {
        Client result = new WithoutTaxesFilter().filter(client);

        assertThat(result.getPortfolios().size(), is(3));
        assertThat(result.getAccounts().size(), is(3));

        Account account = result.getAccounts().get(0);

        // 6 regular transactions + 3 corrections for buy, sell, and dividend tx
        assertThat(account.getTransactions().size(), is(9));

        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .filter(t -> t.getAmount() == Values.Amount.factorize(11)) //
                        .findAny().isPresent(), is(true));
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.REMOVAL)
                        .findAny().isPresent(), is(true));

        // taxes converted to removal
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.TAXES)
                        .findAny().isPresent(), is(false));

        // expect 3 removals: for the tax transaction + part of buy + sell +
        // dividend
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(4L));

        // tax refund converted to deposit
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.TAX_REFUND)
                        .findAny().isPresent(), is(false));
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(2L));

        assertThat(AccountSnapshot.create(account, new TestCurrencyConverter(), LocalDate.parse("2016-09-03"))
                        .getFunds(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10 + 250))));

        Portfolio portfolio = result.getPortfolios().get(0);
        assertThat(portfolio.getTransactions().size(), is(4));

        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.BUY) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-02-01T00:00"))) //
                        .filter(t -> t.getAmount() == Values.Amount.factorize(100 - 5)) //
                        .findAny().isPresent(), is(true));

        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-02-02T00:00"))) //
                        .filter(t -> t.getAmount() == Values.Amount.factorize(200 + 10)) //
                        .findAny().isPresent(), is(true));

        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-03-01T00:00"))) //
                        .filter(t -> t.getAmount() == Values.Amount.factorize(500 - 4)) //
                        .findAny().isPresent(), is(true));

        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.SELL) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-03-02T00:00"))) //
                        .filter(t -> t.getAmount() == Values.Amount.factorize(250 + 3)) //
                        .findAny().isPresent(), is(true));

    }

    @Test
    public void testMultipleFilters()
    {
        Client result = new WithoutTaxesFilter()
                        .filter(new PortfolioClientFilter(client.getPortfolios().get(0)).filter(client));

        assertThat(result.getPortfolios().size(), is(1));
        assertThat(result.getAccounts().size(), is(1));

        Account account = result.getAccounts().get(0);

        // dividend + removal taxes + removal dividend
        assertThat(account.getTransactions().size(), is(3));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .filter(t -> t.getAmount() == Values.Amount.factorize(11)).findAny().isPresent(), is(true));

        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.REMOVAL)
                        .findAny().isPresent(), is(true));

        assertThat(AccountSnapshot.create(account, new TestCurrencyConverter(), LocalDate.parse("2016-09-03"))
                        .getFunds(), is(Money.of(CurrencyUnit.EUR, 0)));

        Portfolio portfolio = result.getPortfolios().get(0);
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-02-01T00:00"))) //
                        .filter(t -> t.getAmount() == Values.Amount.factorize(100 - 5)) //
                        .findAny().isPresent(), is(true));

        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-03-02T00:00"))) //
                        .filter(t -> t.getAmount() == Values.Amount.factorize(250 + 3)) //
                        .findAny().isPresent(), is(true));
    }

    @Test
    public void testAccountTransfersWithForex()
    {
        Client client = new Client();

        Account a = new Account();
        a.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(a);

        Account b = new Account();
        b.setCurrencyCode(CurrencyUnit.USD);
        client.addAccount(b);

        AccountTransferEntry entry = new AccountTransferEntry(a, b);
        entry.setDate(LocalDateTime.now());
        entry.getSourceTransaction().setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));
        entry.getTargetTransaction().setMonetaryAmount(Money.of(CurrencyUnit.USD, Values.Amount.factorize(200)));

        Transaction.Unit forex = new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE, //
                        entry.getSourceTransaction().getMonetaryAmount(), //
                        entry.getTargetTransaction().getMonetaryAmount(), //
                        BigDecimal.valueOf(0.5));
        entry.getSourceTransaction().addUnit(forex);

        entry.insert();

        Client result = new WithoutTaxesFilter().filter(client);

        assertThat(result.getAccounts().size(), is(2));

        AccountTransferEntry copy = (AccountTransferEntry) result.getAccounts().get(0).getTransactions().get(0)
                        .getCrossEntry();
        assertThat(copy.getSourceAccount().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(copy.getTargetAccount().getCurrencyCode(), is(CurrencyUnit.USD));
    }

}
