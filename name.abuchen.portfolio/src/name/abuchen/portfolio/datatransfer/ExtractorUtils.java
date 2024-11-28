package name.abuchen.portfolio.datatransfer;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.AdditionalLocales;

@SuppressWarnings("nls")
public class ExtractorUtils
{
    private ExtractorUtils()
    {
    }

    // Helper method to create case-insensitive DateTimeFormatter instances
    private static DateTimeFormatter createFormatter(String pattern, Locale locale)
    {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(locale);
    }

    // Date formatters with case-insensitive support for Germany
    private static final DateTimeFormatter[] DATE_FORMATTER_GERMANY = { //
                    createFormatter("d.M.yy", Locale.GERMANY), //
                    createFormatter("dd.M.yy", Locale.GERMANY), //
                    createFormatter("d.M.yyyy", Locale.GERMANY), //
                    createFormatter("dd.M.yyyy", Locale.GERMANY), //
                    createFormatter("d.MM.yy", Locale.GERMANY), //
                    createFormatter("dd.MM.yy", Locale.GERMANY), //
                    createFormatter("d MMM yy", Locale.GERMANY), //
                    createFormatter("dd MMM yy", Locale.GERMANY), //
                    createFormatter("d MMM yyyy", Locale.GERMANY), //
                    createFormatter("dd MMM yyyy", Locale.GERMANY), //
                    createFormatter("d. LLL yyyy", Locale.GERMANY), //
                    createFormatter("dd. LLL yyyy", Locale.GERMANY), //
                    createFormatter("d-M-yyyy", Locale.GERMANY), //
                    createFormatter("dd-M-yyyy", Locale.GERMANY), //
                    createFormatter("d-MM-yyyy", Locale.GERMANY), //
                    createFormatter("dd-MM-yyyy", Locale.GERMANY), //
                    createFormatter("d-MMM-yyyy", Locale.GERMANY), //
                    createFormatter("dd-MMM-yyyy", Locale.GERMANY), //
                    createFormatter("d MMMM yy", Locale.GERMANY), //
                    createFormatter("dd MMMM yy", Locale.GERMANY), //
                    createFormatter("d. MMMM yy", Locale.GERMANY), //
                    createFormatter("dd. MMMM yy", Locale.GERMANY), //
                    createFormatter("d MMMM yyyy", Locale.GERMANY), //
                    createFormatter("dd MMMM yyyy", Locale.GERMANY), //
                    createFormatter("d. MMMM yyyy", Locale.GERMANY), //
                    createFormatter("dd. MMMM yyyy", Locale.GERMANY), //
                    createFormatter("yyyy-M-d", Locale.GERMANY), //
                    createFormatter("yyyy-M-dd", Locale.GERMANY), //
                    createFormatter("yyyy-MM-d", Locale.GERMANY), //
                    createFormatter("yyyy-MM-dd", Locale.GERMANY), //
                    createFormatter("d/MM/yyyy", Locale.GERMANY), //
                    createFormatter("dd/MM/yyyy", Locale.GERMANY), //
                    createFormatter("d/M/yyyy", Locale.GERMANY), //
                    createFormatter("dd/M/yyyy", Locale.GERMANY) };

    // Date formatters with case-insensitive support for French
    private static final DateTimeFormatter[] DATE_FORMATTER_FRENCH = { //
                    createFormatter("d MMM yyyy", Locale.FRENCH), //
                    createFormatter("dd MMM yyyy", Locale.FRENCH), //
                    createFormatter("d MMMM yyyy", Locale.FRENCH), //
                    createFormatter("dd MMMM yyyy", Locale.FRENCH), //
                    createFormatter("d MMM. yyyy", Locale.FRENCH), //
                    createFormatter("dd MMM. yyyy", Locale.FRENCH) };

