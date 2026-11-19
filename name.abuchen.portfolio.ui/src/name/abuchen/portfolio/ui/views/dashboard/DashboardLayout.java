package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

import name.abuchen.portfolio.model.Dashboard.Column;

public class DashboardLayout extends Layout
{
    private static final int SPACING = 10;

    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
    {
        Control[] children = composite.getChildren();
        if (children.length == 0)
            return new Point(wHint, hHint);

        int total = getTotalWeight(children);

        int height = 0;

        for (Control child : children)
        {
            // compute the height with the width the column gets in #layout
            // because widgets such as the description widget wrap their text
            // and become higher in narrow columns

            int width = wHint;
            if (wHint != SWT.DEFAULT && total > 0)
            {
                int widthPerWeight = (wHint - (children.length * SPACING)) / total;
                width = Math.max(((Column) child.getData()).getWeight() * widthPerWeight, 0);
            }

            Point size = child.computeSize(width, hHint);
            height = Math.max(height, size.y);
        }

        return new Point(wHint, height);
    }

    @Override
    protected void layout(Composite composite, boolean flushCache)
    {
        Control[] children = composite.getChildren();
        if (children.length == 0)
            return;

        int total = getTotalWeight(children);
        if (total == 0)
            return;

        Rectangle availableBounds = composite.getBounds();

        int widthPerWeight = (availableBounds.width - (children.length * SPACING)) / total;

        int x = 0;
        for (int ii = 0; ii < children.length; ii++)
        {
            Control child = children[ii];
            int weight = ((Column) child.getData()).getWeight();

            child.setBounds(x, 0, weight * widthPerWeight, availableBounds.height);

            x += (weight * widthPerWeight) + SPACING;
        }
    }

    private static int getTotalWeight(Control[] children)
    {
        int total = 0;
        for (Control child : children)
            total += ((Column) child.getData()).getWeight();
        return total;
    }
}
