package name.abuchen.portfolio.ui.views.taxonomy;

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
    private Function<Object, TaxonomyNode> nodeProvider;

    public DeltaPercentageIndicatorLabelProvider(Function<Object, TaxonomyNode> nodeProvider) // NOSONAR
    {
        this.nodeProvider = nodeProvider;
    }

    @Override
    protected void measure(Event event, Object element)
    {
        event.setBounds(new Rectangle(0, 0, 80, 10));
    }

    @Override
    protected void paint(Event event, Object element)
    {
        TaxonomyNode node = nodeProvider.apply(element);
        if (node.getTarget() == null)
            return;
        
        double totalAmount  = (double)node.getRoot().getActual().getAmount() ;
        double targetAmount = (double)node.getTarget().getAmount() ;
        double actualAmount = (double)node.getActual().getAmount() ;
        
        // the Delta % value (previously: percentage)
        double relativeDeviation = (actualAmount / targetAmount) - 1.;
        // newly introduced
        double relativeTargetAmount = targetAmount / totalAmount;
        
        String propertyString = "25.";//Client.getProperty("rebalance_relative_threshold") ;
        double thRel = Double.parseDouble(propertyString);
        boolean thRelEnabled = Boolean.parseBoolean("true");
        
        propertyString = "5.";//Client.getProperty("rebalance_absolute_threshold") ;
        double thAbs = Double.parseDouble(propertyString) / relativeTargetAmount;
        boolean thAbsEnabled = Boolean.parseBoolean("true");
        
        double threshold = 5;  // default case: threshold set to 5%
        // Make the bar 5% longer than relative threshold (which is the maximum threshold that can occur)
        // However, special case if no relative threshold is given!
        double barMaxlength = threshold + 5.;
        if (thRelEnabled && thAbsEnabled)
        {
            threshold = Math.min(thRel,thAbs);
            barMaxlength = thRel + 5.;
        }
        else if (thRelEnabled)
        {
            threshold = thRel;
            barMaxlength = thRel + 5.;
        }
        else if (thAbsEnabled)
            threshold = thAbs; // use default bar length of 10% ?
        threshold /= 100.;
        barMaxlength /= 100.;
        

        Color oldForeground = event.gc.getForeground();

        Rectangle bounds = getBounds(event.item, event.index);

        int center = bounds.width / 2;

        event.gc.setBackground(Colors.SIDEBAR_BACKGROUND_SELECTED);
        event.gc.fillRectangle(bounds.x + center - 1, bounds.y, 3, bounds.height);

        double absolute = Math.abs(relativeDeviation);

        event.gc.setBackground(absolute > threshold ? Colors.ICON_ORANGE : Colors.ICON_BLUE);

        int bar = Math.min(center, (int) Math.round(absolute * (center / barMaxlength)));
        if (relativeDeviation < 0d)
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