    // Date formatters with case-insensitive support for the United States
    private static final DateTimeFormatter[] DATE_FORMATTER_US = { //
                    createFormatter("d LLL yyyy", Locale.US), //
                    createFormatter("dd LLL yyyy", Locale.US), //
                    createFormatter("d LLLL yyyy", Locale.US), //
                    createFormatter("dd LLLL yyyy", Locale.US), //
                    createFormatter("d-LLL-yyyy", Locale.US), //
                    createFormatter("dd-LLL-yyyy", Locale.US), //
                    createFormatter("yyyyLLd", Locale.US), //
                    createFormatter("yyyyLLdd", Locale.US), //
                    createFormatter("yyyyLLLd", Locale.US), //
                    createFormatter("yyyyLLLdd", Locale.US), //
                    createFormatter("LL-d-yy", Locale.US), //
                    createFormatter("LL-dd-yy", Locale.US), //
                    createFormatter("LL-d-yyyy", Locale.US), //
                    createFormatter("LL-dd-yyyy", Locale.US), //
                    createFormatter("LL/d/yy", Locale.US), //
                    createFormatter("LL/dd/yy", Locale.US), //
                    createFormatter("LL/d/yyyy", Locale.US), //
                    createFormatter("LL/dd/yyyy", Locale.US), //
                    createFormatter("LLL/d/yyyy", Locale.US), //
                    createFormatter("LLL/dd/yyyy", Locale.US), //
                    createFormatter("LLL d, yyyy", Locale.US), //
                    createFormatter("LLL dd, yyyy", Locale.US), //
                    createFormatter("LLLL/d/yyyy", Locale.US), //
                    createFormatter("LLLL/dd/yyyy", Locale.US), //
                    createFormatter("LLLL d, yyyy", Locale.US), //
                    createFormatter("LLLL dd, yyyy", Locale.US), };

    // Date formatters with case-insensitive support for Canada
    private static final DateTimeFormatter[] DATE_FORMATTER_CANADA = { //
                    createFormatter("LLL d, yyyy", Locale.CANADA), //
                    createFormatter("LLL. d, yyyy", Locale.CANADA), //
                    createFormatter("LLL dd, yyyy", Locale.CANADA), //
                    createFormatter("LLL. dd, yyyy", Locale.CANADA) };

    // Date formatters with case-insensitive support for Canadian French
    private static final DateTimeFormatter[] DATE_FORMATTER_CANADA_FRENCH = { //
                    createFormatter("d LLL yyyy", Locale.CANADA_FRENCH), //
                    createFormatter("dd LLL yyyy", Locale.CANADA_FRENCH) };

    // Date formatters with case-insensitive support for Spanish (Spain)
    private static final DateTimeFormatter[] DATE_FORMATTER_SPAIN = {
                    createFormatter("d MMM yyyy", AdditionalLocales.SPAIN),
                    createFormatter("dd MMM yyyy", AdditionalLocales.SPAIN),
                    createFormatter("d MMMM yyyy", AdditionalLocales.SPAIN),
                    createFormatter("dd MMMM yyyy", AdditionalLocales.SPAIN) };

    // Date formatters with case-insensitive support for Spanish (Mexico)
    private static final DateTimeFormatter[] DATE_FORMATTER_MEXICO = {
                    createFormatter("d MMM yyyy", AdditionalLocales.MEXICO),
                    createFormatter("dd MMM yyyy", AdditionalLocales.MEXICO),
                    createFormatter("d MMMM yyyy", AdditionalLocales.MEXICO),
                    createFormatter("dd MMMM yyyy", AdditionalLocales.MEXICO) };

    // Date formatters with case-insensitive support for the United Kingdom
    private static final DateTimeFormatter[] DATE_FORMATTER_UK = { //
                    createFormatter("d LLLL yyyy", Locale.UK), //
                    createFormatter("dd LLLL yyyy", Locale.UK), //
                    createFormatter("d LLL yyyy", Locale.UK), //
                    createFormatter("dd LLL yyyy", Locale.UK), //
                    createFormatter("LL/d/yyyy", Locale.UK), //
                    createFormatter("LL/dd/yyyy", Locale.UK), //
                    createFormatter("L/d/yyyy", Locale.UK), //
                    createFormatter("L/dd/yyyy", Locale.UK), //
                    createFormatter("d.LL.yyyy", Locale.UK), //
                    createFormatter("dd.LL.yyyy", Locale.UK), //
                    createFormatter("LLL/d/yyyy", Locale.UK), //
                    createFormatter("LLL/dd/yyyy", Locale.UK), //
                    createFormatter("LLLL/d/yyyy", Locale.UK), //
                    createFormatter("LLLL/dd/yyyy", Locale.UK) };

    // Map associating locales with their respective date formatters
    private static final Map<Locale, DateTimeFormatter[]> LOCALE2DATE = Map.of( //
                    Locale.GERMANY, DATE_FORMATTER_GERMANY, //
                    Locale.FRENCH, DATE_FORMATTER_FRENCH, //
                    Locale.US, DATE_FORMATTER_US, //
                    Locale.CANADA, DATE_FORMATTER_CANADA, //
                    Locale.CANADA_FRENCH, DATE_FORMATTER_CANADA_FRENCH, //
                    Locale.UK, DATE_FORMATTER_UK, //
                    AdditionalLocales.SPAIN, DATE_FORMATTER_SPAIN, //
                    AdditionalLocales.MEXICO, DATE_FORMATTER_MEXICO);

