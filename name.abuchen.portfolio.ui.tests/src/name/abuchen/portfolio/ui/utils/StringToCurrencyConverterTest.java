package name.abuchen.portfolio.ui.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;

@SuppressWarnings("nls")
public class StringToCurrencyConverterTest
{
    @Test
    public void testValidDEAmount()
    {
        Locale.setDefault(new Locale("DE", "DE"));
        
        String input = "1.2,34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        Long output = converter.convert(input);
        
        Long expectedResult = 1234l ;
        assertEquals(output, expectedResult);
    }
    
    @Test
    public void testInvalidDENegativeAmount()
    {
        Locale.setDefault(new Locale("DE", "DE"));
        
        String input = "-12,34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, false);
        
        try
        {
            converter.convert(input);
            fail("Expected a IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Keine gültige Zahl: -12,34");
        }
   
    }
    
    @Test
    public void testValidDENegativeAmount()
    {
        Locale.setDefault(new Locale("DE", "DE"));
        
        String input = "-12,34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount, true);
        Long output = converter.convert(input);
        
        Long expectedResult = -1234l ;
        assertEquals(output, expectedResult);
    }
    
    @Test
    public void testValidBENLAmount()
    {
        Locale.setDefault(new Locale("NL", "BE"));
        
        String input = "12,34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        Long output = converter.convert(input);
        
        Long expectedResult = 1234l ;
        assertEquals(output, expectedResult);
    }
    
    @Test
    public void testValidBENLAmountWithOnlyDecimals()
    {
        Locale.setDefault(new Locale("NL", "BE"));
        
        String input = ",34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        Long output = converter.convert(input);
        
        Long expectedResult = 34l ;
        assertEquals(output, expectedResult);
    }
    
    @Test
    public void testValidBENLAmountWithDotSeperator()
    {
        Locale.setDefault(new Locale("NL", "BE"));
        
        String input = "12.34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        Long output = converter.convert(input);
        
        Long expectedResult = 1234l ;
        assertEquals(output, expectedResult);
    }
    
    @Test
    public void testValidBENLAmountWithDotSeperatorAndOnlyDecimals()
    {
        Locale.setDefault(new Locale("NL", "BE"));
        
        String input = ".34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        Long output = converter.convert(input);
        
        Long expectedResult = 34l ;
        assertEquals(output, expectedResult);
    }
    
    @Test
    public void testValidBEFRAmount()
    {
        Locale.setDefault(new Locale("FR", "BE"));
        
        String input = "12,34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        Long output = converter.convert(input);
        
        Long expectedResult = 1234l ;
        assertEquals(output, expectedResult);
    }
    
    @Test
    public void testValidBEFRAmountWithDotSeperator()
    {
        Locale.setDefault(new Locale("FR", "BE"));
        
        String input = "12.34";

        StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
        Long output = converter.convert(input);
        
        Long expectedResult = 1234l ;
        assertEquals(output, expectedResult);
    }
}
