package name.abuchen.portfolio.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings("nls")
public class IsinTest
{
    @Test
    public void testValidIsin()
    {
        String ubsIsin = "CH0244767585";
        assertTrue(Isin.isValid(ubsIsin));

        String adidasIsin = "DE000A1EWWW0";
        assertTrue(Isin.isValid(adidasIsin));

        String toyotaIsin = "JP3633400001";
        assertTrue(Isin.isValid(toyotaIsin));
    }

    @Test
    public void testInvalidIsin()
    {
        String invalidUbsIsin = "CH0244767586"; // Wrong Checksum
        assertFalse(Isin.isValid(invalidUbsIsin));
    }

    @Test
    public void testIsinInvalidLength()
    {
        String isinTooLong = "CH0244767585222222";
        assertFalse(Isin.isValid(isinTooLong));

        String isinTooShort = "CH02381";
        assertFalse(Isin.isValid(isinTooShort));
    }

    @Test
    public void testIsinNull()
    {
        String nullIsin = null;
        assertFalse(Isin.isValid(nullIsin));
    }

    @Test
    public void testInvalidChar()
    {
        String invalidCharIsin = "ÜE0244767585";
        assertFalse(Isin.isValid(invalidCharIsin));
    }
}
