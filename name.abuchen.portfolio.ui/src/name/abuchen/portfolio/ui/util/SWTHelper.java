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
            int w = widgets[ii].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            if (w >= width)
                answer = widgets[ii];
        }

        return answer;
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
     * Uses FormData objects to place the label and input field below the given
     * reference item (which is typically another input field).
     */
    public static void placeBelow(Control referenceItem, Label label, Control value)
    {
        FormData data = new FormData();
        data.top = new FormAttachment(value, 0, SWT.CENTER);
        label.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(referenceItem, 5);
        data.left = new FormAttachment(referenceItem, 0, SWT.LEFT);
        data.right = new FormAttachment(referenceItem, 0, SWT.RIGHT);
        value.setLayoutData(data);
    }

}
