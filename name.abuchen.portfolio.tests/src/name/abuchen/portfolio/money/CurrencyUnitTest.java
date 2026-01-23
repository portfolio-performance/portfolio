package name.abuchen.portfolio.money;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Locale;

import org.junit.Test;

@SuppressWarnings("nls")
public class CurrencyUnitTest
{
    @Test
    public void testGetDefaultInstanceWithValidCountry()
    {
        var result = CurrencyUnit.getDefaultInstance(Locale.US);
        assertThat(result.getCurrencyCode(), is("USD"));

        result = CurrencyUnit.getDefaultInstance(Locale.GERMANY);
        assertThat(result.getCurrencyCode(), is("EUR"));

        result = CurrencyUnit.getDefaultInstance(Locale.JAPAN);
        assertThat(result.getCurrencyCode(), is("JPY"));
    }

    @Test
    public void testGetDefaultInstanceWithLocaleWithoutCountry()
    {
        var result = CurrencyUnit.getDefaultInstance(Locale.ENGLISH);
        assertThat(result.getCurrencyCode(), is("EUR"));

        result = CurrencyUnit.getDefaultInstance(Locale.FRENCH);
        assertThat(result.getCurrencyCode(), is("EUR"));

        result = CurrencyUnit.getDefaultInstance(Locale.GERMAN);
        assertThat(result.getCurrencyCode(), is("EUR"));

        var localeWithoutCountry = Locale.forLanguageTag("es");
        result = CurrencyUnit.getDefaultInstance(localeWithoutCountry);
        assertThat(result.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void testGetDefaultInstanceWithUNM49AreaCode()
    {
        // JDK does not know a currency for an area - should return EUR
        // (fallback)

        // Test with UN M.49 area code 001 (World)
        var localeWithAreaCode001 = Locale.forLanguageTag("en-001");
        var result = CurrencyUnit.getDefaultInstance(localeWithAreaCode001);
        assertThat(result.getCurrencyCode(), is("EUR"));

        // Test with UN M.49 area code 150 (Europe)
        var localeWithAreaCode150 = Locale.forLanguageTag("en-150");
        result = CurrencyUnit.getDefaultInstance(localeWithAreaCode150);
        assertThat(result.getCurrencyCode(), is("EUR"));

        // Test with UN M.49 area code 021 (Northern America)
        var localeWithAreaCode021 = Locale.forLanguageTag("en-021");
        result = CurrencyUnit.getDefaultInstance(localeWithAreaCode021);
        assertThat(result.getCurrencyCode(), is("EUR"));

        // Test with UN M.49 area code 419 (Latin America)
        var localeWithAreaCode419 = Locale.forLanguageTag("es-419");
        result = CurrencyUnit.getDefaultInstance(localeWithAreaCode419);
        assertThat(result.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void testGetDefaultInstanceWithInvalidLocale()
    {
        var invalidLocale = Locale.forLanguageTag("en-XX");
        var result = CurrencyUnit.getDefaultInstance(invalidLocale);
        assertThat(result.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void testGetDefaultInstanceFallbackToEUR()
    {
        var result = CurrencyUnit.getDefaultInstance(Locale.ROOT);
        assertThat(result.getCurrencyCode(), is("EUR"));
    }
}
