package name.abuchen.portfolio.datatransfer.csv;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.text.ParseException;

import org.junit.Test;

@SuppressWarnings("nls")
public class AmountFormatTest
{
    @Test
    public void testCommaDecimalSeperator() throws ParseException
    {
        var amountFormat = new CSVImporter.AmountFormat(',');

        assertThat(amountFormat.parseObject("123"), is(123L));
        assertThat(amountFormat.parseObject("123,45"), is(123.45));
        assertThat(amountFormat.parseObject("1,98E-6"), is(0.00000198));
        assertThat(amountFormat.parseObject("2,12e-6"), is(0.00000212));
        assertThat(amountFormat.parseObject("+ 5%"), is(5L));
        assertThat(amountFormat.parseObject("EUR 42,42"), is(42.42));
        assertThat(amountFormat.parseObject(",42"), is(.42));
        assertThat(amountFormat.parseObject(".42"), is(42L));
        assertThat(amountFormat.parseObject("1,234.56"), is(1.23456));
        assertThat(amountFormat.parseObject("1'345,12"), is(1345.12));
        assertThat(amountFormat.parseObject("1.345,12"), is(1345.12));
        assertThat(amountFormat.parseObject("  1 234,56  "), is(1234.56));
        assertThat(amountFormat.parseObject("  1 234.56  "), is(123456L));
    }

    @Test
    public void testPointDecimalSeperator() throws ParseException
    {
        var amountFormat = new CSVImporter.AmountFormat('.');

        assertThat(amountFormat.parseObject("123"), is(123L));
        assertThat(amountFormat.parseObject("123,45"), is(12345L));
        assertThat(amountFormat.parseObject("1,98E-6"), is(0.000198));
        assertThat(amountFormat.parseObject("2,12e-6"), is(0.000212));
        assertThat(amountFormat.parseObject("+ 5%"), is(5L));
        assertThat(amountFormat.parseObject("EUR 42,42"), is(4242L));
        assertThat(amountFormat.parseObject(",42"), is(42L));
        assertThat(amountFormat.parseObject(".42"), is(0.42));
        assertThat(amountFormat.parseObject("1,234.56"), is(1234.56));
        assertThat(amountFormat.parseObject("1'345,12"), is(134512L));
        assertThat(amountFormat.parseObject("1.345,12"), is(1.34512));
        assertThat(amountFormat.parseObject("  1 234,56  "), is(123456L));
        assertThat(amountFormat.parseObject("  1 234.56  "), is(1234.56));
    }
}
