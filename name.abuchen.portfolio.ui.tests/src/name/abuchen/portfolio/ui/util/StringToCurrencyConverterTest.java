package name.abuchen.portfolio.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class StringToCurrencyConverterTest
{
    private static Locale DEFAULT_LOCALE;

    @BeforeClass
    public static void setupLocale()
    {
        DEFAULT_LOCALE = Locale.getDefault();
    }

    @AfterClass
    public static void resetLocale()
    {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAmount()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, true);
        converter.convert("a,b");
    }

    @Test
    public void testValidAmountsDE()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, true);
        assertThat(converter.convert("0"), is(0l));
        assertThat(converter.convert("0,0"), is(0l));
        assertThat(converter.convert("00"), is(0l));
        assertThat(converter.convert("1,234"), is(123l));
        assertThat(converter.convert("1,2"), is(120l));
        assertThat(converter.convert("0,3333"), is(33l));
        assertThat(converter.convert(" 0,3333 "), is(33l));
        assertThat(converter.convert("120 "), is(12000l));
        assertThat(converter.convert("0,585"), is(58l));
    }

    @Test
    public void testValidShareDE()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Share);
        assertThat(converter.convert("1,234"), is(Values.Share.factorize(1.234)));
        assertThat(converter.convert("0,585"), is(Values.Share.factorize(0.585)));
    }

    @Test
    public void testValidDEAmount()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, true);
        assertThat(converter.convert("1.2,34"), is(1234l));
    }

    @Test
    public void testValidDEDecimalAmount()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, true);
        assertThat(converter.convert(",34"), is(34l));
    }

    @Test
    public void testValidDENummberAmount()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, true);
        assertThat(converter.convert("12"), is(1200l));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDENegativeAmount()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, false);
        converter.convert("-12,34");
    }

    @Test
    public void testValidDENegativeAmount()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, true);
        assertThat(converter.convert("-12,34"), is(-1234l));
    }

    @Test
    public void testValidBENLAmount()
    {
        Locale.setDefault(Locale.forLanguageTag("nl-BE"));
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        assertThat(converter.convert("12,34"), is(1234l));
        assertThat(converter.convert(",34"), is(34l));
        assertThat(converter.convert("12.34"), is(1234l));
        assertThat(converter.convert(".34"), is(34l));
        assertThat(converter.convert("12.345,67"), is(1234567l));
    }

    @Test
    public void testValidBEFRAmount()
    {
        Locale.setDefault(Locale.forLanguageTag("fr-BE"));
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        assertThat(converter.convert("12,34"), is(1234l));
        assertThat(converter.convert("12.34"), is(1234l));
        assertThat(converter.convert("1 234,56"), is(123456l));
    }

    @Test
    public void testValidBEFRAmountWithNBSP()
    {
        Locale.setDefault(Locale.forLanguageTag("fr-BE"));
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);

        // Belgian locale has changed between Java 11 and 17

        double version = Double.parseDouble(System.getProperty("java.specification.version")); //$NON-NLS-1$
        String string = version <= 11.0 ? "1\u00a0234,56" : "1\u202f234,56";
        assertThat(converter.convert(string), is(123456l));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBEFREAmount()
    {
        Locale.setDefault(Locale.forLanguageTag("fr-BE"));
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, false);
        converter.convert("1.234,56");
    }

    @Test
    public void testValidArithmetic()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter;
        converter = new StringToCurrencyConverter(Values.Amount, false);
        assertThat(converter.convert("10+10"), is(20_00L));
        assertThat(converter.convert("10,01-10"), is(0_01L));
        assertThat(converter.convert("-1.234,5+2.000,50"), is(766_00L));
        assertThat(converter.convert("5-4+3-2+1"), is(3_00L));
        converter = new StringToCurrencyConverter(Values.Amount, true);
        assertThat(converter.convert("10+10"), is(20_00L));
        assertThat(converter.convert("10,01-10"), is(0_01L));
        assertThat(converter.convert("-1.234,5+2.000,50"), is(766_00L));
        assertThat(converter.convert("5-4+3-2+1"), is(3_00L));
        assertThat(converter.convert("12,34-23,45"), is(-11_11L));
        assertThat(converter.convert("-1234"), is(-1234_00L));
        assertThat(converter.convert("0,00-1.234,00"), is(-1234_00L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArithmetic()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        converter.convert("1234,56-+0,44");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDisallowedNegativeValue()
    {
        Locale.setDefault(Locale.GERMANY);
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, false);
        converter.convert("12,34-23,45");
    }
}
