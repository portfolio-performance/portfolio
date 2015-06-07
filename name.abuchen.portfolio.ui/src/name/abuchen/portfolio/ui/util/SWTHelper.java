package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Drawable;
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
    {}

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

    /**
     * Sets the weights of the sash in such a way that the item (for example a
     * details viewer) initially takes its actual size. In order to do that, we
     * need to determine the size of the parent. The size of the parent might be
     * zero if it never has been rendered before.
     * 
     * @param sash
     *            the sash on which to set the weights
     * @param parent
     *            the parent composite that determines the full width available
     * @param item
     *            the item that shall be placed on the right
     */
    public static void setSashWeights(SashForm sash, Composite parent, Control item)
    {
        item.pack();
        int childWidth = item.getBounds().width;

        int parentWidth = parent.getBounds().width;
        if (parentWidth == 0)
        {
            // #pack is required if parent has never been rendered before
            parent.pack();
            parentWidth = parent.getBounds().width;
        }

        sash.setWeights(new int[] { parentWidth - childWidth, childWidth });
    }

}
