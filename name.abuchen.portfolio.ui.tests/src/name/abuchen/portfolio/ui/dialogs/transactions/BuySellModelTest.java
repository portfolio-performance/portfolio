package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class BuySellModelTest
{
    @Test
    public void testBuyTotal()
    {
        // fees and taxes added on top of gross value:
        // shares 100, price 5, sub-total 500, fees 11, taxes 22, total 533
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        model.setShares(100L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(5.0));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
        assertThat(model.getGrossValue(), is(500L * Values.Amount.factor()));
        assertThat(model.getTotal(), is(500L * Values.Amount.factor()));
        model.setFees(11 * Values.Amount.factor());
        model.setTaxes(22 * Values.Amount.factor());
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
        assertThat(model.getTotal(), is(533L * Values.Amount.factor()));
    }

    @Test
    public void testSellTotal()
    {
        // fees and taxes deducted from gross value
        // shares 100, price 5, sub-total 500, fees 11, taxes 22, total 467
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.SELL);
        model.setShares(100L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(5.0));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
        assertThat(model.getGrossValue(), is(500L * Values.Amount.factor()));
        assertThat(model.getTotal(), is(500L * Values.Amount.factor()));
        model.setFees(11 * Values.Amount.factor());
        model.setTaxes(22 * Values.Amount.factor());
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
        assertThat(model.getTotal(), is(467L * Values.Amount.factor()));
    }

    @Test
    public void testChangedShares()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        model.setShares(100L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(5.0));

        // doubling the number of shares should trigger a doubling of the totals
        model.setShares(200L * Values.Share.factor());
        assertThat(model.getQuote(), is(BigDecimal.valueOf(5.0)));
        assertThat(model.getGrossValue(), is(1000L * Values.Amount.factor()));
        assertThat(model.getTotal(), is(1000L * Values.Amount.factor()));
    }

    @Test
    public void testChangedGrossValue()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        model.setShares(100L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(5.0));

        // changes to the gross value should update quote and total value
        model.setGrossValue(1000L * Values.Amount.factor());
        assertThat(model.getQuote(), is(BigDecimal.valueOf(10.0)));
        assertThat(model.getTotal(), is(1000L * Values.Amount.factor()));
    }

    @Test
    public void testChangedTotal()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        model.setShares(100L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(5.0));

        // changes to the total value should update quote and subtotal
        model.setTotal(1000L * Values.Amount.factor());
        assertThat(model.getQuote(), is(BigDecimal.valueOf(10.0)));
        assertThat(model.getGrossValue(), is(1000L * Values.Amount.factor()));
    }

    @Test
    public void testStatusErrors()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);

        // number of shares needs to be != 0
        model.setShares(0L);
        model.setQuote(BigDecimal.valueOf(5.0));
        assertThat(model.getCalculationStatus(), is(ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnShares))));

        // number of shares needs to be positive
        model.setShares(-100L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(5.0));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.error(Messages.MsgIncorrectSubTotal)));

        // quote needs to be positive
        model.setShares(100L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(-5.0));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.error(Messages.MsgIncorrectConvertedSubTotal)));

        // subtotal needs to be != 0
        model.setShares(1L);
        model.setQuote(BigDecimal.valueOf(5.0));
        model.setGrossValue(0L);
        assertThat(model.getCalculationStatus(), is(ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnSubTotal))));
    }

    @SuppressWarnings("nls")
    @Test
    public void testWithSecurity()
    {
        // some properties can be fetched from a Security object
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        var security = new Security("Acme Corporation", "USD");
        var date = LocalDate.now();
        security.addPrice(new SecurityPrice(date, 5L * Values.Quote.factor()));
        model.setSecurity(security);
        model.setShares(100L * Values.Share.factor());
        model.setDate(date);
        assertThat(model.getQuote(), is(BigDecimal.valueOf(5.0)));
        assertThat(model.getSecurityCurrencyCode(), is("USD"));
        assertThat(model.getExchangeRate(), is(BigDecimal.ONE));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNewBuyCreatesLedgerBackedProjections()
    {
        var fixture = fixture(PortfolioTransaction.Type.BUY);
        var model = model(fixture, PortfolioTransaction.Type.BUY);

        fillForexBuySell(model);
        model.applyChanges();

        var accountTransaction = fixture.account().getTransactions().get(0);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);

        assertThat(accountTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(portfolioTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(accountTransaction.getType().name(), is(PortfolioTransaction.Type.BUY.name()));
        assertThat(portfolioTransaction.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(portfolioTransaction.getShares(), is(Values.Share.factorize(5)));
        assertThat(portfolioTransaction.getAmount(), is(Values.Amount.factorize(127)));
        assertThat(portfolioTransaction.getUnits().count(), is(3L));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNewSellCreatesLedgerBackedProjections()
    {
        var fixture = fixture(PortfolioTransaction.Type.SELL);
        var model = model(fixture, PortfolioTransaction.Type.SELL);

        fillForexBuySell(model);
        model.applyChanges();

        var accountTransaction = fixture.account().getTransactions().get(0);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);

        assertThat(accountTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(portfolioTransaction.getClass().getName(), containsString("LedgerBacked"));
        assertThat(accountTransaction.getType().name(), is(PortfolioTransaction.Type.SELL.name()));
        assertThat(portfolioTransaction.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(portfolioTransaction.getShares(), is(Values.Share.factorize(5)));
        assertThat(portfolioTransaction.getAmount(), is(Values.Amount.factorize(113)));
        assertThat(portfolioTransaction.getUnits().count(), is(3L));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    @SuppressWarnings("nls")
    @Test
    public void testExistingLedgerBackedBuySellEditsThroughLedgerAndMovesOwner()
    {
        var fixture = fixture(PortfolioTransaction.Type.BUY);
        var entry = new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        fixture.account(), PortfolioTransaction.Type.BUY, LocalDateTime.of(2026, 6, 7, 8, 9),
                        Values.Amount.factorize(100), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), java.util.List.of(), "note", "source");
        var accountTransaction = entry.getAccountTransaction();
        var portfolioTransaction = entry.getPortfolioTransaction();
        var editModel = editModel(fixture, PortfolioTransaction.Type.BUY);

        editModel.setSource(entry);
        editModel.setShares(Values.Share.factorize(7));
        editModel.setGrossValue(Values.Amount.factorize(140));
        editModel.setNote("updated");
        editModel.applyChanges();

        assertThat(fixture.account().getTransactions(), is(java.util.List.of(accountTransaction)));
        assertThat(fixture.portfolio().getTransactions(), is(java.util.List.of(portfolioTransaction)));
        assertThat(portfolioTransaction.getShares(), is(Values.Share.factorize(7)));
        assertThat(portfolioTransaction.getAmount(), is(Values.Amount.factorize(140)));
        assertThat(portfolioTransaction.getNote(), is("updated"));

        var otherPortfolio = new Portfolio("Other");
        otherPortfolio.setReferenceAccount(fixture.account());
        fixture.client().addPortfolio(otherPortfolio);
        var moveModel = editModel(fixture, PortfolioTransaction.Type.BUY);
        moveModel.setSource(entry);
        moveModel.setPortfolio(otherPortfolio);
        moveModel.applyChanges();

        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.account().getTransactions().get(0).getUUID(), is(accountTransaction.getUUID()));
        assertTrue(fixture.portfolio().getTransactions().isEmpty());
        assertThat(otherPortfolio.getTransactions().size(), is(1));
        assertThat(otherPortfolio.getTransactions().get(0).getUUID(), is(portfolioTransaction.getUUID()));
        assertSame(otherPortfolio.getTransactions().get(0),
                        fixture.account().getTransactions().get(0).getCrossEntry()
                                        .getCrossTransaction(fixture.account().getTransactions().get(0)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
    }

    private void fillForexBuySell(BuySellModel model)
    {
        model.setExchangeRate(new BigDecimal("0.5000"));
        model.setShares(Values.Share.factorize(5));
        model.setGrossValue(Values.Amount.factorize(240));
        model.setFees(Values.Amount.factorize(3));
        model.setForexTaxes(Values.Amount.factorize(8));
        model.setNote("note");
    }

    private BuySellModel model(Fixture fixture, PortfolioTransaction.Type type)
    {
        var model = editModel(fixture, type);

        model.setPortfolio(fixture.portfolio());
        model.setSecurity(fixture.security());
        model.setDate(LocalDate.of(2026, 6, 7));

        return model;
    }

    private BuySellModel editModel(Fixture fixture, PortfolioTransaction.Type type)
    {
        var model = new BuySellModel(fixture.client(), type);

        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));

        return model;
    }

    private Fixture fixture(PortfolioTransaction.Type type)
    {
        var client = new Client();
        var account = new Account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.USD);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 6, 7), Values.Quote.factorize(48)));

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security, type);
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security,
                    PortfolioTransaction.Type type)
    {
    }
}
