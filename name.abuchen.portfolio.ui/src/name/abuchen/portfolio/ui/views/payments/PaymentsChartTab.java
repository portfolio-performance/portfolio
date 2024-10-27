package name.abuchen.portfolio.ui.views.payments;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.Range;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.chart.PlainChart;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;

public class PaymentsChartTab implements PaymentsTab
{

    @Inject
    protected PaymentsViewModel model;

    @Inject
    protected AbstractFinanceView view;

    private LocalResourceManager resources;
    private PaymentsChartBuilder chartBuilder;
    private Chart chart;

    @Inject
    @Optional
    public void onDiscreedModeChanged(@UIEventTopic(UIConstants.Event.Global.DISCREET_MODE) Object obj)
    {
        if (chart != null)
            chart.redraw();
    }

    protected void setChartBuilder(PaymentsChartBuilder chartBuilder)
    {
        this.chartBuilder = chartBuilder;
    }

    protected Chart getChart()
    {
        return chart;
    }

    protected LocalResourceManager getResources()
    {
        return resources;
    }

    @Override
    public String getLabel()
    {
        return chartBuilder.getLabel();
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

        chartBuilder.configure(chart, data -> view.setInformationPaneInput(data));

        try
        {
            chart.suspendUpdate(true);
            chartBuilder.createSeries(chart, model);
            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }

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

        model.addUpdateListener(this::updateChart);

        return chart;
    }

    private void updateChart()
    {
        try
        {
            chart.suspendUpdate(true);
            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            chartBuilder.createSeries(chart, model);

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }
}
