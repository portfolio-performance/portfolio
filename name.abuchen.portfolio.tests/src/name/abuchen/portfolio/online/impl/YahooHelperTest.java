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
    
    @Test(expected = ParseException.class)
    public void asPriceExceptionTest() throws ParseException
    {
        String priceFromYahoo = "a277";

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

        long result = YahooHelper.asNumber(priceFromYahoo);
    }    
}
