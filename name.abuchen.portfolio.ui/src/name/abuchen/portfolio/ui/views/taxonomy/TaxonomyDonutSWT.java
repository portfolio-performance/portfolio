package name.abuchen.portfolio.ui.views.taxonomy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsCenteredInPie;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsOutsidePie;
import name.abuchen.portfolio.ui.views.IPieChart;

public class TaxonomyDonutSWT implements IPieChart
{
    private CircularChart chart;
    private DonutChartBuilder builder = new DonutChartBuilder();
    private DonutViewer chartPage;
    private AbstractFinanceView financeView;

    public TaxonomyDonutSWT(DonutViewer page, AbstractFinanceView view)
    {
        this.chartPage = page;
        this.financeView = view;
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new CircularChart(parent, SeriesType.DOUGHNUT);
        chart.addLabelPainter(new RenderLabelsCenteredInPie(chart));
        chart.addLabelPainter(new RenderLabelsOutsidePie(chart, node -> {
            final TaxonomyNode taxonomyNode = (TaxonomyNode) node.getData();
            return taxonomyNode != null ? taxonomyNode.getName() : ""; //$NON-NLS-1$
        }));

        builder.configureToolTip(chart.getToolTip());

        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.RIGHT);

        // Listen on mouse clicks to update information pane
        ((Composite) chart.getPlotArea()) //
                        .addListener(SWT.MouseUp, event -> chart.getNodeAt(event.x, event.y) //
                                        .ifPresent(node -> {
                                            TaxonomyNode taxonomoyNode = (TaxonomyNode) node.getData();
                                            if (taxonomoyNode != null)
                                                financeView.setInformationPaneInput(taxonomoyNode);
                                        }));

        builder.createCircularSeries(chart, chartPage.getModel());
        chart.updateAngleBounds();
        return chart;
    }

    @Override
    public void refresh(ClientSnapshot snapshot)
    {
        updateChart();
    }

    private void updateChart()
    {
        try
        {
            chart.suspendUpdate(true);

            for (ISeries<?> s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            builder.createCircularSeries(chart, chartPage.getModel());

            chart.updateAngleBounds();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

}
