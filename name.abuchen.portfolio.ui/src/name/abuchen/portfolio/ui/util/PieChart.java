package name.abuchen.portfolio.ui.util;

import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class PieChart extends Composite implements Listener
{
    public static class Slice
    {
        public static class ByValue implements Comparator<Slice>
        {
            @Override
            public int compare(Slice o1, Slice o2)
            {
                return o1.value > o2.value ? -1 : o1.value == o2.value ? 0 : 1;
            }
        }

        private long value;
        private String label;
        private Color color;

        public Slice(long value, String label, Color color)
        {
            this.value = value;
            this.label = label;
            this.color = color;
        }

        public Slice(long value, String label)
        {
            this(value, label, null);
        }

        public long getValue()
        {
            return value;
        }

        public String getLabel()
        {
            return label;
        }

        public Color getColor()
        {
            return color;
        }

        public void setColor(Color color)
        {
            this.color = color;
        }
    }

    private static final int PADDING = 40;

    private Image image;
    private boolean updateImage;

    private List<Slice> slices;

    public PieChart(Composite parent, int style)
    {
        super(parent, style);
        setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        addListener(SWT.Paint, this);
        addListener(SWT.Resize, this);
    }

    public void setSlices(List<Slice> slices)
    {
        this.slices = slices;
        this.updateImage = true;
    }

    public void handleEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.Paint:
                paintControl(event);
                break;
            case SWT.Resize:
                redraw();
                break;
            default:
                break;
        }
    }

    @Override
    public void update()
    {
        super.update();
        updateImage = true;
    }

    @Override
    public void redraw()
    {
        super.redraw();
        updateImage = true;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (image != null && !image.isDisposed())
            image.dispose();
    }

    private void paintControl(Event e)
    {
        if (updateImage)
        {
            Point size = getSize();
            if (image != null && !image.isDisposed())
                image.dispose();
            image = new Image(Display.getCurrent(), size.x, size.y);
            GC gc = new GC(image);
            gc.setAntialias(SWT.ON);

            int total = 0;
            for (Slice slice : slices)
                total += slice.getValue();

            final int centerX = size.x / 2;
            final int centerY = size.y / 2;
            final int diameter = Math.min(size.x, size.y) - 2 * PADDING;
            final int radius = diameter / 2;

            // background
            gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            gc.fillRectangle(0, 0, size.x, size.y);

            // slices
            int startAngle = 0;
            for (Slice slice : slices)
            {
                int arcAngle = (int) ((double) slice.getValue() * 360 / total + 0.5d);
                if (slice == slices.get(slices.size() - 1))
                    arcAngle = 360 - startAngle;

                gc.setBackground(slice.getColor());
                gc.fillArc(centerX - radius, centerY - radius, diameter, diameter, startAngle, arcAngle);

                startAngle += arcAngle;
            }

            // circle
            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            gc.drawOval(centerX - radius, centerY - radius, diameter, diameter);

            // edges
            startAngle = 0;
            for (Slice slice : slices)
            {
                int arcAngle = (int) ((double) slice.getValue() * 360 / total + 0.5d);
                if (slice == slices.get(slices.size() - 1))
                    arcAngle = 360 - startAngle;

                double angleRad = startAngle * Math.PI / 180.0d;
                int x = centerX + (int) (radius * Math.cos(-angleRad));
                int y = centerY + (int) (radius * Math.sin(-angleRad));
                gc.drawLine(centerX, centerY, x, y);

                startAngle += arcAngle;
            }

            // labels
            startAngle = 0;
            for (Slice slice : slices)
            {
                int arcAngle = (int) ((double) slice.getValue() * 360 / total + 0.5d);
                if (slice == slices.get(slices.size() - 1))
                    arcAngle = 360 - startAngle;

                if (arcAngle > 1)
                {
                    // percentage
                    double angleRad = (startAngle + arcAngle / 2) * Math.PI / 180.0d;
                    int x = (size.x / 2) + (int) (radius / 1.2 * Math.cos(-angleRad));
                    int y = (size.y / 2) + (int) (radius / 1.2 * Math.sin(-angleRad));

                    String label = String.format("%,.2f%%", (double) slice.getValue() / total * 100); //$NON-NLS-1$
                    Point extend = gc.stringExtent(label);
                    gc.setForeground(Colors.getTextColor(slice.getColor()));
                    gc.drawString(label, x - extend.x / 2, y - extend.y / 2, true);

                    // label
                    x = (size.x / 2) + (int) ((radius + 10) * Math.cos(-angleRad));
                    y = (size.y / 2) + (int) ((radius + 10) * Math.sin(-angleRad));

                    extend = gc.stringExtent(slice.getLabel());
                    if (x < centerX)
                        x -= extend.x;

                    gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                    gc.drawString(slice.getLabel(), x, y - extend.y / 2, true);
                }

                startAngle += arcAngle;
            }

            updateImage = false;
        }
        e.gc.drawImage(image, 0, 0);
    }

}
