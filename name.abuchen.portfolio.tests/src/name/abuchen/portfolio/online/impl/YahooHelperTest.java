package name.abuchen.portfolio.online.impl;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.junit.Test;

import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class YahooHelperTest
{

    @Test
    public void asPriceNotAvailableTest() throws ParseException
    {
        String priceFromYahoo = "N/A";

        long result = YahooHelper.asPrice(priceFromYahoo);

        assertEquals(result, -1L);
    }

    @Test
    public void asPriceNullTest() throws ParseException
    {
        String priceFromYahoo = "null";

        long result = YahooHelper.asPrice(priceFromYahoo);

        assertEquals(result, -1L);
    }

    @Test
    public void asPriceSimpleTest() throws ParseException
    {
        String priceFromYahoo = "277.5300";

        long result = YahooHelper.asPrice(priceFromYahoo);

        assertEquals(result, Values.Quote.factorize(277.53));
    }

    @Test
    public void asPriceWithoutFractionTest() throws ParseException
    {
        String priceFromYahoo = "277";

        long result = YahooHelper.asPrice(priceFromYahoo);

        assertEquals(result, Values.Quote.factorize(277));
    }

    @Test(expected = ParseException.class)
    public void asPriceExceptionTest() throws ParseException
    {
        String priceFromYahoo = "a277";

        @SuppressWarnings("unused")
        long result = YahooHelper.asPrice(priceFromYahoo);
    }

    @Test
    public void asNumberNotAvailableTest() throws ParseException
    {
        String priceFromYahoo = "N/A";

        int result = YahooHelper.asNumber(priceFromYahoo);

        assertEquals(result, -1);
    }

    @Test
    public void asNumberNullTest() throws ParseException
    {
        String priceFromYahoo = "null";

        int result = YahooHelper.asNumber(priceFromYahoo);

        assertEquals(result, -1);
    }

    @Test
    public void asNumberSimpleTest() throws ParseException
    {
        String priceFromYahoo = "277";

        int result = YahooHelper.asNumber(priceFromYahoo);

        assertEquals(result, 277);
    }

    @Test
    public void asNumberWithFractionTest() throws ParseException
    {
        String priceFromYahoo = "277.02";

        int result = YahooHelper.asNumber(priceFromYahoo);

        assertEquals(result, 277);
    }

    @Test(expected = ParseException.class)
    public void asNumberExceptionTest() throws ParseException
    {
        String priceFromYahoo = "a277";

        @SuppressWarnings("unused")
        long result = YahooHelper.asNumber(priceFromYahoo);
    }

    @Test
    public void asDateNotAvailableTest()
    {
        String dateFromYahoo = "\"N/A\"";

        LocalDate result = YahooHelper.asDate(dateFromYahoo);

        assertEquals(result, null);
    }

    @Test
    public void asDateSimpleTest()
    {
        String dateFromYahoo = "\"04/20/2020\"";

        LocalDate result = YahooHelper.asDate(dateFromYahoo);

        assertEquals(result, LocalDate.of(2020, 4, 20));
    }

    @Test(expected = DateTimeParseException.class)
    public void asDateExceptionTest() throws ParseException
    {
        String dateFromYahoo = "\"A4/20/2020\"";

        @SuppressWarnings("unused")
        LocalDate result = YahooHelper.asDate(dateFromYahoo);
    }

    @Test
    public void fromISODateNotAvailableTest()
    {
        String isoDateFromYahoo = "\"N/A\"";

        LocalDate result = YahooHelper.fromISODate(isoDateFromYahoo);

        assertEquals(result, null);
    }

    @Test
    public void fromISODateNullTest()
    {
        String isoDateFromYahoo = null;

        LocalDate result = YahooHelper.fromISODate(isoDateFromYahoo);

        assertEquals(result, null);
    }

    @Test
    public void fromISODateNullStringTest()
    {
        String isoDateFromYahoo = "null";

        LocalDate result = YahooHelper.fromISODate(isoDateFromYahoo);

        assertEquals(result, null);
    }

    @Test
    public void fromISODateSimpleTest()
    {
        String isoDateFromYahoo = "2020-04-19";

        LocalDate result = YahooHelper.fromISODate(isoDateFromYahoo);

        assertEquals(result, LocalDate.of(2020, 4, 19));
    }

    @Test(expected = DateTimeParseException.class)
    public void fromISODateExceptionTest() throws ParseException
    {
        String dateFromYahoo = "\"A4/20/2020\"";

        @SuppressWarnings("unused")
        LocalDate result = YahooHelper.fromISODate(dateFromYahoo);
    }
}
