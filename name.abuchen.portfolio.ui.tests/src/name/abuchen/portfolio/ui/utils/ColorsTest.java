package name.abuchen.portfolio.ui.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.swt.graphics.Color;
import org.junit.Test;

import name.abuchen.portfolio.ui.util.Colors;

public class ColorsTest
{
    private final static Color SWT_BLACK = Colors.getColor(0, 0, 0);
    private final static Color SWT_WHITE = Colors.getColor(255, 255, 255);

    @Test
    public void testGetTextColor()
    {
        assertThat(Colors.getTextColor(Colors.theme().greenBackground()), is(SWT_BLACK));
        assertThat(Colors.getTextColor(Colors.theme().redBackground()), is(SWT_WHITE));
    }
}
