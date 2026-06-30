package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityDeliveryModelTest
{
    @Test
    public void testNewDeliveryInboundCreatesLedgerBackedProjection() throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.USD);
        var model = model(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);

        applyDeliveryValues(model);
        model.applyChanges();

        var transaction = fixture.portfolio().getTransactions().get(0);

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(transaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertSame(fixture.security(), transaction.getSecurity());
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(260)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getNote(), is("note"));
        assertThat(transaction.getUnits().count(), is(3L));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @Test
    public void testNewDeliveryOutboundCreatesLedgerBackedProjection() throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.USD);
        var model = model(fixture, PortfolioTransaction.Type.DELIVERY_OUTBOUND);

        applyDeliveryValues(model);
        model.applyChanges();

        var transaction = fixture.portfolio().getTransactions().get(0);

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(transaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertSame(fixture.security(), transaction.getSecurity());
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(140)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getUnits().count(), is(3L));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @Test
    public void testExistingLedgerBackedDeliveryEditsThroughLedgerAndMovesOwner()
                    throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.USD);
        var createModel = model(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        applyDeliveryValues(createModel);
        createModel.applyChanges();
        var transaction = fixture.portfolio().getTransactions().get(0);

        var editModel = model(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        editModel.setSource(new TransactionPair<>(fixture.portfolio(), transaction));
        editModel.setShares(Values.Share.factorize(20));
        editModel.setGrossValue(Values.Amount.factorize(150));
        editModel.setForexTaxes(Values.Amount.factorize(5));
        editModel.setNote("updated");
        editModel.applyChanges();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.portfolio().getTransactions(), is(java.util.List.of(transaction)));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(330)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));
        assertThat(transaction.getNote(), is("updated"));
        assertTrue(transaction.getUnits().anyMatch(unit -> unit.getType() == Transaction.Unit.Type.TAX
                        && unit.getForex() != null
                        && unit.getForex().getAmount() == Values.Amount.factorize(5)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));

        var otherPortfolio = portfolio(fixture.account());
        fixture.client().addPortfolio(otherPortfolio);
        var moveModel = model(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        moveModel.setSource(new TransactionPair<>(fixture.portfolio(), transaction));
        moveModel.setPortfolio(otherPortfolio);
        moveModel.applyChanges();

        assertTrue(fixture.portfolio().getTransactions().isEmpty());
        assertThat(otherPortfolio.getTransactions().size(), is(1));
        assertThat(otherPortfolio.getTransactions().get(0).getUUID(), is(transaction.getUUID()));
        assertThat(otherPortfolio.getTransactions().get(0).getShares(), is(Values.Share.factorize(20)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @Test
    public void testUnsupportedFamiliesAreNotAcceptedByDeliveryModel() throws ReflectiveOperationException
    {
        var fixture = fixture(CurrencyUnit.EUR);

        assertThrows(IllegalArgumentException.class,
                        () -> new SecurityDeliveryModel(fixture.client(), PortfolioTransaction.Type.BUY));
        assertThrows(IllegalArgumentException.class,
                        () -> new SecurityDeliveryModel(fixture.client(), PortfolioTransaction.Type.SELL));
        assertThrows(IllegalArgumentException.class,
                        () -> new SecurityDeliveryModel(fixture.client(), PortfolioTransaction.Type.TRANSFER_IN));
        assertThrows(IllegalArgumentException.class,
                        () -> new SecurityDeliveryModel(fixture.client(), PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(ledgerEntryCount(fixture.client()), is(0));
        assertTrue(fixture.portfolio().getTransactions().isEmpty());
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

    private SecurityDeliveryModel model(Fixture fixture, PortfolioTransaction.Type type)
    {
        var model = new SecurityDeliveryModel(fixture.client(), type);

        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.setPortfolio(fixture.portfolio());
        model.setSecurity(fixture.security());
        model.setDate(LocalDate.of(2026, 6, 7));

        return model;
    }

    private void applyDeliveryValues(SecurityDeliveryModel model)
    {
        model.setExchangeRate(BigDecimal.valueOf(2));
        model.setShares(Values.Share.factorize(12));
        model.setGrossValue(Values.Amount.factorize(100));
        model.setForexFees(Values.Amount.factorize(10));
        model.setForexTaxes(Values.Amount.factorize(20));
        model.setNote("note");
    }

    private Fixture fixture(String securityCurrency)
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);
        var account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        var portfolio = portfolio(account);
        var security = new Security("Security", securityCurrency);

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security);
    }

    private Portfolio portfolio(Account account)
    {
        var portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        return portfolio;
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }
}
