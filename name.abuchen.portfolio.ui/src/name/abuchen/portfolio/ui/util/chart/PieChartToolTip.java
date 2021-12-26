package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.model.Node;


public class PieChartToolTip extends AbstractSWTChartToolTip
{
    private static final int ID_PRIMARY_X_AXIS = 0;
    private static final int ID_PRIMARY_Y_AXIS = 0;
    private static final String X_AXIS = "X_AXIS"; //$NON-NLS-1$
    private static final String Y_AXIS = "Y_AXIS"; //$NON-NLS-1$
    private DecimalFormat defaultValueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    public PieChartToolTip(Chart chart)
    {
        super(chart);
    }

    @Override
    protected Object getFocusObjectAt(Event event)
    {
        Node node = null;
        for(ISeries<?> series : getChart().getSeriesSet().getSeries()) {
            if(!(series instanceof ICircularSeries)) {
                continue;
            }
            ICircularSeries<?> circularSeries = (ICircularSeries<?>) series;

            double primaryValueX = getSelectedPrimaryAxisValue(event.x, X_AXIS);
            double primaryValueY = getSelectedPrimaryAxisValue(event.y, Y_AXIS);

            node = circularSeries.getPieSliceFromPosition(primaryValueX, primaryValueY);
            circularSeries.setHighlightedNode(node);

        }
        return node;
    }

    @Override
    protected void createComposite(Composite parent, Object focus)
    {
        if (focus == null) {
            return;
        }
        Node currentNode = (Node) focus;
        final Composite container = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.center = true;
        container.setLayout(layout);
        Composite data = new Composite(container, SWT.NONE);
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);
        Label left = new Label(data, SWT.NONE);
        left.setText(currentNode.getId());
        Label right = new Label(data, SWT.NONE);

        right.setText(defaultValueFormat.format(currentNode.getValue()));
    }

    @Override
    protected boolean isHoverMode()
    {
        return true;
    }

    private double getSelectedPrimaryAxisValue(int position, String orientation)
    {
        double primaryValue = 0.0d;
        double start;
        double stop;
        int length;

        if(orientation.equals(X_AXIS)) {
            IAxis axis = getChart().getAxisSet().getXAxis(ID_PRIMARY_X_AXIS);
            start = axis.getRange().lower;
            stop = axis.getRange().upper;
            length = getChart().getPlotArea().getSize().x;
        } else {
            IAxis axis = getChart().getAxisSet().getYAxis(ID_PRIMARY_Y_AXIS);
            start = axis.getRange().lower;
            stop = axis.getRange().upper;
            length = getChart().getPlotArea().getSize().y;
        }
        
        if(position <= 0) {
            primaryValue = start;
        } else if(position > length) {
            primaryValue = stop;
        } else {
            double delta = stop - start;
            double percentage;
            if(orientation.equals(X_AXIS)) {
                percentage = ((100.0d / length) * position) / 100.0d;
            } else {
                percentage = (100.0d - ((100.0d / length) * position)) / 100.0d;
            }
            primaryValue = start + delta * percentage;
        }
        return primaryValue;
    }

    @Override
    void onFocusChanged(Object newFocus)
    {
        // Update highlighted node
        getChart().redraw();
    }
}
