package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

import name.abuchen.portfolio.ui.util.swt.ColoredLabel;

public class ScatterChartToolTip extends AbstractChartToolTip
{

    public ScatterChartToolTip(Chart chart)
    {
        super(chart);
    }

    @Override
    protected Object getFocusObjectAt(Event event)
    {
        // find closest scatter series
        ISeries[] series = getChart().getSeriesSet().getSeries();

        ISeries closest = null;
        double minDist = Double.MAX_VALUE;
        for (ISeries serie : series)
        {
            Point point = serie.getPixelCoordinates(0);

            // compute distance to mouse position
            double newDist = Math.sqrt(Math.pow((event.x - point.x), 2) + Math.pow((event.y - point.y), 2));

            if (newDist < minDist)
            {
                minDist = newDist;
                closest = serie;
            }
        }

        return closest;
    }

    @Override
    protected void createComposite(Composite parent)
    {
        final Composite container = new Composite(parent, SWT.NONE);
        container.setBackgroundMode(SWT.INHERIT_FORCE);
        GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);

        IAxis xAxis = getChart().getAxisSet().getXAxis(0);
        IAxis yAxis = getChart().getAxisSet().getYAxis(0);
        ILineSeries closest = (ILineSeries) getFocusedObject();

        // header

        Label left = new Label(container, SWT.NONE);
        left.setText(""); //$NON-NLS-1$

        Label middle = new Label(container, SWT.NONE);
        middle.setText(xAxis.getTitle().getText());

        Label right = new Label(container, SWT.NONE);
        right.setText(yAxis.getTitle().getText());

        // values

        ColoredLabel cl = new ColoredLabel(container, SWT.NONE);
        cl.setBackdropColor(closest.getSymbolColor());
        cl.setText(closest.getId());

        middle = new Label(container, SWT.RIGHT);
        middle.setText(xAxis.getTick().getFormat().format(closest.getXSeries()[0]));
        GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(middle);

        right = new Label(container, SWT.RIGHT);
        right.setText(yAxis.getTick().getFormat().format(closest.getYSeries()[0]));
        GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);
    }
}
