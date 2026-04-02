package name.abuchen.portfolio.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

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
        transaction.setDateTime(LocalDate.of(2024, 03, 12).atStartOfDay());
        transaction.setType(AccountTransaction.Type.DEPOSIT);
        transaction.setAmount(10000);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
    }

    @Test
    public void testAddTransaction()
    {
        assertEquals(0, account.getTransactions().size());

        account.addTransaction(transaction);

        assertEquals(1, account.getTransactions().size());
        assertTrue(account.getTransactions().contains(transaction));
    }

    @Test
    public void testAddTransactionIllegalCurrencyUnit()
    {
        transaction.setCurrencyCode(CurrencyUnit.USD);

        String expectedDateText = Values.Date.format(LocalDate.of(2024, 03, 12));
        String expectedAmount = Values.Amount.format(Long.valueOf(10000));
        
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                        () -> account.addTransaction(transaction));
        assertEquals("exception message mismatch",
                        "Unable to add transaction '" + expectedDateText
                                        + " DEPOSIT           USD    " + expectedAmount
                                        + " <no Security> <no XEntry>' to account 'Testaccount' (uuid "
                                        + transaction.getUUID() + "): EUR <> USD",
                        iae.getMessage());
        assertNull("no cause expected", iae.getCause());

        Security sec = new Security();
        sec.setIsin("DEISIN");
        sec.setName("Security Name Inc");
        transaction.setSecurity(sec);

        iae = assertThrows(IllegalArgumentException.class, () -> account.addTransaction(transaction));
        assertEquals("exception message mismatch",
                        "Unable to add transaction '" + expectedDateText
                                        + " DEPOSIT           USD    " + expectedAmount
                                        + " Security Name Inc <no XEntry>' to account 'Testaccount' (uuid "
                                        + transaction.getUUID() + "): EUR <> USD",
                        iae.getMessage());
        assertNull("no cause expected", iae.getCause());
    }

    @Test
    public void testGetLastTransactionDateWithNoTransactions()
    {
        assertEquals(0, account.getTransactions().size());
        assertEquals(null, account.getLastTransactionDate());
    }

    @Test
    public void testGetLastTransactionDate()
    {
        assertEquals(0, account.getTransactions().size());

        var transactionDates = List.of(LocalDate.of(2020, 04, 16).atStartOfDay(), //
                        LocalDate.of(2026, 03, 15).atStartOfDay(), //
                        LocalDate.of(2024, 06, 20).atStartOfDay());

        for (var date : transactionDates)
        {
            var t = new AccountTransaction(date, CurrencyUnit.EUR, 100, null,
                            AccountTransaction.Type.DEPOSIT);
            account.addTransaction(t);
        }

        assertEquals(LocalDate.of(2026, 03, 15).atStartOfDay(), account.getLastTransactionDate());
    }
}
