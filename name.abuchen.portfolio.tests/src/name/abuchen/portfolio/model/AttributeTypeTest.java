package name.abuchen.portfolio.model;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.model.AttributeType.AmountConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AttributeTypeTest
{
    private static Locale DEFAULT_LOCALE = Locale.getDefault();

    @BeforeClass
    public static void setupLocale()
    {
        DEFAULT_LOCALE = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
    }

    @AfterClass
    public static void resetLocale()
    {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    public void testLongParsing()
    {
        AmountConverter converter = new AmountConverter();
        assertThat(converter.fromString("1.234,56"), is(Values.Amount.factorize(1234.56)));
        assertThat(converter.fromString("1.234,562"), is(Values.Amount.factorize(1234.56)));

        // no rounding, just cut off
        assertThat(converter.fromString("1.234,569"), is(Values.Amount.factorize(1234.56)));
    }

    @Test
    public void testPercentParsing()
    {
        PercentConverter converter = new PercentConverter();
        assertThat(converter.fromString("22%"), is(0.22));
        assertThat(converter.fromString("22"), is(0.22));
        assertThat(converter.fromString("12,34%"), is(0.1234));
        assertThat(converter.fromString("12,34567%"), is(0.1234567));
        
        assertThat(converter.toString(converter.fromString("22%")), is("22,00%"));
        assertThat(converter.toString(converter.fromString("22")), is("22,00%"));
    }
}
