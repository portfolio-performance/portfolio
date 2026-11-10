package name.abuchen.portfolio.datatransfer.csv;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Stream;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.util.TextUtil;

@SuppressWarnings("nls")
public class AmountFieldTest
{
    private static final String COMMA = "0.000,00";
    private static final String DOT = "0,000.00";

    private static String guess(String value)
    {
        return new AmountField("value", "Value").guessFormat(null, value).getCode();
    }

    @Test
    public void testDecimalSeparatorIsReadFromValue()
    {
        assertThat(guess("100.00"), is(DOT));
        assertThat(guess("100,00"), is(COMMA));
        assertThat(guess("1.234,56"), is(COMMA));
        assertThat(guess("1,234.56"), is(DOT));
        assertThat(guess("1.234.567"), is(COMMA));
        assertThat(guess("1,234,567"), is(DOT));
        assertThat(guess("2.12e-6"), is(DOT));
        assertThat(guess("1.5E10"), is(DOT));
        assertThat(guess("1.234,56. "), is(COMMA));
        assertThat(guess("100,00 EUR."), is(COMMA));
    }

    @Test
    public void testLaterValueDecidesAnAmbiguousFirstValue()
    {
        var format = new AmountField("value", "Value").guessFormatOf(null, Stream.of("1,000", "1,000.50"));
        assertThat(format.getCode(), is(DOT));
    }

    @Test
    public void testAmbiguousValueFallsBackToLocale()
    {
        var locale = TextUtil.DECIMAL_SEPARATOR == '.' ? DOT : COMMA;

        assertThat(guess("1,234"), is(locale));
        assertThat(guess("1.234 (2024)"), is(locale));
        assertThat(guess("1234"), is(locale));
        assertThat(guess(null), is(locale));
    }
}
