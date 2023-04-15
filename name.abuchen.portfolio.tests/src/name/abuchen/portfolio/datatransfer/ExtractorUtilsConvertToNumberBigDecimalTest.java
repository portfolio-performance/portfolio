package name.abuchen.portfolio.datatransfer;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.money.Values;

@RunWith(Parameterized.class)
@SuppressWarnings("nls")
public class ExtractorUtilsConvertToNumberBigDecimalTest
{
    private String input;
    private Locale locale;
    private BigDecimal expectedOutput;

    public ExtractorUtilsConvertToNumberBigDecimalTest(String input, Locale locale, BigDecimal expectedOutput)
    {
        this.input = input;
        this.locale = locale;
        this.expectedOutput = expectedOutput;
    }

    @Parameters(name = "{index}: convertToNumberBigDecimal({0}, {1}) = {2}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] { //
                        { "1 234,56", new Locale("fr", "FR"), new BigDecimal("1234.56") }, //
                        { "1,234.56", new Locale("en", "US"), new BigDecimal("1234.56") }, //
                        { "1'234.56", new Locale("de", "CH"), new BigDecimal("1234.56") }, //
                        { "1.234,56", new Locale("de", "DE"), new BigDecimal("1234.56") }, //
                        { "1 234.56", new Locale("xh", "ZA"), new BigDecimal("1234.56") }, //
                        { "0,56", new Locale("fr", "FR"), new BigDecimal("0.56") }, //
                        { "0.56", new Locale("en", "US"), new BigDecimal("0.56") }, //
                        { "0.56", new Locale("de", "CH"), new BigDecimal("0.56") }, //
                        { "0,56", new Locale("de", "DE"), new BigDecimal("0.56") }, //
                        { "0.56", new Locale("xh", "ZA"), new BigDecimal("0.56") }, //
        });
    }

    @Test
    public void testConvertToNumberBigDecimal()
    {
        BigDecimal actualOutput = ExtractorUtils.convertToNumberBigDecimal(input, null, locale.getLanguage(),
                        locale.getCountry());
        assertEquals(expectedOutput, actualOutput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertToNumberBigDecimalWithInvalidInput()
    {
        String input = "abc";
        Locale locale = new Locale("de", "DE");
        ExtractorUtils.convertToNumberBigDecimal(input, Values.Share, locale.getLanguage(), locale.getCountry());
    }
}
