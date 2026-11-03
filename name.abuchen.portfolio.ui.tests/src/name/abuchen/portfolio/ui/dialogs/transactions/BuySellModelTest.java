package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
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

    @SuppressWarnings("nls")
    @Test
    public void testChangedGrossValueWithPercentQuoting()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        var security = new Security("Acme Bond", "EUR");
        security.setPercentageQuoted(true);
        model.setSecurity(security);
        model.setShares(1000L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(90.0));
        assertThat(model.getGrossValue(), is(900L * Values.Amount.factor()));

        // changes to the gross value must update the quote in percent
        model.setGrossValue(950L * Values.Amount.factor());
        assertThat(model.getQuote(), is(BigDecimal.valueOf(95.0)));
        assertThat(model.getTotal(), is(950L * Values.Amount.factor()));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @SuppressWarnings("nls")
    @Test
    public void testChangedTotalWithPercentQuoting()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        var security = new Security("Acme Bond", "EUR");
        security.setPercentageQuoted(true);
        model.setSecurity(security);
        model.setShares(1000L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(90.0));

        // changes to the total must update the quote in percent
        model.setTotal(1010L * Values.Amount.factor());
        assertThat(model.getQuote(), is(BigDecimal.valueOf(101.0)));
        assertThat(model.getGrossValue(), is(1010L * Values.Amount.factor()));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @SuppressWarnings("nls")
    @Test
    public void testChangedSharesWithPercentQuoting()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        var security = new Security("Acme Bond", "EUR");
        security.setPercentageQuoted(true);
        model.setSecurity(security);
        model.setQuote(BigDecimal.ZERO);
        model.setGrossValue(900L * Values.Amount.factor());

        // without quote, the quote is derived from gross value and nominal
        model.setShares(1000L * Values.Share.factor());
        assertThat(model.getQuote(), is(BigDecimal.valueOf(90.0)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @SuppressWarnings("nls")
    @Test
    public void testEditForexEntryWithPercentQuoting()
    {
        var client = new Client();
        var account = new Account();
        account.setCurrencyCode("EUR");
        client.addAccount(account);
        var portfolio = new Portfolio();
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        var security = new Security("Acme Bond", "USD");
        security.setPercentageQuoted(true);
        client.addSecurity(security);

        // USD bond bought via EUR account: 1,000 nominal x 100 % = USD 1,000
        var entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.parse("2025-01-15T00:00"));
        entry.setSecurity(security);
        entry.setShares(1000L * Values.Share.factor());
        entry.setMonetaryAmount(Money.of("EUR", 900L * Values.Amount.factor()));
        entry.getPortfolioTransaction()
                        .addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 900L * Values.Amount.factor()),
                                        Money.of("USD", 1000L * Values.Amount.factor()), BigDecimal.valueOf(0.9)));
        entry.insert();

        var model = new BuySellModel(client, PortfolioTransaction.Type.BUY);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        model.setSource(entry);

        // the quote is restored in percent from the forex gross value
        assertThat(model.getQuote(), is(BigDecimal.valueOf(100.0)));
        assertThat(model.getGrossValue(), is(1000L * Values.Amount.factor()));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @SuppressWarnings("nls")
    @Test
    public void testStatusErrorsWithPercentQuoting()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.BUY);
        var security = new Security("Acme Bond", "EUR");
        security.setPercentageQuoted(true);
        model.setSecurity(security);
        assertThat(model.getSecurityQuotation(), is("%"));

        // nominal value needs to be != 0
        model.setShares(0L);
        model.setQuote(BigDecimal.valueOf(90.0));
        assertThat(model.getCalculationStatus(), is(ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnNominal))));
    }

    @SuppressWarnings("nls")
    @Test
    public void testPurchaseWithAccruedInterest()
    {
        var client = new Client();
        var model = new BuySellModel(client, PortfolioTransaction.Type.BUY);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        var account = new Account();
        account.setCurrencyCode("EUR");
        var portfolio = new Portfolio();
        portfolio.setReferenceAccount(account);
        model.setAccount(account);
        model.setPortfolio(portfolio);

        var bond = new Security("Acme Bond", "EUR");
        bond.setPercentageQuoted(true);
        model.setSecurity(bond);
        model.setShares(1000L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(100.5));
        model.setAccruedInterest(Values.Amount.factorize(12.34));
        model.setFees(Values.Amount.factorize(5));

        // debit = gross value + accrued interest + fees
        assertThat(model.getGrossValue(), is(Values.Amount.factorize(1005)));
        assertThat(model.getTotal(), is(Values.Amount.factorize(1022.34)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));

        // changing the total keeps the accrued interest out of the price
        model.setTotal(Values.Amount.factorize(1027.34));
        assertThat(model.getGrossValue(), is(Values.Amount.factorize(1010)));
        assertThat(model.getQuote(), is(BigDecimal.valueOf(101.0)));

        model.applyChanges();

        var transaction = portfolio.getTransactions().getFirst();
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1027.34)));
        assertThat(transaction.getAccruedInterest(), is(Money.of("EUR", Values.Amount.factorize(12.34))));
        assertThat(transaction.getGrossValueAmount(), is(Values.Amount.factorize(1010)));

        // reopen the transaction
        var edit = new BuySellModel(client, PortfolioTransaction.Type.BUY);
        edit.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        edit.setSource(transaction.getCrossEntry());
        assertThat(edit.getAccruedInterest(), is(Values.Amount.factorize(12.34)));
        assertThat(edit.getGrossValue(), is(Values.Amount.factorize(1010)));
        assertThat(edit.getQuote(), is(BigDecimal.valueOf(101.0)));
        assertThat(edit.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @SuppressWarnings("nls")
    @Test
    public void testSaleWithAccruedInterest()
    {
        var model = new BuySellModel(new Client(), PortfolioTransaction.Type.SELL);
        var bond = new Security("Acme Bond", "EUR");
        bond.setPercentageQuoted(true);
        model.setSecurity(bond);
        model.setShares(1000L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(101.0));
        model.setAccruedInterest(Values.Amount.factorize(5));
        model.setFees(Values.Amount.factorize(5));

        // credit = gross value + accrued interest - fees
        assertThat(model.getGrossValue(), is(Values.Amount.factorize(1010)));
        assertThat(model.getTotal(), is(Values.Amount.factorize(1010)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @SuppressWarnings("nls")
    @Test
    public void testPurchaseWithAccruedInterestInForeignCurrency()
    {
        // EUR bond bought via USD account at 1.10 USD/EUR
        var client = new Client();
        var account = new Account();
        account.setCurrencyCode("USD");
        client.addAccount(account);
        var portfolio = new Portfolio();
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
        var bond = new Security("Acme Bond", "EUR");
        bond.setPercentageQuoted(true);
        client.addSecurity(bond);

        var model = new BuySellModel(client, PortfolioTransaction.Type.BUY);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        model.setAccount(account);
        model.setPortfolio(portfolio);
        model.setSecurity(bond);
        model.setDate(LocalDate.parse("2015-01-15"));
        model.setShares(1000L * Values.Share.factor());
        model.setQuote(BigDecimal.valueOf(100.0));
        model.setExchangeRate(new BigDecimal("1.1"));
        model.setForexAccruedInterest(Values.Amount.factorize(10));
        model.setFees(Values.Amount.factorize(5.50));

        // debit = 1,100.00 + 11.00 (10.00 EUR) + 5.50 USD
        assertThat(model.getConvertedGrossValue(), is(Values.Amount.factorize(1100)));
        assertThat(model.getTotal(), is(Values.Amount.factorize(1116.50)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));

        model.applyChanges();

        // the accrued interest is stored with the exchange rate of the
        // transaction
        var transaction = portfolio.getTransactions().getFirst();
        var unit = transaction.getUnit(Unit.Type.ACCRUED_INTEREST).orElseThrow();
        assertThat(unit.getAmount(), is(Money.of("USD", Values.Amount.factorize(11))));
        assertThat(unit.getForex(), is(Money.of("EUR", Values.Amount.factorize(10))));
        assertThat(unit.getExchangeRate(), is(new BigDecimal("1.1")));
        assertThat(transaction.getGrossValueAmount(), is(Values.Amount.factorize(1100)));

        // cost in the currency of the security uses the transaction rate
        // for both, the amount and the accrued interest
        var converter = new TestCurrencyConverter();
        assertThat(transaction.getMonetaryAmountWithoutAccruedInterest(converter),
                        is(Money.of("EUR", Values.Amount.factorize(1005))));

        // reopen
        var edit = new BuySellModel(client, PortfolioTransaction.Type.BUY);
        edit.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        edit.setSource(transaction.getCrossEntry());
        assertThat(edit.getForexAccruedInterest(), is(Values.Amount.factorize(10)));
        assertThat(edit.getAccruedInterest(), is(0L));
        assertThat(edit.getTotal(), is(Values.Amount.factorize(1116.50)));
        assertThat(edit.getCalculationStatus(), is(ValidationStatus.ok()));
    }
}
