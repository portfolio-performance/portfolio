package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

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

    @SuppressWarnings("nls")
    public static long convertToNumberLong(String value, Values<Long> valueType, String language, String country) 
    {
        DecimalFormat newNumberFormat = (DecimalFormat) NumberFormat.getInstance(new Locale(language, country));
        
        if (country.equals("CH"))
        {
            /***
             * The group separator for language format German, 
             * region Switzerland (de_CH) changed from Java 10 to Java 11.
             * In 10, it was correctly a ' (ASCII 39), 
             * in Java 11 it prints a ’ (ASCII 8217?) instead.
             *  
             * This is wrong, ASCII 39 was the correct character.
             */
            DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
            decimalFormatSymbols.setDecimalSeparator('.');
            decimalFormatSymbols.setGroupingSeparator('\'');
            newNumberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        }

        try
        {
            return Math.abs(Math.round(newNumberFormat.parse(value).doubleValue() * valueType.factor()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("nls")
    public static BigDecimal convertToNumberBigDecimal(String value, Values<Long> valueType, String language, String country) 
    {
        DecimalFormat newNumberFormat = (DecimalFormat) NumberFormat.getInstance(new Locale(language, country));

        if (country.equals("CH"))
        {
            /***
             * The group separator for language format German, 
             * region Switzerland (de_CH) changed from Java 10 to Java 11.
             * In 10, it was correctly a ' (ASCII 39), 
             * in Java 11 it prints a ’ (ASCII 8217?) instead.
             *  
             * This is wrong, ASCII 39 was the correct character.
             */
            DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
            decimalFormatSymbols.setDecimalSeparator('.');
            decimalFormatSymbols.setGroupingSeparator('\'');
            newNumberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        }

        try
        {
            return BigDecimal.valueOf(newNumberFormat.parse(value).doubleValue());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
}
