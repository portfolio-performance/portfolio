package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityTransferModelTest
{
    @Test
    public void testNewPortfolioTransferCreatesLedgerBackedProjections() throws ReflectiveOperationException
    {
        var fixture = fixture();
        var model = model(fixture);

        model.setShares(Values.Share.factorize(5));
        model.setAmount(Values.Amount.factorize(123));
        model.setNote("note");
        model.applyChanges();

        var sourceTransaction = fixture.source().getTransactions().get(0);
        var targetTransaction = fixture.target().getTransactions().get(0);

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(sourceTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(targetTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(sourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(fixture.security(), sourceTransaction.getSecurity());
        assertSame(fixture.security(), targetTransaction.getSecurity());
        assertThat(sourceTransaction.getShares(), is(Values.Share.factorize(5)));
        assertThat(targetTransaction.getShares(), is(Values.Share.factorize(5)));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(sourceTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(targetTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(sourceTransaction.getNote(), is("note"));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @Test
    public void testExistingLedgerBackedPortfolioTransferEditsThroughLedgerAndMovesOwner()
                    throws ReflectiveOperationException
    {
        var fixture = fixture();
        var creator = new LedgerPortfolioTransferTransactionCreator(fixture.client());
        var transfer = creator.create(fixture.source(), fixture.target(), fixture.security(), java.time.LocalDateTime
                        .of(2026, 6, 7, 8, 9), Values.Share.factorize(5), Values.Amount.factorize(123),
                        CurrencyUnit.EUR, "note", "source");
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();

        var editModel = editModel(fixture);
        editModel.setSource(transfer);
        editModel.setShares(Values.Share.factorize(7));
        editModel.setAmount(Values.Amount.factorize(456));
        editModel.setNote("updated");
        editModel.applyChanges();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions(), is(List.of(sourceTransaction)));
        assertThat(fixture.target().getTransactions(), is(List.of(targetTransaction)));
        assertThat(sourceTransaction.getShares(), is(Values.Share.factorize(7)));
        assertThat(targetTransaction.getShares(), is(Values.Share.factorize(7)));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(456)));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(456)));
        assertThat(sourceTransaction.getNote(), is("updated"));
        assertThat(fixture.client().getAllTransactions().size(), is(1));

        var otherPortfolio = new Portfolio("Other");
        fixture.client().addPortfolio(otherPortfolio);
        var moveModel = editModel(fixture);
        moveModel.setSource(transfer);
        moveModel.setSourcePortfolio(otherPortfolio);
        moveModel.applyChanges();

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertThat(otherPortfolio.getTransactions().size(), is(1));
        assertThat(otherPortfolio.getTransactions().get(0).getUUID(), is(sourceTransaction.getUUID()));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().get(0).getUUID(), is(targetTransaction.getUUID()));
        assertSame(fixture.target().getTransactions().get(0),
                        otherPortfolio.getTransactions().get(0).getCrossEntry()
                                        .getCrossTransaction(otherPortfolio.getTransactions().get(0)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @Test
    public void testExistingLedgerBackedPortfolioTransferMovesSourceAndTargetOwners()
                    throws ReflectiveOperationException
    {
        var fixture = fixture();
        var transfer = new LedgerPortfolioTransferTransactionCreator(fixture.client()).create(fixture.source(),
                        fixture.target(), fixture.security(), java.time.LocalDateTime.of(2026, 6, 7, 8, 9),
                        Values.Share.factorize(5), Values.Amount.factorize(123), CurrencyUnit.EUR, "note", "source");
        var swapModel = editModel(fixture);
        swapModel.setSource(transfer);
        swapModel.setSourcePortfolio(fixture.target());
        swapModel.setTargetPortfolio(fixture.source());
        swapModel.applyChanges();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertSame(fixture.source().getTransactions().get(0),
                        fixture.target().getTransactions().get(0).getCrossEntry()
                                        .getCrossTransaction(fixture.target().getTransactions().get(0)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    private SecurityTransferModel model(Fixture fixture)
    {
        var model = new SecurityTransferModel(fixture.client());

        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.setSourcePortfolio(fixture.source());
        model.setTargetPortfolio(fixture.target());
        model.setSecurity(fixture.security());
        model.setDate(LocalDate.of(2026, 6, 7));

        return model;
    }

    private SecurityTransferModel editModel(Fixture fixture)
    {
        var model = new SecurityTransferModel(fixture.client());

        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));

        return model;
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

    private Fixture fixture()
    {
        var client = new Client();
        var source = new Portfolio("Source");
        var target = new Portfolio("Target");
        var security = new Security("Security", CurrencyUnit.EUR);

        client.addPortfolio(source);
        client.addPortfolio(target);
        client.addSecurity(security);

        return new Fixture(client, source, target, security);
    }

    private record Fixture(Client client, Portfolio source, Portfolio target, Security security)
    {
    }
}
