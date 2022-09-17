package name.abuchen.portfolio.ui.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;

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
        Locale.setDefault(new Locale("nl", "BE"));
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
        Locale.setDefault(new Locale("fr", "BE"));
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        assertThat(converter.convert("12,34"), is(1234l));
        assertThat(converter.convert("12.34"), is(1234l));
        assertThat(converter.convert("1 234,56"), is(123456l));
    }

    @Test
    public void testValidBEFRAmountWithNBSP()
    {
        Locale.setDefault(new Locale("fr", "BE"));
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        assertThat(converter.convert("12\u00A034,56"), is(123456l));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBEFREAmount()
    {
        Locale.setDefault(new Locale("fr", "BE"));
        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, false);
        converter.convert("1.234,56");
    }
}
