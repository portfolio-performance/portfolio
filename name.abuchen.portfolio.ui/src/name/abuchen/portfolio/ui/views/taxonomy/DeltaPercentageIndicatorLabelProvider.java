package name.abuchen.portfolio.ui.views.taxonomy;

import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.function.Function;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;

public class DeltaPercentageIndicatorLabelProvider extends OwnerDrawLabelProvider
{
    private static class Data
    {
        long totalAmount;
        long targetAmount;
        long actualAmount;

        double absoluteDeviation;
        double relativeDeviation;

        private static Data from(TaxonomyNode node)
        {
            if (node.getTarget() == null || node.getTarget().isZero())
                return null;

            Data data = new Data();

            data.totalAmount = node.getClassificationRoot().getActual().getAmount();
            data.targetAmount = node.getTarget().getAmount();
            data.actualAmount = node.getActual().getAmount();

            // calculate deviations
            data.absoluteDeviation = (data.actualAmount - data.targetAmount) / (double) data.totalAmount;
            data.relativeDeviation = (data.actualAmount / (double) data.targetAmount) - 1;

            return data;
        }
    }

    private Function<Object, TaxonomyNode> nodeProvider;

    private RebalancingColoringRule coloring;

    public DeltaPercentageIndicatorLabelProvider(Control control, Client client,
                    Function<Object, TaxonomyNode> nodeProvider) // NOSONAR
    {
        this.nodeProvider = nodeProvider;

        this.coloring = new RebalancingColoringRule(client);

        PropertyChangeListener listener = evt -> this.coloring = new RebalancingColoringRule(client);
        client.addPropertyChangeListener(listener);
        control.addDisposeListener(e -> client.removePropertyChangeListener(listener));
    }

    @Override
    protected void measure(Event event, Object element)
    {
        event.setBounds(new Rectangle(0, 0, 80, 10));
    }

    @Override
    protected void paint(Event event, Object element)
    {
        Data data = Data.from(nodeProvider.apply(element));
        if (data == null)
            return;

        boolean isColored = (Math.abs(data.relativeDeviation) > coloring.getRelativeThreshold() / 100d)
                        || (Math.abs(data.absoluteDeviation) > coloring.getAbsoluteThreshold() / 100d);

        Color oldForeground = event.gc.getForeground();

        Rectangle bounds = getBounds(event.item, event.index);

        int center = bounds.width / 2;

        event.gc.setBackground(Colors.SIDEBAR_BACKGROUND_SELECTED);
        event.gc.fillRectangle(bounds.x + center - 1, bounds.y, 3, bounds.height);

        double absolute = Math.abs(data.relativeDeviation);

        event.gc.setBackground(isColored ? Colors.ICON_ORANGE : Colors.ICON_BLUE);

        int bar = Math.min(center, (int) Math.round(absolute * (center / (coloring.getBarLength() / 100d))));
        if (data.relativeDeviation < 0d)
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
        if (widget instanceof TableItem tableItem)
            return tableItem.getBounds(index);
        else if (widget instanceof TreeItem treeItem)
            return treeItem.getBounds(index);
        else
            throw new IllegalArgumentException("unsupported widget type " + widget); //$NON-NLS-1$
    }

    @Override
    public String getToolTipText(Object element)
    {
        Data data = Data.from(nodeProvider.apply(element));
        if (data == null)
            return null;

        return MessageFormat.format(Messages.TooltipRebalancingIndicator, //
                        coloring.getAbsoluteThreshold(), //
                        coloring.getRelativeThreshold(), //
                        Values.Amount.format(data.actualAmount - data.targetAmount), //
                        Values.Percent2.format(data.absoluteDeviation), //
                        Values.Amount.format(data.totalAmount), //
                        Values.Percent2.format(data.relativeDeviation), //
                        Values.Amount.format(data.targetAmount));
    }
}
