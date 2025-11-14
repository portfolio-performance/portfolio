package name.abuchen.portfolio.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assume.assumeTrue;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Test;

public class SWTHelperTest
{
    @Before
    public void setUp()
    {
        // Skip tests in headless environments (e.g., CI/CD without X11/Wayland)
        try
        {
            Display display = Display.getCurrent();
            if (display == null)
                display = Display.getDefault();

            // If we get here, display is available
            assumeTrue("Display not available in headless environment", display != null && !display.isDisposed());
        }
        catch (SWTError | UnsatisfiedLinkError e)
        {
            // Display initialization failed (headless environment)
            assumeTrue("SWT Display not available: " + e.getMessage(), false);
        }
    }

    @Test
    public void testGetDPI()
    {
        Point dpi = SWTHelper.getDPI();

        assertThat(dpi, is(notNullValue()));
        assertThat(dpi.x, greaterThan(0));
        assertThat(dpi.y, greaterThan(0));

        // Most displays have the same horizontal and vertical DPI
        assertThat(dpi.x, is(dpi.y));
    }

    @Test
    public void testGetDPIScalingFactor()
    {
        double scalingFactor = SWTHelper.getDPIScalingFactor();

        // Scaling factor should be positive
        assertThat(scalingFactor, greaterThan(0.0));

        // Windows supports DPI scaling from 100% to 400%
        // Typical scaling factors: 1.0 (100%), 1.25 (125%), 1.5 (150%), 1.75 (175%),
        // 2.0 (200%), 2.25 (225%), 2.5 (250%), 3.0 (300%), 3.5 (350%), 4.0 (400%)
        // At minimum should be 0.5 (50%) and at most 5.0 (500%) for safety margin
        assertThat("Scaling factor should be at least 0.5", scalingFactor, greaterThan(0.5));
        assertThat("Scaling factor should not exceed 5.0", scalingFactor < 5.0, is(true));
    }

    @Test
    public void testScalePixelInt()
    {
        double scalingFactor = SWTHelper.getDPIScalingFactor();

        // Test scaling of 100 pixels
        int scaled100 = SWTHelper.scalePixel(100);
        int expected100 = (int) Math.round(100 * scalingFactor);
        assertThat(scaled100, is(expected100));

        // Test scaling of 0 pixels
        int scaled0 = SWTHelper.scalePixel(0);
        assertThat(scaled0, is(0));

        // Test scaling of 1 pixel
        int scaled1 = SWTHelper.scalePixel(1);
        int expected1 = (int) Math.round(1 * scalingFactor);
        assertThat(scaled1, is(expected1));
    }

    @Test
    public void testScalePixelDouble()
    {
        double scalingFactor = SWTHelper.getDPIScalingFactor();

        // Test scaling of 100.0 pixels
        double scaled100 = SWTHelper.scalePixel(100.0);
        double expected100 = 100.0 * scalingFactor;
        assertThat(scaled100, is(expected100));

        // Test scaling of 0.0 pixels
        double scaled0 = SWTHelper.scalePixel(0.0);
        assertThat(scaled0, is(0.0));

        // Test scaling of 50.5 pixels
        double scaled50_5 = SWTHelper.scalePixel(50.5);
        double expected50_5 = 50.5 * scalingFactor;
        assertThat(scaled50_5, is(expected50_5));
    }

    @Test
    public void testGetDPIDebugInfo()
    {
        String debugInfo = SWTHelper.getDPIDebugInfo();

        assertThat(debugInfo, is(notNullValue()));

        // Debug info should contain DPI values
        assertThat(debugInfo.contains("DPI:"), is(true));
        assertThat(debugInfo.contains("Scaling:"), is(true));
        assertThat(debugInfo.contains("%"), is(true));
    }

    @Test
    public void testDPIScalingFactorConsistency()
    {
        // Call multiple times to ensure caching works correctly
        double factor1 = SWTHelper.getDPIScalingFactor();
        double factor2 = SWTHelper.getDPIScalingFactor();

        assertThat(factor1, is(factor2));
    }

    @Test
    public void testWindowsScalingLevels()
    {
        double scalingFactor = SWTHelper.getDPIScalingFactor();

        // Test common Windows scaling levels
        if (Math.abs(scalingFactor - 1.0) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(100));
        }
        else if (Math.abs(scalingFactor - 1.25) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(125));
        }
        else if (Math.abs(scalingFactor - 1.5) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(150));
        }
        else if (Math.abs(scalingFactor - 1.75) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(175));
        }
        else if (Math.abs(scalingFactor - 2.0) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(200));
        }
        else if (Math.abs(scalingFactor - 2.25) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(225));
        }
        else if (Math.abs(scalingFactor - 2.5) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(250));
        }
        else if (Math.abs(scalingFactor - 3.0) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(300));
        }
        else if (Math.abs(scalingFactor - 3.5) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(350));
        }
        else if (Math.abs(scalingFactor - 4.0) < 0.01)
        {
            assertThat(SWTHelper.scalePixel(100), is(400));
        }
    }

    @Test
    public void testMacOSRetinaScaling()
    {
        if (!Platform.OS_MACOSX.equals(Platform.getOS()))
            return;

        Point dpi = SWTHelper.getDPI();
        double scalingFactor = SWTHelper.getDPIScalingFactor();

        // macOS Non-Retina: 72 DPI (factor 1.0)
        if (Math.abs(scalingFactor - 1.0) < 0.01 && dpi.x == 72)
        {
            assertThat(SWTHelper.scalePixel(100), is(100));
        }
        // macOS Retina 2x: 144 DPI (factor 2.0)
        else if (Math.abs(scalingFactor - 2.0) < 0.01 && dpi.x == 144)
        {
            assertThat(SWTHelper.scalePixel(100), is(200));
        }
        // macOS Retina 3x: 218 DPI (factor ~3.03)
        else if (Math.abs(scalingFactor - 3.0) < 0.1 && dpi.x >= 216 && dpi.x <= 220)
        {
            int scaled = SWTHelper.scalePixel(100);
            assertThat(scaled, greaterThan(290));
            assertThat(scaled, greaterThan(0));
        }
    }
}
