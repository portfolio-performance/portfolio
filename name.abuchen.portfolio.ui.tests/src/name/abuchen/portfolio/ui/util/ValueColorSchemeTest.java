package name.abuchen.portfolio.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.graphics.RGB;
import org.junit.After;
import org.junit.Test;

@SuppressWarnings("nls")
public class ValueColorSchemeTest
{
    @After
    public void tearDown()
    {
        ValueColorScheme.initialize(ValueColorScheme.STANDARD_SCHEME);
    }

    @Test
    public void testAvailableSchemes()
    {
        var schemes = ValueColorScheme.getAvailableSchemes();
        assertThat(schemes.size(), is(5));

        var ids = schemes.stream().map(ValueColorScheme::getIdentifier).toList();
        assertTrue(ids.contains(ValueColorScheme.STANDARD_SCHEME));
        assertTrue(ids.contains("blueorange"));
        assertTrue(ids.contains("asia"));
        assertTrue(ids.contains("vivid"));
        assertTrue(ids.contains(ValueColorScheme.CUSTOM_SCHEME));
    }

    @Test
    public void testCustomSchemeProperties()
    {
        var custom = ValueColorScheme.getCustomScheme();
        assertThat(custom, is(notNullValue()));
        assertTrue(custom.isCustom());
        assertThat(custom.getIdentifier(), is(ValueColorScheme.CUSTOM_SCHEME));
    }

    @Test
    public void testSetCustomColors()
    {
        var custom = ValueColorScheme.getCustomScheme();
        RGB testPos = new RGB(10, 200, 50);
        RGB testNeg = new RGB(220, 30, 40);

        custom.setPositiveForeground(testPos);
        custom.setNegativeForeground(testNeg);

        assertThat(custom.positiveForeground().getRGB(), is(testPos));
        assertThat(custom.negativeForeground().getRGB(), is(testNeg));
    }

    @Test
    public void testInitializeCustomScheme()
    {
        ValueColorScheme.initialize(ValueColorScheme.CUSTOM_SCHEME);
        assertThat(ValueColorScheme.current(), is(ValueColorScheme.getCustomScheme()));
    }
}
