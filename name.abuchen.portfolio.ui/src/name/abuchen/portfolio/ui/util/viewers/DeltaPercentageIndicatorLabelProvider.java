package name.abuchen.portfolio.ui.util.viewers;

import java.util.function.Function;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.ui.util.Colors;

public class DeltaPercentageIndicatorLabelProvider extends OwnerDrawLabelProvider
{
    private Function<Object, Double> percentageProvider;

    public DeltaPercentageIndicatorLabelProvider(Function<Object, Double> percentageProvider) // NOSONAR
    {
        this.percentageProvider = percentageProvider;
    }

    @Override
    protected void measure(Event event, Object element)
    {
        event.setBounds(new Rectangle(0, 0, 80, 10));
    }

    @Override
    protected void paint(Event event, Object element)
    {
        Double percentage = percentageProvider.apply(element);

        if (percentage == null)
            return;

        Color oldForeground = event.gc.getForeground();

        Rectangle bounds = getBounds(event.item, event.index);

        int center = bounds.width / 2;

        event.gc.setBackground(Colors.SIDEBAR_BACKGROUND_SELECTED);
        event.gc.fillRectangle(bounds.x + center - 1, bounds.y, 3, bounds.height);

        double absolute = Math.abs(percentage);

        event.gc.setBackground(absolute > 0.05d ? Colors.ICON_ORANGE : Colors.ICON_BLUE);

        int bar = Math.min(center, (int) Math.round(absolute * (center / 0.1)));
        if (percentage < 0d)
            event.gc.fillRectangle(bounds.x + center - bar, bounds.y + (bounds.height / 2) - 2, bar, 5);
        else
            event.gc.fillRectangle(bounds.x + center, bounds.y + (bounds.height / 2) - 2, bar, 5);

        event.gc.setForeground(oldForeground);
    }

    @Override
    protected void erase(Event event, Object element)
    {
        // use os-specific background
    }

    private Rectangle getBounds(Widget widget, int index)
    {
        if (widget instanceof TableItem)
            return ((TableItem) widget).getBounds(index);
        else if (widget instanceof TreeItem)
            return ((TreeItem) widget).getBounds(index);
        else
            throw new IllegalArgumentException();
    }
}
