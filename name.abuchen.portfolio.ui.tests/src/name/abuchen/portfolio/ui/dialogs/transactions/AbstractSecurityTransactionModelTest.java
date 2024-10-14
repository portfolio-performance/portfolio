package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.MessageFormat;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.ui.Messages;

@SuppressWarnings("nls")
public class AbstractSecurityTransactionModelTest
{
    private AbstractSecurityTransactionModel model;

    @Before
    public void setup()
    {
        Client client = new Client();
        PortfolioTransaction.Type type = PortfolioTransaction.Type.BUY;

        model = new AbstractSecurityTransactionModel(client, type)
        {
            @Override
            public boolean accepts(Type type)
            {
                return true;
            }

            @Override
            public void setSource(Object source)
            {
            }

            @Override
            public boolean hasSource()
            {
                return false;
            }

            @Override
            public String getTransactionCurrencyCode()
            {
                return null;
            }

            @Override
            public void applyChanges()
            {
            }
        };
    }

    @Test
    public void testCalculationStatus_WithValidData()
    {
        model.setShares(1);
        model.setQuote(BigDecimal.valueOf(100));
        model.setGrossValue(100);
        model.setExchangeRate(BigDecimal.valueOf(0.5));
        model.setConvertedGrossValue(200);
        model.setTotal(800);
        model.setFees(50);
        model.setTaxes(100);
        model.setForexFees(150);
        model.setForexTaxes(200);

        assertEquals(model.calculateStatus(), ValidationStatus.ok());

        // 800 = 1650 - (200 / 0.5) - (150 / 0.5) - 100 - 50
        assertEquals(800L, model.calculateConvertedGrossValue());

        // 1650 = setTotal(800) + (200 / 0.5) + (150 / 0.5) + 100 + 50
        assertEquals(1650L, model.calculateTotal());
    }

    @Test
    public void testCalculationStatus_WithInvalidData()
    {
        model.setShares(0);
        model.setQuote(BigDecimal.valueOf(0));
        model.setGrossValue(0);
        model.setExchangeRate(BigDecimal.valueOf(0));
        model.setConvertedGrossValue(0);
        model.setTotal(0);

        assertEquals(model.calculateStatus(), ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnShares)));
    }

    @Test
    public void testCalculationStatus_WithInvalidDataTotalAndSubTotal()
    {
        model.setShares(1);
        model.setQuote(BigDecimal.valueOf(100));
        model.setGrossValue(0);
        model.setConvertedGrossValue(0);
        model.setTotal(0);

        assertEquals(model.calculateStatus(), ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnSubTotal)));
    }

    @Test
    public void testCalculationStatus_WithInvalidDataSubTotal()
    {
        model.setShares(1);
        model.setQuote(BigDecimal.valueOf(100));
        model.setGrossValue(100);
        model.setConvertedGrossValue(0);
        model.setTotal(0);

        assertEquals(model.calculateStatus(), ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnSubTotal)));
    }

    @Test
    public void testCalculationStatus_WithInvalidDataQuote()
    {
        model.setShares(1);
        model.setQuote(BigDecimal.valueOf(0));
        model.setGrossValue(100);
        model.setConvertedGrossValue(0);
        model.setTotal(100);

        assertEquals(model.calculateStatus(),
                        ValidationStatus.error(String.format(Messages.CellEditor_NotANumber, model.getQuote())));
    }

    @Test
    public void testCalculateStatus_WithNegativeShares()
    {
        model.setShares(-1);
        model.setQuote(BigDecimal.valueOf(100));

        assertEquals(model.calculateStatus(),
                        ValidationStatus.error(String.format(Messages.CellEditor_NotANumber, model.getShares())));

        assertEquals(0L, model.getTotal());
    }

    @Test
    public void testCalculateStatus_WithNegativeQuote()
    {
        model.setShares(1);
        model.setQuote(BigDecimal.valueOf(-100));

        assertEquals(model.calculateStatus(),
                        ValidationStatus.error(String.format(Messages.CellEditor_NotANumber, model.getQuote())));

        assertEquals(0L, model.getTotal());
    }
}
