package name.abuchen.portfolio.ui.views.payments;

import java.text.DateFormatSymbols;
import java.util.Arrays;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.ISeries;
import org.swtchart.Range;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.chart.PlainChart;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;

public abstract class AbstractChartTab implements PaymentsTab
{

    @Inject
    protected PaymentsViewModel model;

    private LocalResourceManager resources;
    private Chart chart;

    @Inject
    @Optional
    public void onDiscreedModeChanged(@UIEventTopic(UIConstants.Event.Global.DISCREET_MODE) Object obj)
    {
        if (chart != null)
            chart.redraw();
    }

    protected abstract void createSeries();

    protected Chart getChart()
    {
        return chart;
    }

    protected LocalResourceManager getResources()
    {
        return resources;
    }

    @Override
    public Control createControl(Composite parent)
    {
        resources = new LocalResourceManager(JFaceResources.getResources(), parent);

        chart = new PlainChart(parent, SWT.NONE);

        chart.setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.BOTTOM);

        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setVisible(false);
        xAxis.getTitle().setText(Messages.ColumnMonth);

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.setPosition(Position.Secondary);

        xAxis.enableCategory(true);

        // format symbols returns 13 values as some calendars have 13 months
        xAxis.setCategorySeries(Arrays.copyOfRange(new DateFormatSymbols().getMonths(), 0, 12));

        createSeries();

        chart.getAxisSet().adjustRange();

        // if max/min value of range is more than 1000, formatting is #.#k
        Range r = yAxis.getRange();
        if (r.lower < -1000.0 || r.upper > 1000.0)
        {
            yAxis.getTick().setFormat(new ThousandsNumberFormat());
        }
        else
        {
            yAxis.getTick().setFormat(new AmountNumberFormat());
        }

        attachTooltipTo(chart);

        model.addUpdateListener(this::updateChart);

        return chart;
    }

    protected abstract void attachTooltipTo(Chart chart);

    private void updateChart()
    {
        try
        {
            chart.suspendUpdate(true);
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            createSeries();

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

}
