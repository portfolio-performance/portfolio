package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class PDFExtractorUtils
{
    private static final DateTimeFormatter[] DATE_FORMATTER_GERMANY = { //
                    DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d.M.yy", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("yyyy-M-d", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d-M-yyyy", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("dd.MM.yy", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.GERMANY) }; //$NON-NLS-1$

    private static final DateTimeFormatter[] DATE_FORMATTER_US = { //
                    DateTimeFormatter.ofPattern("dd LLL yyyy", Locale.US), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US) }; //$NON-NLS-1$

    private static final DateTimeFormatter[] DATE_FORMATTER_UK = { //
                    DateTimeFormatter.ofPattern("dd LLL yyyy", Locale.UK), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("LL/dd/yyyy", Locale.UK) }; //$NON-NLS-1$

    private static final Map<Locale, DateTimeFormatter[]> LOCALE2DATE = Map.of(Locale.GERMANY, DATE_FORMATTER_GERMANY,
                    Locale.US, DATE_FORMATTER_US, Locale.UK, DATE_FORMATTER_UK);

    private static final DateTimeFormatter[] DATE_TIME_FORMATTER = { //
                    DateTimeFormatter.ofPattern("d.M.yyyy HH:mm", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d LLL yyyy HH:mm:ss", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d. MMMM yyyy HH:mm:ss", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm.ss", Locale.GERMANY), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US) }; //$NON-NLS-1$

    private PDFExtractorUtils()
    {
    }

    public static void checkAndSetGrossUnit(Money gross, Money fxGross, Object transaction, DocumentType type)
    {
        if (transaction instanceof name.abuchen.portfolio.model.Transaction)
            PDFExtractorUtils.checkAndSetGrossUnit(gross, fxGross,
                            (name.abuchen.portfolio.model.Transaction) transaction, type);
        else if (transaction instanceof BuySellEntry)
            PDFExtractorUtils.checkAndSetGrossUnit(gross, fxGross,
                            ((BuySellEntry) transaction).getPortfolioTransaction(), type);
        else
            throw new UnsupportedOperationException();
    }

    public static void checkAndSetGrossUnit(Money gross, Money fxGross, name.abuchen.portfolio.model.Transaction t,
                    DocumentType type)
    {
        if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
            return;

        Optional<PDFExchangeRate> rate = type.getCurrentContext().getType(PDFExchangeRate.class);

        if (rate.isPresent())
        {
            t.addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, rate.get().getRate(gross.getCurrencyCode())));
        }
    }

    public static void checkAndSetTax(Money tax, Object transaction, DocumentType type)
    {
        if (transaction instanceof name.abuchen.portfolio.model.Transaction)
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) transaction, type);
        else if (transaction instanceof BuySellEntry)
            PDFExtractorUtils.checkAndSetTax(tax, ((BuySellEntry) transaction).getPortfolioTransaction(), type);
        else
            throw new UnsupportedOperationException();
    }

    public static void checkAndSetTax(Money tax, name.abuchen.portfolio.model.Transaction t, DocumentType type)
    {
        if (tax.getCurrencyCode().equals(t.getCurrencyCode()))
        {
            t.addUnit(new Unit(Unit.Type.TAX, tax));
            return;
        }

        Optional<PDFExchangeRate> rate = type.getCurrentContext().getType(PDFExchangeRate.class);

        if (rate.isPresent())
        {
            Money fxTax = rate.get().convert(t.getCurrencyCode(), tax);

            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                t.addUnit(new Unit(Unit.Type.TAX, fxTax));
            else
                t.addUnit(new Unit(Unit.Type.TAX, fxTax, tax, rate.get().getRate(t.getCurrencyCode())));

        }
    }

    public static void checkAndSetFee(Money fee, Object transaction, DocumentType type)
    {
        if (transaction instanceof name.abuchen.portfolio.model.Transaction)
            PDFExtractorUtils.checkAndSetFee(fee, (name.abuchen.portfolio.model.Transaction) transaction, type);
        else if (transaction instanceof BuySellEntry)
            PDFExtractorUtils.checkAndSetFee(fee, ((BuySellEntry) transaction).getPortfolioTransaction(), type);
        else
            throw new UnsupportedOperationException();
    }

    public static void checkAndSetFee(Money fee, name.abuchen.portfolio.model.Transaction t, DocumentType type)
    {
        if (fee.getCurrencyCode().equals(t.getCurrencyCode()))
        {
            t.addUnit(new Unit(Unit.Type.FEE, fee));
            return;
        }

        Optional<PDFExchangeRate> rate = type.getCurrentContext().getType(PDFExchangeRate.class);

        if (rate.isPresent())
        {
            Money fxFee = rate.get().convert(t.getCurrencyCode(), fee);

            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                t.addUnit(new Unit(Unit.Type.FEE, fxFee));
            else
                t.addUnit(new Unit(Unit.Type.FEE, fxFee, fee, rate.get().getRate(t.getCurrencyCode())));
        }
    }

    @SuppressWarnings("nls")
    public static long convertToNumberLong(String value, Values<Long> valueType, String language, String country)
    {
        DecimalFormat newNumberFormat = (DecimalFormat) NumberFormat.getInstance(new Locale(language, country));

        if (country.equals("CH"))
        {
            /***
             * The group separator for language format German, region
             * Switzerland (de_CH) changed from Java 10 to Java 11. In 10, it
             * was correctly a ' (ASCII 39), in Java 11 it prints a ’ (ASCII
             * 8217?) instead. This is wrong, ASCII 39 was the correct
             * character.
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
    public static BigDecimal convertToNumberBigDecimal(String value, Values<Long> valueType, String language,
                    String country)
    {
        DecimalFormat newNumberFormat = (DecimalFormat) NumberFormat.getInstance(new Locale(language, country));

        if (country.equals("CH"))
        {
            /***
             * The group separator for language format German, region
             * Switzerland (de_CH) changed from Java 10 to Java 11. In 10, it
             * was correctly a ' (ASCII 39), in Java 11 it prints a ’ (ASCII
             * 8217?) instead. This is wrong, ASCII 39 was the correct
             * character.
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

    public static long asShares(String value, String language, String country)
    {
        return convertToNumberLong(value, Values.Share, language, country);
    }

    public static LocalDateTime asDate(String value, Locale... hints)
    {
        Locale[] locales = hints.length > 0 ? hints : new Locale[] { Locale.GERMANY, Locale.US, Locale.UK };

        for (Locale l : locales)
        {
            for (DateTimeFormatter formatter : LOCALE2DATE.get(l))
            {
                try
                {
                    return LocalDate.parse(value, formatter).atStartOfDay();
                }
                catch (DateTimeParseException ignore)
                {
                    // continue with next formatter
                }
            }
        }

        throw new DateTimeParseException(Messages.MsgErrorNotAValidDate, value, 0);
    }

    public static LocalTime asTime(String value)
    {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTER)
        {
            try
            {
                return LocalTime.parse(value, formatter).withSecond(0);
            }
            catch (DateTimeParseException ignore)
            {
                // continue with next formatter
            }
        }

        throw new DateTimeParseException(Messages.MsgErrorNotAValidDate, value, 0);
    }

    public static LocalDateTime asDate(String date, String time)
    {
        String value = String.format("%s %s", date, time); //$NON-NLS-1$

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTER)
        {
            try
            {
                return LocalDateTime.parse(value, formatter);
            }
            catch (DateTimeParseException ignore)
            {
                // continue with next formatter
            }
        }

        throw new DateTimeParseException(Messages.MsgErrorNotAValidDate, value, 0);
    }

    public static Consumer<Transaction> fixGrossValue()
    {
        return t -> {
            // check if the relevant transaction properties have been parsed
            if (t.getCurrencyCode() == null || t.getSecurity() == null || t.getSecurity().getCurrencyCode() == null)
                return;

            // if transaction currency equals to the currency of
            // the security, then there is no forex information required
            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                return;

            // check if the reported gross value fits to the
            // expected gross value
            Optional<Unit> actualGross = t.getUnit(Unit.Type.GROSS_VALUE);

            if (actualGross.isPresent())
            {
                Unit grossUnit = actualGross.get();
                Money expectedGross = t.getGrossValue();

                if (!expectedGross.equals(grossUnit.getAmount()))
                {
                    // check if it a rounding difference that is acceptable
                    try
                    {
                        Unit u = new Unit(Unit.Type.GROSS_VALUE, expectedGross, grossUnit.getForex(),
                                        grossUnit.getExchangeRate());

                        t.removeUnit(grossUnit);
                        t.addUnit(u);
                        return;
                    }
                    catch (IllegalArgumentException ignore)
                    {
                        // recalculate the unit to fix the gross value
                    }

                    Money caculatedGrossValue = Money.of(grossUnit.getForex().getCurrencyCode(),
                                    BigDecimal.valueOf(expectedGross.getAmount())
                                                    .divide(grossUnit.getExchangeRate(), Values.MC)
                                                    .setScale(0, RoundingMode.HALF_EVEN).longValue());

                    t.removeUnit(grossUnit);
                    t.addUnit(new Unit(Unit.Type.GROSS_VALUE, expectedGross, caculatedGrossValue,
                                    grossUnit.getExchangeRate()));
                }
            }
            else
            {
                // create gross value once we know forex
                // currency (if currency is stored with base and
                // term currency in context)
            }
        };
    }

    public static Consumer<AccountTransaction> fixGrossValueA()
    {
        return t -> fixGrossValue().accept(t);
    }

    public static Consumer<BuySellEntry> fixGrossValueBuySell()
    {
        return t -> fixGrossValue().accept(t.getPortfolioTransaction());
    }
}
