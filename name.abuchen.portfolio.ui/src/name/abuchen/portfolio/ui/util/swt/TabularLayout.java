package name.abuchen.portfolio.ui.util.swt;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Monitor;

import name.abuchen.portfolio.ui.Images;

public class TabularLayout extends Layout
{
    private static final int MARGIN = 5;
    private static final int SPACING = 5;

    private int numColumns;
    private int numHeaderRows;
    private int numFooterRows;

    private Label divider;

    public TabularLayout(int numColumns, int numHeaderRows, int numFooterRows)
    {
        this.numColumns = numColumns;
        this.numHeaderRows = numHeaderRows;
        this.numFooterRows = numFooterRows;
    }

    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
    {
        Point size = layout(composite, false, 0, 0, wHint, hHint, flushCache);
        if (wHint != SWT.DEFAULT)
            size.x = wHint;
        if (hHint != SWT.DEFAULT)
            size.y = hHint;
        return size;
    }

    @Override
    protected void layout(Composite composite, boolean flushCache)
    {
        Rectangle rect = composite.getClientArea();
        layout(composite, true, rect.x, rect.y, rect.width, rect.height, flushCache);
    }

    Point layout(Composite composite, boolean move, int x, int y, int width, int height, boolean flushCache)
    {
        Control[] children = getChildren(composite);
        if (children.length % numColumns != 0)
            throw new UnsupportedOperationException();

        int cols = numColumns;
        int rows = children.length / cols;

        int[] widths = new int[cols];
        int[] heights = new int[rows];

        for (int ii = 0; ii < children.length; ii++)
        {
            Control control = children[ii];
            Point size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);

            widths[ii % cols] = Math.max(widths[ii % cols], size.x);
            heights[ii / cols] = Math.max(heights[ii / cols], size.y);
        }

        int fullHeight = 2 * MARGIN + (rows - 1) * SPACING + Arrays.stream(heights).sum();

        // check if we need to hide rows

        Monitor primaryMonitor = composite.getDisplay().getPrimaryMonitor();
        if (primaryMonitor != null)
        {
            Rectangle rect = primaryMonitor.getClientArea();
            if (rect.height < fullHeight)
            {
                int overflow = fullHeight - rect.height;

                int ii = rows - numFooterRows - 1;
                while (ii > numHeaderRows && overflow > 0)
                {
                    overflow -= heights[ii] + SPACING;
                    heights[ii] = ii == rows - numFooterRows - 1 ? 0 : -SPACING;
                    ii--;
                }
            }
        }

        if (move)
        {
            boolean hasAtLeastOneRowHidden = false;

            for (int ii = 0; ii < children.length; ii++)
            {
                Control control = children[ii];

                int col = ii % cols;
                int row = ii / cols;

                boolean hide = heights[row] <= 0;

                if (hide)
                {
                    control.setVisible(false);
                    hasAtLeastOneRowHidden = true;
                }
                else
                {
                    int xx = MARGIN;
                    for (int jj = 0; jj < col; jj++)
                        xx += widths[jj] + SPACING;

                    int yy = MARGIN;
                    for (int jj = 0; jj < row; jj++)
                        yy += heights[jj] + SPACING;

                    control.setBounds(x + xx, y + yy, widths[col], heights[row]);
                    control.setVisible(true);
                }
            }

            if (hasAtLeastOneRowHidden)
            {
                if (divider == null)
                {
                    divider = new Label(composite, SWT.NONE);
                    divider.setImage(Images.DOT_HORIZONAL.image());
                }

                int yy = MARGIN;
                for (int jj = 0; jj < rows - numHeaderRows; jj++)
                    yy += heights[jj] + SPACING;

                int h = divider.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

                divider.setBounds(x + MARGIN, yy - SPACING - (h / 2), width - 2 * MARGIN, h);
                divider.setVisible(true);
            }
            else
            {
                if (divider != null)
                    divider.setVisible(false);
            }
        }

        // calculate height again as it might have been changed

        return new Point(2 * MARGIN + (cols - 1) * SPACING + Arrays.stream(widths).sum(),
                        2 * MARGIN + (rows - 1) * SPACING + Arrays.stream(heights).sum());
    }

    private Control[] getChildren(Composite composite)
    {
        Control[] children = composite.getChildren();

        if (divider != null)
        {
            for (int ii = 0; ii < children.length; ii++)
            {
                if (children[ii] == divider)
                {
                    Control[] answer = new Control[children.length - 1];
                    System.arraycopy(children, 0, answer, 0, ii);
                    System.arraycopy(children, ii + 1, answer, ii, children.length - ii - 1);
                    return answer;
                }
            }
        }

        return children;
    }

}
