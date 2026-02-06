package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Quote;
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
        assertThat(model.getCalculationStatus(), is(ValidationStatus
                        .error(Messages.MsgIncorrectConvertedSubTotal)));

        // subtotal needs to be != 0
        model.setShares(1L);
        model.setQuote(BigDecimal.valueOf(5.0));
        model.setGrossValue(0L);
        assertThat(model.getCalculationStatus(), is(ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnSubTotal))));
    }

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
        assertThat(model.getGrossValue(), is(500L * Values.Amount.factor()));
    }

    @Test
    public void testWithPercentQuoting()
    {
        // some properties can be fetched from a Security object
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        var security = new Security("Acme Corporation", "USD");
        security.setPercentageQuoted(true);
        var date = LocalDate.now();
        security.addPrice(new SecurityPrice(date, 90L * Values.Quote.factor()));
        model.setSecurity(security);
        model.setShares(1000L * Values.Share.factor());
        model.setDate(date);
        assertThat(model.getGrossValue(), is(900L * Values.Amount.factor()));
    }

    @Test
    public void testEntryWithPercentQuoting()
    {
        // set up model with accounts, etc.
        var client = new Client();
        var model = new BuySellModel(client, PortfolioTransaction.Type.BUY);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        var account = new Account();
        account.setCurrencyCode("USD");
        var portfolio = new Portfolio();
        portfolio.setReferenceAccount(account);
        model.setAccount(account);
        model.setPortfolio(portfolio);

        var security = new Security("Acme Corporation", "USD");
        security.setPercentageQuoted(true);
        var date = LocalDate.now();
        security.addPrice(new SecurityPrice(date, 90L * Values.Quote.factor()));
        model.setSecurity(security);
        model.setShares(1000L * Values.Share.factor());
        model.setDate(date);
        assertThat(model.getGrossValue(), is(900L * Values.Amount.factor()));

        // add new transactions to account and portfolio
        model.applyChanges();

        // check the newly created PortfolioTransaction object
        var transaction = portfolio.getTransactions().getFirst();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is("USD"));
        assertThat(transaction.getShares(), is(1000L * Values.Share.factor()));
        assertThat(transaction.getGrossValueAmount(), is(900L * Values.Amount.factor()));
        assertThat(transaction.getGrossPricePerShare(), is(Quote.of("USD", 90L * Values.Quote.factor())));
    }
}
