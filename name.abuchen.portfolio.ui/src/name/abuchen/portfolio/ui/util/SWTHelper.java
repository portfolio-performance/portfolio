package name.abuchen.portfolio.ui.util;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public final class SWTHelper
{
    public static final String EMPTY_LABEL = ""; //$NON-NLS-1$

    /** standard DPI value (Windows default at 100% scaling) */
    private static final int STANDARD_DPI_WINDOWS = 96;

    /** standard DPI value (macOS default for non-Retina displays) */
    private static final int STANDARD_DPI_MACOS = 72;

    /** average char width needed to resize input fields on length */
    private static double averageCharWidth = -1;

    /** cached DPI scaling factor */
    private static double dpiScalingFactor = -1;

    private SWTHelper()
    {
    }

    /**
     * Returns the widest control. Used when layouting dialogs.
     */
    public static Control widestWidget(Control... widgets)
    {
        int width = 0;
        Control answer = null;

        for (int ii = 0; ii < widgets.length; ii++)
        {
            if (widgets[ii] == null)
                continue;

            int w = widgets[ii].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            if (w >= width)
            {
                answer = widgets[ii];
                width = w;
            }
        }

        return answer;
    }

    /**
     * Returns the widest control. Used when layouting dialogs.
     */
    public static int widest(Control... widgets)
    {
        int width = 0;

        for (int ii = 0; ii < widgets.length; ii++)
        {
            if (widgets[ii] == null)
                continue;

            int w = widgets[ii].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            if (w >= width)
                width = w;
        }

        return width;
    }

    /**
     * Returns the width needed to display a date. Used when layouting dialogs.
     */
    public static int dateWidth(Drawable drawable)
    {
        GC gc = new GC(drawable);
        Point extentText = gc.stringExtent("YYYY-MM-DD"); //$NON-NLS-1$
        gc.dispose();
        return extentText.x;
    }

    /**
     * Returns the width needed to display the sample string. Used when
     * layouting dialogs.
     */
    public static int stringWidth(Drawable drawable, String text)
    {
        GC gc = new GC(drawable);
        Point extentText = gc.stringExtent(text);
        gc.dispose();
        return extentText.x;
    }
    
    public static double getAverageCharWidth(Drawable drawable)
    {
        if (averageCharWidth > 0)
            return averageCharWidth;

        GC gc = new GC(drawable);
        FontMetrics fm = gc.getFontMetrics();
        averageCharWidth = fm.getAverageCharacterWidth();
        gc.dispose();

        return averageCharWidth;
    }


    /**
     * Returns the number of pixels needed to render one character.
     */
    public static int lineHeight(Control drawable)
    {
        GC gc = new GC(drawable);
        gc.setFont(drawable.getFont());
        FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();
        return fontMetrics.getHeight();
    }

    /**
     * Returns the width needed to display a currency.
     */
    public static int amountWidth(Drawable drawable)
    {
        return stringWidth(drawable, "12345678,00"); //$NON-NLS-1$
    }

    /**
     * Returns the width needed to display a currency.
     */
    public static int currencyWidth(Drawable drawable)
    {
        return stringWidth(drawable, "XXXX"); //$NON-NLS-1$
    }

    /**
     * Uses FormData objects to place the label and input field below the given
     * reference item (which is typically another input field). The label is
     * placed to the left of the value.
     */
    public static void placeBelow(Control referenceItem, Label label, Control value)
    {
        FormData data = new FormData();

        if (label != null)
        {
            data.top = new FormAttachment(value, 0, SWT.CENTER);
            label.setLayoutData(data);
        }

        data = new FormData();
        data.top = new FormAttachment(referenceItem, 5);
        data.left = new FormAttachment(referenceItem, 0, SWT.LEFT);
        data.right = new FormAttachment(referenceItem, 0, SWT.RIGHT);
        value.setLayoutData(data);
    }

    /**
     * Uses FormData objects to place the label and input field below the given
     * reference item (which is typically another input field).
     */
    public static void placeBelow(Control referenceItem, Control value)
    {
        placeBelow(referenceItem, null, value);
    }

    /**
     * Sets the label of the given elements to an empty string.
     */
    public static void clearLabel(Label... labels)
    {
        for (Label label : labels)
            label.setText(EMPTY_LABEL);
    }

    public static int getPackedWidth(Control item)
    {
        item.pack();
        return item.getBounds().width;
    }

    public static ComboViewer createComboViewer(Composite parent)
    {
        if (Platform.OS_WIN32.equals(Platform.getOS()))
            return new ComboViewer(new CCombo(parent, SWT.READ_ONLY | SWT.FLAT | SWT.BORDER));
        else
            return new ComboViewer(parent, SWT.READ_ONLY);
    }

    /**
     * Returns the DPI scaling factor for the current display. For example:
     *
     * Windows:
     * - 100% scaling (96 DPI) returns 1.0
     * - 125% scaling (120 DPI) returns 1.25
     * - 150% scaling (144 DPI) returns 1.5
     * - 200% scaling (192 DPI) returns 2.0
     * - 400% scaling (384 DPI) returns 4.0
     *
     * macOS:
     * - Non-Retina (72 DPI) returns 1.0
     * - Retina 2x (144 DPI) returns 2.0
     * - Retina 3x (218 DPI) returns 3.0
     *
     * If DPI cannot be determined, returns 1.0 (no scaling).
     *
     * @return DPI scaling factor relative to platform standard DPI, or 1.0 as fallback
     */
    public static double getDPIScalingFactor()
    {
        if (dpiScalingFactor > 0)
            return dpiScalingFactor;

        try
        {
            Display display = Display.getCurrent();
            if (display == null)
                display = Display.getDefault();

            if (display == null || display.isDisposed())
            {
                // Fallback: no scaling
                dpiScalingFactor = 1.0;
                return dpiScalingFactor;
            }

            Point dpi = display.getDPI();

            // Validate DPI values
            if (dpi == null || dpi.x <= 0 || dpi.y <= 0)
            {
                // Fallback: no scaling
                dpiScalingFactor = 1.0;
                return dpiScalingFactor;
            }

            // Use horizontal DPI (x) as reference
            // Determine base DPI based on platform
            int baseDPI;
            if (Platform.OS_MACOSX.equals(Platform.getOS()))
            {
                // macOS uses 72 DPI as standard for non-Retina displays
                baseDPI = STANDARD_DPI_MACOS;
            }
            else
            {
                // Windows and Linux use 96 DPI as standard
                baseDPI = STANDARD_DPI_WINDOWS;
            }

            dpiScalingFactor = dpi.x / (double) baseDPI;

            // Sanity check: scaling factor should be between 0.5 and 5.0
            if (dpiScalingFactor < 0.5 || dpiScalingFactor > 5.0)
            {
                // Fallback: no scaling
                dpiScalingFactor = 1.0;
            }
        }
        catch (Exception e)
        {
            // Fallback: no scaling if any error occurs
            dpiScalingFactor = 1.0;
        }

        return dpiScalingFactor;
    }

    /**
     * Returns the current DPI values for the display.
     *
     * @return Point with x = horizontal DPI, y = vertical DPI, or platform default if unavailable
     */
    public static Point getDPI()
    {
        try
        {
            Display display = Display.getCurrent();
            if (display == null)
                display = Display.getDefault();

            if (display != null && !display.isDisposed())
            {
                Point dpi = display.getDPI();
                if (dpi != null && dpi.x > 0 && dpi.y > 0)
                {
                    return dpi;
                }
            }
        }
        catch (Exception e)
        {
            // Fall through to default
        }

        // Fallback: return platform default DPI
        if (Platform.OS_MACOSX.equals(Platform.getOS()))
        {
            return new Point(STANDARD_DPI_MACOS, STANDARD_DPI_MACOS);
        }
        else
        {
            return new Point(STANDARD_DPI_WINDOWS, STANDARD_DPI_WINDOWS);
        }
    }

    /**
     * Scales a pixel value according to the current DPI scaling factor.
     * For example, at 150% scaling, scalePixel(100) returns 150.
     *
     * @param pixels the pixel value at 100% scaling
     * @return scaled pixel value
     */
    public static int scalePixel(int pixels)
    {
        return (int) Math.round(pixels * getDPIScalingFactor());
    }

    /**
     * Scales a pixel value according to the current DPI scaling factor.
     *
     * @param pixels the pixel value at 100% scaling
     * @return scaled pixel value
     */
    public static double scalePixel(double pixels)
    {
        return pixels * getDPIScalingFactor();
    }

    /**
     * Returns a debug string with current DPI information.
     * Useful for logging and troubleshooting DPI scaling issues.
     *
     * @return debug string with DPI values and scaling factor
     */
    public static String getDPIDebugInfo()
    {
        Point dpi = getDPI();
        double scalingFactor = getDPIScalingFactor();
        int percentage = (int) Math.round(scalingFactor * 100);

        return String.format("DPI: %d x %d, Scaling: %.2f (%d%%)", //$NON-NLS-1$
                           dpi.x, dpi.y, scalingFactor, percentage);
    }
}
