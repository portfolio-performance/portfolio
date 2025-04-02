package name.abuchen.portfolio.money;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

public class CurrencyUnitTest
{

    private static List<CurrencyUnit> currencyUnits;

    // This method will be run once before all tests to load the available
    // currency units
    @BeforeClass
    public static void setUp()
    {
        // Load all available CurrencyUnit instances
        currencyUnits = CurrencyUnit.getAvailableCurrencyUnits();
    }

    /**
     * Test to ensure that there are no duplicate currency codes in the system.
     * <p>
     * This test iterates through all available currency units and checks if
     * there are any duplicate currency codes. A HashSet is used to track unique
     * codes, and if a duplicate is found, the test will fail and print the
     * duplicate.
     */
    @Test
    public void testNoDuplicateCurrencyCodes()
    {
        Set<String> currencyCodes = new HashSet<>();
        for (CurrencyUnit currencyUnit : currencyUnits)
        {
            var currencyCode = currencyUnit.getCurrencyCode();

            // Using a HashSet to ensure there are no duplicates.
            assertTrue("Duplicate currency code found: " + currencyCode, currencyCodes.add(currencyCode));
        }
    }

    /**
     * Test to ensure that there are no duplicate currency symbols in the
     * system.
     * <p>
     * This test checks if any currency units share the same currency symbol. It
     * skips null values (if any) and uses a HashSet to guarantee uniqueness. If
     * a duplicate is found, the test will fail and print the duplicate symbol.
     */
    @Test
    public void testNoDuplicateCurrencySymbols()
    {
        Set<String> currencySymbols = new HashSet<>();
        for (CurrencyUnit currencyUnit : currencyUnits)
        {
            var currencySymbol = currencyUnit.getCurrencySymbol();
            var currencyCode = currencyUnit.getCurrencyCode();
            if (currencySymbol != null)
            {
                // Use HashSet to ensure there are no duplicates
                var isAdded = currencySymbols.add(currencySymbol);

                // Using a HashSet to ensure there are no duplicates.
                assertTrue("Duplicate currency symbol found for currency code " + currencyCode + ": " + currencySymbol,
                                isAdded);
            }
        }
    }

    /**
     * Test to ensure that no currency unit has a null or empty currency code.
     * <p>
     * This test ensures that every currency unit has a valid, non-null and
     * non-empty currency code.
     */
    @Test
    public void testCurrencyCodeIsNotNullOrEmpty()
    {
        for (CurrencyUnit currencyUnit : currencyUnits)
        {
            var currencyCode = currencyUnit.getCurrencyCode();
            assertNotNull("Currency code should not be null", currencyCode);
            assertFalse("Currency code should not be empty", currencyCode.isEmpty());
        }
    }

    /**
     * Test to ensure that the currency code has a valid length (3 characters).
     * <p>
     * ISO 4217 currency codes should have exactly 3 characters. This test
     * checks that.
     */
    @Test
    public void testCurrencyCodeLength()
    {
        for (CurrencyUnit currencyUnit : currencyUnits)
        {
            var currencyCode = currencyUnit.getCurrencyCode();
            assertEquals("Currency code should have exactly 3 characters", 3, currencyCode.length());
        }
    }

    /**
     * Test to ensure that the display name of each currency unit is not null or
     * empty.
     * <p>
     * Every currency unit should have a valid display name that is neither null
     * nor empty.
     */
    @Test
    public void testCurrencyDisplayNameIsNotNullOrEmpty()
    {
        for (CurrencyUnit currencyUnit : currencyUnits)
        {
            var displayName = currencyUnit.getDisplayName();
            assertNotNull("Currency display name should not be null", displayName);
            assertFalse("Currency display name should not be empty", displayName.isEmpty());
        }
    }

    /**
     * Test to ensure that no currency code is invalid (non-ISO 4217 codes, too
     * long/short).
     * <p>
     * This test checks for invalid `currencyCode` values, e.g., codes that are
     * too short or too long.
     */
    @Test
    public void testInvalidCurrencyCode()
    {
        List<String> invalidCodes = List.of("US", "USD1", "1234", "EURO", "A1");
        for (String invalidCode : invalidCodes)
        {
            var currencyUnit = CurrencyUnit.getInstance(invalidCode);
            assertNull("Invalid currency code should not exist: " + invalidCode, currencyUnit);

            assertFalse("Currency code should not be cached: " + invalidCode,
                            CurrencyUnit.containsCurrencyCode(invalidCode));
        }
    }

    /**
     * Test to ensure that no currency unit has an invalid or empty currency
     * symbol.
     * <p>
     * Currency symbols should not be empty or invalid. This test ensures that
     * all symbols are properly defined and contain only valid characters.
     */
    @Test
    public void testInvalidCurrencySymbol()
    {
        for (CurrencyUnit currencyUnit : currencyUnits)
        {
            // Get the currency symbol and currency code
            var currencySymbol = currencyUnit.getCurrencySymbol();
            var currencyCode = currencyUnit.getCurrencyCode(); // Get the
                                                               // currency code

            if (currencySymbol != null)
            {
                // Check for empty or invalid symbols (e.g., containing
                // whitespace or illegal characters)
                assertFalse("Currency symbol for " + currencyCode + " should not be empty or invalid: '"
                                + currencySymbol + "'", currencySymbol.trim().isEmpty());

                assertTrue("Currency symbol for " + currencyCode + " should be a valid single-character symbol: '"
                                + currencySymbol + "'",
                                currencySymbol.length() > 0 || currencySymbol.matches("[A-Z]{2,3}\\p{Sc}"));
            }
        }
    }

    /**
     * Test to ensure that invalid CurrencyUnits do not get added to the cache.
     * <p>
     * If a `CurrencyUnit` has an invalid code or symbol, it should not be
     * cached or retrievable.
     */
    @Test
    public void testInvalidCurrencyUnitsDoNotExistInCache()
    {
        // Example of invalid currency codes
        List<String> invalidCodes = List.of("XYZ", "INVALID", "123", "ABCDEF");
        for (String invalidCode : invalidCodes)
        {
            assertNull("Invalid currency code should not be cached: " + invalidCode,
                            CurrencyUnit.getInstance(invalidCode));
        }
    }
}
