package name.abuchen.portfolio.online.impl;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.junit.Test;

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

        assertEquals(result, 2775300L);
    }
    
    @Test
    public void asPriceWithoutFractionTest() throws ParseException
    {
        String priceFromYahoo = "277";

        long result = YahooHelper.asPrice(priceFromYahoo);

        assertEquals(result, 2770000L);
    }
}
