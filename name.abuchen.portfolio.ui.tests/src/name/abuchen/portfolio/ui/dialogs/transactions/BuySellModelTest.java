package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.text.MessageFormat;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
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
}
