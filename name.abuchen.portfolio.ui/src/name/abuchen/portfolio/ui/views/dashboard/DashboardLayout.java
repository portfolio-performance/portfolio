package name.abuchen.portfolio.ui.views.dashboard;

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

        int height = 0;

        for (Control child : children)
        {
            // at the moment, the width of the column does not matter because
            // none of the widgets actually wrap so we can safely ignore the
            // computed width

            Point size = child.computeSize(wHint, hHint);
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

        int total = 0;
        for (Control child : children)
            total += ((Column) child.getData()).getWeight();
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
}
