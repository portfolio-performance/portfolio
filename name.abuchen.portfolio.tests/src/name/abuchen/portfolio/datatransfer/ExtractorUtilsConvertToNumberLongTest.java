package name.abuchen.portfolio.datatransfer;

import static org.junit.Assert.assertEquals;

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
public class ExtractorUtilsConvertToNumberLongTest
{
    private final String input;
    private final Locale locale;
    private final long expectedOutput;

    public ExtractorUtilsConvertToNumberLongTest(String input, Locale locale, long expectedOutput)
    {
        this.input = input;
        this.locale = locale;
        this.expectedOutput = expectedOutput;
    }

    @Parameters(name = "{index}: convert {0} to ({1}) should return {2}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] { //
                        { "1 234,56", Locale.forLanguageTag("fr-FR"), 123456 }, //
                        { "1,234.56", Locale.forLanguageTag("en-US"), 123456 }, //
                        { "1'234.56", Locale.forLanguageTag("de-CH"), 123456 }, //
                        { "1.234,56", Locale.forLanguageTag("de-DE"), 123456 }, //
                        { "1 234.56", Locale.forLanguageTag("xh-ZA"), 123456 }, //
        });
    }

    @Test
    public void testConvertToNumberLong()
    {
        long actualOutput = ExtractorUtils.convertToNumberLong(input, Values.Amount, locale.getLanguage(),
                        locale.getCountry());
        assertEquals(expectedOutput, actualOutput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertToNumberLongWithInvalidInput()
    {
        ExtractorUtils.convertToNumberLong("abc", Values.Amount, locale.getLanguage(), locale.getCountry());
    }
}
