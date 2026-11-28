package name.abuchen.portfolio.ui.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.swt.graphics.RGB;
import org.junit.After;
import org.junit.Test;

import name.abuchen.portfolio.util.ColorConversion;

@SuppressWarnings("nls")
public class ValueColorSchemeTest
{
    @After
    public void resetCustomScheme()
    {
        var custom = ValueColorScheme.getCustomScheme();
        custom.setDefaultPositiveForeground(ColorConversion.hex2RGBA("#007A00"));
        custom.setDefaultNegativeForeground(ColorConversion.hex2RGBA("#C62828"));
        ValueColorScheme.setCustomPositiveColor(null);
        ValueColorScheme.setCustomNegativeColor(null);
    }

    @Test
    public void testCustomSchemeAndValidColors()
    {
        var custom = ValueColorScheme.getCustomScheme();
        assertThat(custom, is(notNullValue()));
        assertThat(custom.getIdentifier(), is(ValueColorScheme.CUSTOM_SCHEME));

        ValueColorScheme.setCustomPositiveColor("#112233");
        assertThat(custom.hasCustomPositive(), is(true));
        assertThat(custom.positiveForeground().getRGB(), is(new RGB(0x11, 0x22, 0x33)));
        assertThat(custom.upArrow(), is(notNullValue()));
        assertThat(custom.upArrow().getBounds().width, is(16));

        ValueColorScheme.setCustomNegativeColor("#445566");
        assertThat(custom.hasCustomNegative(), is(true));
        assertThat(custom.negativeForeground().getRGB(), is(new RGB(0x44, 0x55, 0x66)));
        assertThat(custom.downArrow(), is(notNullValue()));
        assertThat(custom.downArrow().getBounds().width, is(16));
    }

    @Test
    public void testInvalidOrBlankHexRestoresDefaults()
    {
        var custom = ValueColorScheme.getCustomScheme();
        var defaultPos = custom.defaultPositiveForeground().getRGB();
        var defaultNeg = custom.defaultNegativeForeground().getRGB();

        ValueColorScheme.setCustomPositiveColor("#123456");
        ValueColorScheme.setCustomPositiveColor("not-a-hex");
        assertThat(custom.hasCustomPositive(), is(false));
        assertThat(custom.positiveForeground().getRGB(), is(defaultPos));

        ValueColorScheme.setCustomNegativeColor("#654321");
        ValueColorScheme.setCustomNegativeColor("   ");
        assertThat(custom.hasCustomNegative(), is(false));
        assertThat(custom.negativeForeground().getRGB(), is(defaultNeg));
    }

    @Test
    public void testDefaultForegroundFallback()
    {
        var custom = ValueColorScheme.getCustomScheme();
        custom.setDefaultPositiveForeground(ColorConversion.hex2RGBA("#00E000"));
        custom.setDefaultNegativeForeground(ColorConversion.hex2RGBA("#FF6B6B"));

        assertThat(custom.defaultPositiveForeground().getRGB(), is(new RGB(0x00, 0xE0, 0x00)));
        assertThat(custom.defaultNegativeForeground().getRGB(), is(new RGB(0xFF, 0x6B, 0x6B)));
    }
}