    // DateTime formatters with case-insensitive support for various locales
    private static final DateTimeFormatter[] DATE_TIME_FORMATTER = { //
                    createFormatter("d.M.yyyy HH:mm", Locale.GERMANY), //
                    createFormatter("dd.M.yyyy HH:mm", Locale.GERMANY), //
                    createFormatter("d-MM-yyyy HH:mm", Locale.GERMANY), //
                    createFormatter("dd-MM-yyyy HH:mm", Locale.GERMANY), //
                    createFormatter("d-MM-yyyy HH:mm", Locale.GERMANY), //
                    createFormatter("dd-MM-yyyy HH:mm", Locale.GERMANY), //
                    createFormatter("d-MM-yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("dd-MM-yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("d.MM.yyyy HH.mm.ss", Locale.GERMANY), //
                    createFormatter("dd.MM.yyyy HH.mm.ss", Locale.GERMANY), //
                    createFormatter("d.MM.yyyy H:mm:ss", Locale.GERMANY), //
                    createFormatter("dd.MM.yyyy H:mm:ss", Locale.GERMANY), //
                    createFormatter("d LLL yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("dd LLL yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("d MMMM yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("dd MMMM yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("d. MMMM yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("dd. MMMM yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("d.M.yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("dd.M.yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("d/MM/yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("dd/MM/yyyy HH:mm:ss", Locale.GERMANY), //
                    createFormatter("LLL d, yyyy hh:mm:ss a", Locale.US), //
                    createFormatter("LLL dd, yyyy hh:mm:ss a", Locale.US), //
                    createFormatter("yyyy-LL-dd HH:mm:ss", Locale.US), //
                    createFormatter("yyyy-LL-d HH:mm:ss", Locale.US), //
                    createFormatter("yyyy-LL-dd HH:mm:ss", Locale.US), //
                    createFormatter("yyyyLLd HHmmss", Locale.US), //
                    createFormatter("yyyyLLdd HHmmss", Locale.US), //
                    createFormatter("d/LL/yyyy HH:mm", Locale.UK), //
                    createFormatter("dd/LL/yyyy HH:mm", Locale.UK), //
                    createFormatter("d.LL.yyyy hh:mm:ss a", Locale.UK), //
                    createFormatter("dd.LL.yyyy hh:mm:ss a", Locale.UK), //
                    createFormatter("d LLL yyyy HH:mm:ss", Locale.UK), //
                    createFormatter("dd LLL yyyy HH:mm:ss", Locale.UK), //
                    createFormatter("d/LL/yy HH.mm", Locale.UK), //
                    createFormatter("dd/LL/yy HH.mm", Locale.UK), //
                    createFormatter("d/LL/yy HH.mm", Locale.UK), //
                    createFormatter("dd/LL/yy HH.mm", Locale.UK) };

    public static void checkAndSetGrossUnit(Money gross, Money fxGross, Object transaction, DocumentContext context)
    {
        if (transaction instanceof name.abuchen.portfolio.model.Transaction tx)
            ExtractorUtils.checkAndSetGrossUnit(gross, fxGross, tx, context);
        else if (transaction instanceof BuySellEntry buysell)
            ExtractorUtils.checkAndSetGrossUnit(gross, fxGross, buysell.getPortfolioTransaction(), context);
        else
            throw new UnsupportedOperationException();
    }

    public static void checkAndSetGrossUnit(Money gross, Money fxGross, name.abuchen.portfolio.model.Transaction t,
                    DocumentContext context)
    {
        if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
            return;

        Optional<ExtrExchangeRate> rate = context.getType(ExtrExchangeRate.class);

        if (rate.isPresent())
            t.addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, rate.get().getRate(gross.getCurrencyCode())));
    }

    public static void checkAndSetTax(Money tax, Object transaction, DocumentContext context)
    {
        if (transaction instanceof name.abuchen.portfolio.model.Transaction tx)
            ExtractorUtils.checkAndSetTax(tax, tx, context);
        else if (transaction instanceof BuySellEntry buySell)
            ExtractorUtils.checkAndSetTax(tax, buySell.getPortfolioTransaction(), context);
        else
            throw new UnsupportedOperationException();
    }

    public static void checkAndSetTax(Money tax, name.abuchen.portfolio.model.Transaction t, DocumentContext context)
    {
        if (tax.getCurrencyCode().equals(t.getCurrencyCode()))
        {
            t.addUnit(new Unit(Unit.Type.TAX, tax));
            return;
        }

        Optional<ExtrExchangeRate> rate = context.getType(ExtrExchangeRate.class);

        if (rate.isPresent())
        {
            Money fxTax = rate.get().convert(t.getCurrencyCode(), tax);

            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                t.addUnit(new Unit(Unit.Type.TAX, fxTax));
            else
                t.addUnit(new Unit(Unit.Type.TAX, fxTax, tax, rate.get().getRate(t.getCurrencyCode())));

        }
    }

    public static void checkAndSetFee(Money fee, Object transaction, DocumentContext context)
    {
        if (transaction instanceof name.abuchen.portfolio.model.Transaction tx)
            ExtractorUtils.checkAndSetFee(fee, tx, context);
        else if (transaction instanceof BuySellEntry buysell)
            ExtractorUtils.checkAndSetFee(fee, buysell.getPortfolioTransaction(), context);
        else
            throw new UnsupportedOperationException();
    }

    public static void checkAndSetFee(Money fee, name.abuchen.portfolio.model.Transaction t, DocumentContext context)
    {
        if (fee.getCurrencyCode().equals(t.getCurrencyCode()))
        {
            t.addUnit(new Unit(Unit.Type.FEE, fee));
            return;
        }

        Optional<ExtrExchangeRate> rate = context.getType(ExtrExchangeRate.class);

        if (rate.isPresent())
        {
            Money fxFee = rate.get().convert(t.getCurrencyCode(), fee);

            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                t.addUnit(new Unit(Unit.Type.FEE, fxFee));
            else
                t.addUnit(new Unit(Unit.Type.FEE, fxFee, fee, rate.get().getRate(t.getCurrencyCode())));
        }
    }

    public static long convertToNumberLong(String value, Values<Long> valueType, String language, String country)
    {
        DecimalFormat newNumberFormat = (DecimalFormat) NumberFormat
                        .getInstance(Locale.forLanguageTag(language + "-" + country));

        /**
         * @formatter:off
         *
         * The group separator for language format French is a space followed by the decimal separator comma.
         * fr_FR --> 1 234,56
         *
         * If the amount has the group separator space, the string can still have the following format.
         * xh_ZA --> 1 234.56
         *
         * To simplify the two variants, we remove the spaces in the string, bypassing the generation of an additional identification via the locale.
         *
         * The problem is that we can only pass one main formatting per importer.
         * e.g.
         * Importer: PostfinancePDFExtractor.java
         * Test file: Kauf01.txt vs. Kauf04.txt
         * We remove the spaces from the complete string and bypass the formatting.
         *
         * @formatter:on
         */
        value = trim(value).replaceAll("\\s", "");

        if ("CH".equals(country))
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

    public static BigDecimal convertToNumberBigDecimal(String value, Values<Long> valueType, String language,
                    String country)
    {
        /**
         * @formatter:off
         *
         * The group separator for language format French is a space followed by the decimal separator comma.
         * fr_FR --> 1 234,56
         *
         * If the amount has the group separator space, the string can still have the following format.
         * xh_ZA --> 1 234.56
         *
         * To simplify the two variants, we remove the spaces in the string, bypassing the generation of an additional identification via the locale.
         *
         * The problem is that we can only pass one main formatting per importer.
         * e.g.
         * Importer: PostfinancePDFExtractor.java
         * Test file: Kauf01.txt vs. Kauf04.txt
         * We remove the spaces from the complete string and bypass the formatting.
         *
         * @formatter:on
         */
        value = trim(value).replaceAll("\\s", "");

        DecimalFormat newNumberFormat = (DecimalFormat) NumberFormat
                        .getInstance(Locale.forLanguageTag(language + "-" + country));

        if ("CH".equals(country))
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
        // starting with Java 8, the abbreviation Mrz is not supported out of
        // the box anymore
        value = value.replaceAll("(?i)\\bMrz\\b", "Mär");

        Locale[] locales = hints.length > 0 ? hints
                        : new Locale[] { Locale.GERMANY, Locale.FRENCH, Locale.US, Locale.CANADA, Locale.CANADA_FRENCH,
                                        Locale.UK, AdditionalLocales.SPAIN, AdditionalLocales.MEXICO };

        for (Locale l : locales)
        {
            DateTimeFormatter[] formatters = LOCALE2DATE.get(l);
            if (formatters == null)
            {
                continue; // Skip this locale if no formatters are found
            }

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

        throw new DateTimeParseException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value), value, 0);
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

        throw new DateTimeParseException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value), value, 0);
    }

    public static LocalDateTime asDate(String date, String time)
    {
        // starting with Java 8, the abbreviation Mrz is not supported out of
        // the box anymore
        date = date.replaceAll("(?i)\\bMrz\\b", "Mär");

        String value = String.format("%s %s", date, time);

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

        throw new DateTimeParseException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value), value, 0);
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
