package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsCenteredInPie;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsOutsidePie;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.util.ColorConversion;

public class TaxonomyDonutSWT implements IPieChart
{
    private CircularChart chart;
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
        chart.addLabelPainter(new RenderLabelsOutsidePie(chart, node -> ((TaxonomyNode) node.getData()).getName()));

        // set customized tooltip builder
        chart.getToolTip().setToolTipBuilder(new TaxonomyPieChartSWT.TaxonomyTooltipBuilder());

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

        updateChart();
        return chart;
    }

    @Override
    public void refresh(ClientSnapshot snapshot)
    {
        updateChart();
    }

    private void updateChart()
    {

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                        chartPage.getModel().getTaxonomy().getName());

        circularSeries.setBorderColor(Colors.WHITE);

        Node rootNode = circularSeries.getRootNode();
        rootNode.setData(getModel().getChartRenderingRootNode());

        // do not set the color on the node directly because it is reset
        // each time a new node is added; instead save them in color map and
        // apply after adding the last node
        Map<String, Color> id2color = new HashMap<>();

        chartPage.computeNodeList().forEach(pair -> {
            // create a new identifier because an investment vehicle can be
            // assigned to multiple classifications
            String id = pair.getLeft().getId() + pair.getRight().getId();

            Node childNode = rootNode.addChild(id, pair.getRight().getActual().getAmount() / Values.Amount.divider());
            childNode.setData(pair.getRight());

            Color color = Colors.getColor(ColorConversion.hex2RGB(pair.getLeft().getColor()));
            id2color.put(id, color);
        });

        id2color.entrySet().forEach(e -> circularSeries.setColor(e.getKey(), e.getValue()));

        chart.updateAngleBounds();

        chart.redraw();
    }

    private TaxonomyModel getModel()
    {
        return chartPage.getModel();
    }
}
