package name.abuchen.portfolio.datatransfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.Messages;

@SuppressWarnings("nls")
public class ExtractorUtilsDateParserTest
{
    @Test
    public void testAsDateValidFormats()
    {
        // Test valid date strings for each pattern in DATE_FORMATTER_GERMANY
        LocalDateTime expected = LocalDateTime.of(2022, 4, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("11.4.2022"));
        assertEquals(expected, ExtractorUtils.asDate("11.4.22"));
        assertEquals(expected, ExtractorUtils.asDate("2022-4-11"));
        assertEquals(expected, ExtractorUtils.asDate("11-4-2022"));
        assertEquals(expected, ExtractorUtils.asDate("11.04.22"));
        assertEquals(expected, ExtractorUtils.asDate("11-04-2022"));
        assertEquals(expected, ExtractorUtils.asDate("2022-04-11"));
        assertEquals(expected, ExtractorUtils.asDate("11. April 2022"));
        assertEquals(expected, ExtractorUtils.asDate("11/04/2022"));
    }

    @Test(expected = DateTimeParseException.class)
    public void testAsDateInvalidFormat()
    {
        // Test invalid date string
        ExtractorUtils.asDate("11-April-2022");
    }

    @Test
    public void testAsDateValidFormatsWithHints()
    {
        // Test valid date strings for each pattern in
        // DATE_FORMATTER_GERMANY with hints
        LocalDateTime expected = LocalDateTime.of(2023, 4, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("11.4.2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11.4.23", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-4-11", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11-4-2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11.04.23", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11-04-2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-04-11", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11. April 2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11/04/2023", Locale.GERMANY));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_US with hints
        expected = LocalDateTime.of(2023, 4, 9, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("9 Apr 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("09 Apr 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("20230409", Locale.US));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_CANADA with hints
        expected = LocalDateTime.of(2023, 04, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("Apr. 11, 2023", Locale.CANADA));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_CANADA_FRENCH with hints
        expected = LocalDateTime.of(2023, 4, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("11 avr. 2023", Locale.CANADA_FRENCH));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_UK with hints
        expected = LocalDateTime.of(2023, 4, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("11 Apr 2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("04/11/2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("11.04.2023", Locale.UK));
    }

    @Test(expected = DateTimeParseException.class)
    public void testAsDateInvalidFormatWithHints()
    {
        // Test invalid date string with hints
        ExtractorUtils.asDate("11-April-2023", Locale.GERMANY);

        // Test invalid date string with hints
        ExtractorUtils.asDate("11-April-2023", Locale.US);
    }

    @Test
    public void testAsDateCanadaWithFallback()
    {
        String value = "11 Apr 2023";
        LocalDateTime expected = LocalDateTime.of(2023, 4, 11, 0, 0);
        LocalDateTime result = ExtractorUtils.asDate(value, Locale.UK, Locale.CANADA);
        assertEquals(expected, result);
    }

    @Test
    public void testAsDateInvalidValue()
    {
        String value = "invalid date value";
        try
        {
            ExtractorUtils.asDate(value, Locale.GERMANY);
            fail("Expected DateTimeParseException was not thrown");
        }
        catch (DateTimeParseException e)
        {
            assertEquals(MessageFormat.format(Messages.MsgErrorNotAValidDate, value), e.getMessage());
        }
    }

    @Test
    public void testValidFormats()
    {
        // Test various valid formats
        assertEquals(LocalTime.of(13, 15), ExtractorUtils.asTime("11-04-2023 13:15"));
        assertEquals(LocalTime.of(8, 0), ExtractorUtils.asTime("11/04/2023 08:00:00"));
    }

    @Test(expected = DateTimeParseException.class)
    public void testInvalidFormat()
    {
        // Test an invalid format
        ExtractorUtils.asTime("2023-04-11");
    }

    @Test(expected = DateTimeParseException.class)
    public void testInvalidValue()
    {
        // Test an invalid value that cannot be parsed by any formatter
        ExtractorUtils.asTime("not a time");
    }

    @Test(expected = NullPointerException.class)
    public void testNullValue()
    {
        // Test a null value
        ExtractorUtils.asTime(null);
    }
}
