package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

class PDFExtractorUtils
{
    @SuppressWarnings("nls")
    public static void checkAndSetTax(Money tax, name.abuchen.portfolio.model.Transaction t, DocumentType type) 
    {
        if (tax.getCurrencyCode().equals(t.getCurrencyCode()))
        {
            t.addUnit(new Unit(Unit.Type.TAX, tax));
        }
        else if (type.getCurrentContext().containsKey("exchangeRate"))
        {
            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));
            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

            Money txTax = Money.of(t.getCurrencyCode(),
                            BigDecimal.valueOf(tax.getAmount()).multiply(inverseRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

            // store tax value in both currencies, if security's currency
            // is different to transaction currency
            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
            {
                t.addUnit(new Unit(Unit.Type.TAX, txTax));
            }
            else
            {
                t.addUnit(new Unit(Unit.Type.TAX, txTax, tax, inverseRate));
            }
        }
    }

    @SuppressWarnings("nls")
    public static void checkAndSetFee(Money fee, name.abuchen.portfolio.model.Transaction t, DocumentType type) 
    {
        if (fee.getCurrencyCode().equals(t.getCurrencyCode()))
        {
            t.addUnit(new Unit(Unit.Type.FEE, fee));
        }
        else if (type.getCurrentContext().containsKey("exchangeRate"))
        {
            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));
            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

            Money fxFee = Money.of(t.getCurrencyCode(),
                            BigDecimal.valueOf(fee.getAmount()).multiply(inverseRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

            // store fee value in both currencies, if security's currency
            // is different to transaction currency
            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
            {
                t.addUnit(new Unit(Unit.Type.FEE, fxFee));
            }
            else
            {
                t.addUnit(new Unit(Unit.Type.FEE, fxFee, fee, inverseRate));
            }
        }
    }
}
