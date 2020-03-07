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
import org.eclipse.swt.widgets.Label;

public final class SWTHelper
{
    public static final String EMPTY_LABEL = ""; //$NON-NLS-1$

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
    public static int stringWidth(Drawable drawable, String sample)
    {
        GC gc = new GC(drawable);
        Point extentText = gc.stringExtent(sample);
        gc.dispose();
        return extentText.x;
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
}
