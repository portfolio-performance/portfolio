package name.abuchen.portfolio.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class AccountTest
{
    private Account account;
    private AccountTransaction transaction;

    @Before
    public void setup()
    {
        this.account = new Account();
        account.setName("Testaccount");
        account.setCurrencyCode(CurrencyUnit.EUR);

        this.transaction = new AccountTransaction();
        transaction.setDateTime(LocalDate.now().atStartOfDay());
        transaction.setType(AccountTransaction.Type.DEPOSIT);
        transaction.setAmount(10000);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
    }

    @Test
    public void testAddTransaction()
    {
        assertEquals(account.getTransactions().size(), 0);

        account.addTransaction(transaction);

        assertEquals(account.getTransactions().size(), 1);
        assertTrue(account.getTransactions().contains(transaction));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTransactionIllegalCurrencyUnit()
    {
        transaction.setCurrencyCode(CurrencyUnit.USD);

        account.addTransaction(transaction);
    }
}
