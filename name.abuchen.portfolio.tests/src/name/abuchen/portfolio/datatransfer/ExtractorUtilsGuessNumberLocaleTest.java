package name.abuchen.portfolio.datatransfer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings("nls")
public class ExtractorUtilsGuessNumberLocaleTest
{
    private static final Locale CH = Locale.of("de", "CH");

    private final String input;
    private final Locale fallback;
    private final Locale expected;

    public ExtractorUtilsGuessNumberLocaleTest(String input, Locale fallback, Locale expected)
    {
        this.input = input;
        this.fallback = fallback;
        this.expected = expected;
    }

    @Parameters(name = "{index}: guess {0} (fallback {1}) should be {2}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] { //
                        // both separators -> right-most one is the decimal
                        { "1,234.56", Locale.GERMANY, Locale.US }, //
                        { "1.234,56", Locale.US, Locale.GERMANY }, //

                        // a lone dot is a decimal point
                        { "63.726878", Locale.GERMANY, Locale.US }, //
                        { "0.0353", Locale.GERMANY, Locale.US }, //
                        { "1.25", Locale.GERMANY, Locale.US }, //
                        { "0.353", Locale.GERMANY, Locale.US }, //
                        { "77.638", Locale.GERMANY, Locale.US}, //
                        { "1.262", Locale.GERMANY, Locale.US }, //
                        { "1.000", Locale.GERMANY, Locale.US }, //
                        { "1.000", Locale.US, Locale.US }, //

                        // single comma is a decimal point
                        { "1,25", Locale.US, Locale.GERMANY }, //
                        { "0,353", Locale.US, Locale.GERMANY }, //
                        { "1,000", Locale.US, Locale.GERMANY }, //
                        { "1,000", Locale.GERMANY, Locale.GERMANY }, //
                        { "35,000", Locale.GERMANY, Locale.GERMANY }, //

                        // repeated separator -> grouping
                        { "1.234.567", Locale.US, Locale.GERMANY }, //
                        { "1,234,567", Locale.GERMANY, Locale.US }, //

                        // Swiss apostrophe grouping
                        { "12'345.67", Locale.GERMANY, CH }, //
                        { "1'234'567", Locale.US, CH }, //

                        // no separator -> fallback
                        { "1234", Locale.GERMANY, Locale.GERMANY }, //
                        { "1234", Locale.US, Locale.US }, //
        });
    }

    @Test
    public void testGuessNumberLocale()
    {
        assertEquals(expected, ExtractorUtils.guessNumberLocale(input, fallback));
    }
}
