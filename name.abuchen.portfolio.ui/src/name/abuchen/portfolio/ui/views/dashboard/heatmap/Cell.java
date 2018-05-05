package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.ui.util.Colors;

class Cell extends Canvas // NOSONAR
{
    private static final int MARGIN = 2;

    private CellDataProvider dataProvider;

    public Cell(Composite parent, CellDataProvider dataProvider)
    {
        super(parent, SWT.NO_BACKGROUND | SWT.NO_FOCUS);
        this.dataProvider = dataProvider;

        addPaintListener(this::paintControl);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        GC gc = new GC(this);
        Point extent = gc.stringExtent(dataProvider.getText());
        gc.dispose();

        return new Point(10, extent.y + 2 * MARGIN);
    }

    private void paintControl(PaintEvent e)
    {
        GC gc = e.gc;

        Color oldBackground = gc.getBackground();
        Color oldForeground = gc.getForeground();
        Font oldFont = gc.getFont();

        Rectangle bounds = getClientArea();

        gc.setBackground(dataProvider.getBackground());
        gc.setForeground(Colors.BLACK);
        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

        Font newFont = dataProvider.getFont();
        if (newFont != null)
            gc.setFont(newFont);

        String text = dataProvider.getText();
        Point extend = gc.stringExtent(text);

        gc.drawText(text, bounds.x + (bounds.width - extend.x) / 2, bounds.y + (bounds.height - extend.y) / 2);

        gc.setBackground(oldBackground);
        gc.setForeground(oldForeground);
        gc.setFont(oldFont);
    }

}