package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.HashMap;
import java.util.List;
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
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.PieChart;
import name.abuchen.portfolio.ui.views.charts.IPieChart;
import name.abuchen.portfolio.util.ColorConversion;

public class TaxonomieDonutChartSWT implements IPieChart
{
    private PieChart chart;
    private AbstractChartPage chartPage;
    private ChartType chartType;


    public TaxonomieDonutChartSWT(AbstractChartPage page, ChartType type)
    {
        this.chartPage = page;
        this.chartType = type;
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new PieChart(parent, chartType);
        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.RIGHT);
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
        TaxonomyNode node = chartPage.getModel().getVirtualRootNode();

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(
                        ChartType.DONUT == chartType ? SeriesType.DOUGHNUT : SeriesType.PIE,
                        node.getName());

        circularSeries.setHighlightColor(Colors.GREEN);
        circularSeries.setBorderColor(Colors.WHITE);

        long total = getModel().getChartRenderingRootNode().getActual().getAmount();
        
        Node rootNode = circularSeries.getRootNode();
        Map<String, Color> colors = new HashMap<String, Color>();
        addNodes(colors, rootNode, node, node.getChildren());
        for (Map.Entry<String, Color> entry : colors.entrySet()) {
            circularSeries.setColor(entry.getKey(), entry.getValue());
        }
        chart.redraw();
    }

    private void addNodes(Map<String, Color> colors, Node node, TaxonomyNode parentNode, List<TaxonomyNode> children)
    {
        String parentColor = parentNode.getColor();
        for (TaxonomyNode child : children)
        {
            if (child.getActual().isZero()) {
                continue;
            }
            if (getModel().isUnassignedCategoryInChartsExcluded() && getModel().getUnassignedNode().equals(child)) {
                continue;
            }
            if (child.isAssignment()) {
                node.addChild(child.getName(), child.getActual().getAmount() / Values.Amount.divider());
                Color color = Colors.getColor(ColorConversion.hex2RGB(ColorConversion.brighter(parentColor)));
                colors.put(child.getName(), color);
            }


            if (!child.getChildren().isEmpty()) {
                addNodes(colors, node, child, child.getChildren());
            }
        }
    }

    private TaxonomyModel getModel()
    {
        return chartPage.getModel();
    }
}
