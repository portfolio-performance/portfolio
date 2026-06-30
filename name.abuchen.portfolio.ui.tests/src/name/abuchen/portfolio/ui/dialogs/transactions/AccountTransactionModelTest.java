package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AccountTransactionModelTest
{
    @Test
    public void testNewAccountOnlyTransactionCreatesLedgerBackedProjection() throws ReflectiveOperationException
    {
        var client = new Client();
        var account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        var model = new AccountTransactionModel(client, AccountTransaction.Type.DEPOSIT);

        model.setAccount(account);
        model.setDate(LocalDate.of(2026, 6, 7));
        model.setTotal(Values.Amount.factorize(123));
        model.setNote("note");
        model.applyChanges();

        assertThat(ledgerEntryCount(client), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getClass().getName(), containsString("LedgerBacked"));
        assertThat(client.getAllTransactions().size(), is(1));
    }

    @Test
    public void testExistingLedgerBackedAccountOnlyTransactionEditsThroughLedger()
                    throws ReflectiveOperationException
    {
        var client = new Client();
        var account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        var createModel = new AccountTransactionModel(client, AccountTransaction.Type.DEPOSIT);
        createModel.setAccount(account);
        createModel.setDate(LocalDate.of(2026, 6, 7));
        createModel.setTotal(Values.Amount.factorize(123));
        createModel.setNote("note");
        createModel.applyChanges();

        var transaction = account.getTransactions().get(0);
        var editModel = new AccountTransactionModel(client, AccountTransaction.Type.DEPOSIT);
        editModel.setSource(account, transaction);
        editModel.setTotal(Values.Amount.factorize(456));
        editModel.setNote("updated");
        editModel.applyChanges();

        assertThat(ledgerEntryCount(client), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), is(transaction));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(456)));
        assertThat(transaction.getNote(), is("updated"));
        assertThat(client.getAllTransactions().size(), is(1));
    }

    @Test
    public void testAccountOnlyFeeDoesNotOfferExDate()
    {
        var model = new AccountTransactionModel(new Client(), AccountTransaction.Type.FEES);

        assertThat(model.supportsSecurity(), is(true));
        assertThat(model.supportsExDate(), is(false));
    }

    @Test
    public void testNewDividendCreatesLedgerBackedProjection() throws ReflectiveOperationException
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);
        var account = new Account("Account");
        var security = new Security("Security", CurrencyUnit.USD);
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        client.addSecurity(security);

        var model = new AccountTransactionModel(client, AccountTransaction.Type.DIVIDENDS);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));

        model.setAccount(account);
        model.setSecurity(security);
        model.setDate(LocalDate.of(2026, 6, 7));
        model.setShares(Values.Share.factorize(12));
        model.setExDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        model.setExchangeRate(BigDecimal.valueOf(2));
        model.setFxGrossAmount(Values.Amount.factorize(100));
        model.setFxFees(Values.Amount.factorize(10));
        model.setFxTaxes(Values.Amount.factorize(20));
        model.setNote("note");
        model.applyChanges();

        var transaction = account.getTransactions().get(0);

        assertThat(ledgerEntryCount(client), is(1));
        assertThat(transaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertSame(security, transaction.getSecurity());
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));
        assertThat(transaction.getExDate(), is(LocalDateTime.of(2026, 6, 1, 0, 0)));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(140)));
        assertThat(transaction.getNote(), is("note"));
        assertThat(transaction.getUnits().count(), is(3L));
        assertThat(client.getAllTransactions().size(), is(1));
    }

    @Test
    public void testExistingLedgerBackedDividendEditsThroughLedgerAndMovesOwner()
                    throws ReflectiveOperationException
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);
        var account = new Account("Account");
        var otherAccount = new Account("Other Account");
        var security = new Security("Security", CurrencyUnit.EUR);
        account.setCurrencyCode(CurrencyUnit.EUR);
        otherAccount.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        client.addAccount(otherAccount);
        client.addSecurity(security);

        var createModel = new AccountTransactionModel(client, AccountTransaction.Type.DIVIDENDS);
        createModel.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        createModel.setAccount(account);
        createModel.setSecurity(security);
        createModel.setDate(LocalDate.of(2026, 6, 7));
        createModel.setShares(Values.Share.factorize(12));
        createModel.setTotal(Values.Amount.factorize(123));
        createModel.setNote("note");
        createModel.applyChanges();

        var transaction = account.getTransactions().get(0);
        var editModel = new AccountTransactionModel(client, AccountTransaction.Type.DIVIDENDS);
        editModel.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        editModel.setSource(account, transaction);
        editModel.setShares(Values.Share.factorize(20));
        editModel.setExDate(LocalDateTime.of(2026, 6, 2, 0, 0));
        editModel.setTotal(Values.Amount.factorize(456));
        editModel.setTaxes(Values.Amount.factorize(5));
        editModel.setNote("updated");
        editModel.applyChanges();

        assertThat(ledgerEntryCount(client), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), is(transaction));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(451)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));
        assertThat(transaction.getExDate(), is(LocalDateTime.of(2026, 6, 2, 0, 0)));
        assertThat(transaction.getNote(), is("updated"));
        assertThat(transaction.getUnitSum(Transaction.Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(5)));
        assertThat(client.getAllTransactions().size(), is(1));

        var moveModel = new AccountTransactionModel(client, AccountTransaction.Type.DIVIDENDS);
        moveModel.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        moveModel.setSource(account, transaction);
        moveModel.setAccount(otherAccount);
        moveModel.applyChanges();

        assertTrue(account.getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions().size(), is(1));
        assertThat(otherAccount.getTransactions().get(0).getUUID(), is(transaction.getUUID()));
        assertThat(otherAccount.getTransactions().get(0).getAmount(), is(Values.Amount.factorize(451)));
        assertThat(client.getAllTransactions().size(), is(1));
    }

    private int ledgerEntryCount(Client client) throws ReflectiveOperationException
    {
        try
        {
            var ledger = Client.class.getMethod("getLedger").invoke(client);
            var entries = ledger.getClass().getMethod("getEntries").invoke(ledger);
            return ((java.util.List<?>) entries).size();
        }
        catch (InvocationTargetException e)
        {
            throw new AssertionError(e.getCause());
        }
    }
}
