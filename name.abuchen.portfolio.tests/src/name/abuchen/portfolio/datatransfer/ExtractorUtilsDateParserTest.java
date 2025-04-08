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
        assertEquals(expected, ExtractorUtils.asDate("11.4.22"));
        assertEquals(expected, ExtractorUtils.asDate("11.04.22"));
        assertEquals(expected, ExtractorUtils.asDate("11-4-2022"));
        assertEquals(expected, ExtractorUtils.asDate("11-04-2022"));
        assertEquals(expected, ExtractorUtils.asDate("11/4/2022"));
        assertEquals(expected, ExtractorUtils.asDate("11/04/2022"));
        assertEquals(expected, ExtractorUtils.asDate("11. April 2022"));
        assertEquals(expected, ExtractorUtils.asDate("2022-4-11"));
        assertEquals(expected, ExtractorUtils.asDate("2022-04-11"));
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
        assertEquals(expected, ExtractorUtils.asDate("11.4.23", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11.04.23", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11.4.2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11.04.2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11/4/2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11/04/2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11. April 23", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11. April 2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("11. APRIL 2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-4-11", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-04-11", Locale.GERMANY));

        expected = LocalDateTime.of(2023, 4, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("1.4.2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01.4.2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1.04.2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01.04.2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1-4-2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01-4-2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1-04-2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01-04-2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1/4/2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01/4/2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1/04/2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01/04/2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1 April 23", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01 April 23", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1. April 2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01. April 2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("1. APRIL 2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("01. APRIL 2023", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-4-1", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-4-01", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-04-1", Locale.GERMANY));
        assertEquals(expected, ExtractorUtils.asDate("2023-04-01", Locale.GERMANY));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_FRENCH with hints
        expected = LocalDateTime.of(2024, 7, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("01 juil. 2024", Locale.FRENCH));

        expected = LocalDateTime.of(2024, 8, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("01 août 2024", Locale.FRENCH));

        expected = LocalDateTime.of(2024, 2, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("01 Février 2024", Locale.FRENCH));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_US with hints
        expected = LocalDateTime.of(2023, 4, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("11 Apr 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("11 April 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04-11-23", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04-11-2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04/11/23", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04/11/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("Apr/11/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("Apr 11, 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("April/11/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("April 11, 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("2023Apr11", Locale.US));

        expected = LocalDateTime.of(2023, 4, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("1 Apr 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("01 Apr 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04-1-23", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04-01-23", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04-1-2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04-01-2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04/1/23", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04/01/23", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04/1/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("04/01/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("Apr/1/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("Apr/01/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("Apr 1, 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("Apr 01, 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("April/1/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("April/01/2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("April 1, 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("April 01, 2023", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("2023Apr1", Locale.US));
        assertEquals(expected, ExtractorUtils.asDate("2023Apr01", Locale.US));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_CANADA with hints
        expected = LocalDateTime.of(2023, 04, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("Apr. 11, 2023", Locale.CANADA));

        expected = LocalDateTime.of(2023, 4, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("Apr. 1, 2023", Locale.CANADA));
        assertEquals(expected, ExtractorUtils.asDate("Apr. 01, 2023", Locale.CANADA));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_CANADA_FRENCH with hints
        expected = LocalDateTime.of(2023, 4, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("11 avr. 2023", Locale.CANADA_FRENCH));

        expected = LocalDateTime.of(2023, 4, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("1 avr. 2023", Locale.CANADA_FRENCH));
        assertEquals(expected, ExtractorUtils.asDate("01 avr. 2023", Locale.CANADA_FRENCH));

        // Test valid date strings for each pattern in
        // DATE_FORMATTER_UK with hints
        expected = LocalDateTime.of(2023, 4, 11, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("04/11/2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("11.04.2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("11 Apr 2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("11 APR 2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("11 April 2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("11 APRIL 2023", Locale.UK));

        expected = LocalDateTime.of(2023, 4, 1, 0, 0);
        assertEquals(expected, ExtractorUtils.asDate("4/1/2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("4/01/2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("04/1/2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("01.04.2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("1 Apr 2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("01 Apr 2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("1 APR 2023", Locale.UK));
        assertEquals(expected, ExtractorUtils.asDate("01 APR 2023", Locale.UK));
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
