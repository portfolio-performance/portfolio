package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * Simplified grid layout that avoids the relatively expensive operation to
 * compute the size of each cells twice (once to compute the preferred size
 * which is ignored anyway and once to center the text inside the cell).
 */
class SimpleGridLayout extends Layout
{
    private int numColumns = 1;

    private int numRows = 1;

    private int rowHeight = 20;

    public void setNumColumns(int numColumns)
    {
        this.numColumns = numColumns;
    }

    public void setNumRows(int numRows)
    {
        this.numRows = numRows;
    }

    public void setRowHeight(int rowHeight)
    {
        this.rowHeight = rowHeight;
    }

    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
    {
        int width = numColumns * rowHeight;
        int height = numRows * rowHeight;

        if (wHint != SWT.DEFAULT)
            width = wHint;
        if (hHint != SWT.DEFAULT)
            height = hHint;

        return new Point(width, height);
    }

    @Override
    protected void layout(Composite composite, boolean flushCache)
    {
        Rectangle rect = composite.getClientArea();

        Control[] children = composite.getChildren();
        int count = children.length;
        if (count == 0)
            return;

        int width = rect.width / numColumns;
        int height = rect.height / numRows;

        int column = 0;
        int row = 0;

        for (int index = 0; index < children.length; index++)
        {
            if (column >= numColumns)
            {
                column = 0;
                row += 1;
            }

            children[index].setBounds(rect.x + (column * width), rect.y + (row * height), width - 1, height - 1);

            column++;
        }
    }
}
