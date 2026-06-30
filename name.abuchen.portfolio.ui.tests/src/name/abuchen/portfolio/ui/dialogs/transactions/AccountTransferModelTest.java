package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AccountTransferModelTest
{
    @Test
    public void testNewAccountTransferCreatesLedgerBackedProjections() throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var model = model(fixture);

        model.setAmount(Values.Amount.factorize(123));
        model.setNote("note");
        model.applyChanges();

        var sourceTransaction = fixture.source().getTransactions().get(0);
        var targetTransaction = fixture.target().getTransactions().get(0);

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(sourceTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(targetTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(sourceTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(targetTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(sourceTransaction.getNote(), is("note"));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @Test
    public void testNewCrossCurrencyAccountTransferPreservesSourceAndTargetAmounts()
                    throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);
        var model = model(fixture);

        model.setExchangeRate(BigDecimal.valueOf(2));
        model.setFxAmount(Values.Amount.factorize(100));
        model.applyChanges();

        var sourceTransaction = fixture.source().getTransactions().get(0);
        var targetTransaction = fixture.target().getTransactions().get(0);

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(sourceTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(200)));
        assertThat(targetTransaction.getCurrencyCode(), is(CurrencyUnit.USD));
    }

    @Test
    public void testExistingLedgerBackedAccountTransferEditsThroughLedgerAndMovesOwner()
                    throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var createModel = model(fixture);
        createModel.setAmount(Values.Amount.factorize(123));
        createModel.setNote("note");
        createModel.applyChanges();

        var transfer = wrap(fixture);
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();

        var editModel = model(fixture);
        editModel.setSource(transfer);
        editModel.setAmount(Values.Amount.factorize(456));
        editModel.setNote("updated");
        editModel.applyChanges();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions(), is(java.util.List.of(sourceTransaction)));
        assertThat(fixture.target().getTransactions(), is(java.util.List.of(targetTransaction)));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(456)));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(456)));
        assertThat(sourceTransaction.getNote(), is("updated"));
        assertThat(fixture.client().getAllTransactions().size(), is(1));

        var otherAccount = account("Other", CurrencyUnit.EUR);
        fixture.client().addAccount(otherAccount);
        var moveModel = model(fixture);
        moveModel.setSource(transfer);
        moveModel.setSourceAccount(otherAccount);
        moveModel.applyChanges();

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions().size(), is(1));
        assertThat(otherAccount.getTransactions().get(0).getUUID(), is(sourceTransaction.getUUID()));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().get(0).getUUID(), is(targetTransaction.getUUID()));
        assertSame(fixture.target().getTransactions().get(0),
                        otherAccount.getTransactions().get(0).getCrossEntry()
                                        .getCrossTransaction(otherAccount.getTransactions().get(0)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @Test
    public void testExistingLedgerBackedAccountTransferMovesSourceAndTargetOwners() throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var createModel = model(fixture);
        createModel.setAmount(Values.Amount.factorize(123));
        createModel.applyChanges();

        var transfer = wrap(fixture);
        var swapModel = model(fixture);
        swapModel.setSource(transfer);
        swapModel.setSourceAccount(fixture.target());
        swapModel.setTargetAccount(fixture.source());
        swapModel.applyChanges();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(fixture.source().getTransactions().get(0),
                        fixture.target().getTransactions().get(0).getCrossEntry()
                                        .getCrossTransaction(fixture.target().getTransactions().get(0)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    private AccountTransferModel model(Fixture fixture)
    {
        var model = new AccountTransferModel(fixture.client());

        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.setSourceAccount(fixture.source());
        model.setTargetAccount(fixture.target());
        model.setDate(LocalDate.of(2026, 6, 7));

        return model;
    }

    private AccountTransferEntry wrap(Fixture fixture)
    {
        return AccountTransferEntry.readOnly(fixture.source(), fixture.source().getTransactions().get(0),
                        fixture.target(), fixture.target().getTransactions().get(0));
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

    private Fixture fixture(String sourceCurrency, String targetCurrency)
    {
        var client = new Client();
        var source = account("Source", sourceCurrency);
        var target = account("Target", targetCurrency);

        client.addAccount(source);
        client.addAccount(target);

        return new Fixture(client, source, target);
    }

    private Account account(String name, String currency)
    {
        var account = new Account(name);

        account.setCurrencyCode(currency);

        return account;
    }

    private record Fixture(Client client, Account source, Account target)
    {
    }
}
