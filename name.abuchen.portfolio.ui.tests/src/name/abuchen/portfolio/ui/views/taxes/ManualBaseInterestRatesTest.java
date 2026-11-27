package name.abuchen.portfolio.ui.views.taxes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("nls")
public class ManualBaseInterestRatesTest
{
    private static Locale defaultLocale;

    @BeforeClass
    public static void setupLocale()
    {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @AfterClass
    public static void resetLocale()
    {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testRatesArePersistedAndResetIndependently() throws IOException
    {
        var preferences = new PreferenceStore();
        var rates = new ManualBaseInterestRates(preferences);
        rates.put(2024, new BigDecimal("2.00"));
        rates.put(2025, BigDecimal.ZERO);

        var restored = new ManualBaseInterestRates(reload(preferences));
        assertThat(restored.get(2024), is(Optional.of(new BigDecimal("2.00"))));
        assertThat(restored.get(2025), is(Optional.of(BigDecimal.ZERO)));

        rates.remove(2024);
        restored = new ManualBaseInterestRates(reload(preferences));
        assertThat(restored.get(2024), is(Optional.empty()));
        assertThat(restored.get(2025), is(Optional.of(BigDecimal.ZERO)));
    }

    private static PreferenceStore reload(PreferenceStore preferences) throws IOException
    {
        var data = new ByteArrayOutputStream();
        preferences.save(data, null);
        var restored = new PreferenceStore();
        restored.load(new ByteArrayInputStream(data.toByteArray()));
        return restored;
    }

    @Test
    public void testParsePercentAcceptsBothDecimalSeparatorsWithoutGrouping()
    {
        assertThat(ManualBaseInterestRates.parsePercent("2,53"), is(new BigDecimal("2.53")));
        assertThat(ManualBaseInterestRates.parsePercent("2.53"), is(new BigDecimal("2.53")));
        assertThat(ManualBaseInterestRates.parsePercent("1.234,56"), is(nullValue()));
        assertThat(ManualBaseInterestRates.parsePercent(" 2,53 % "), is(new BigDecimal("2.53")));
        assertThat(ManualBaseInterestRates.parsePercent("-0,45"), is(new BigDecimal("-0.45")));
        assertThat(ManualBaseInterestRates.parsePercent("1,005"), is(new BigDecimal("1.01")));
    }

    @Test
    public void testParsePercentRejectsInvalidInput()
    {
        assertThat(ManualBaseInterestRates.parsePercent(""), is(nullValue()));
        assertThat(ManualBaseInterestRates.parsePercent("%"), is(nullValue()));
        assertThat(ManualBaseInterestRates.parsePercent("abc"), is(nullValue()));
        assertThat(ManualBaseInterestRates.parsePercent("2,5x"), is(nullValue()));
        assertThat(ManualBaseInterestRates.parsePercent(null), is(nullValue()));
    }
}
