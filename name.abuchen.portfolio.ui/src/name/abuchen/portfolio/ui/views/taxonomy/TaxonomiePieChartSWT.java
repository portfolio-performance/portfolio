package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class TaxonomiePieChartSWT implements IPieChart
{
    private PieChart chart;
    private AbstractChartPage chartPage;
    private ChartType chartType;


    public TaxonomiePieChartSWT(AbstractChartPage page, ChartType type)
    {
        this.chartPage = page;
        this.chartType = type;
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new PieChart(parent, chartType, new PieChart.ILabelProvider() {
            @Override
            public String getLabel(Node node)
            {
                return node.getId();
            }
            
        });
        chart.getTitle().setVisible(false);
        chart.getLegend().setVisible(false);
        updateChart();
        return chart;
    }

    @Override
    public void refresh(ClientSnapshot snapshot)
    {
        updateChart();
    }

    private TaxonomyModel getModel()
    {
        return chartPage.getModel();
    }

    private void updateChart()
    {
        TaxonomyNode node = getModel().getVirtualRootNode();

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(
                        ChartType.DONUT == chartType ? SeriesType.DOUGHNUT : SeriesType.PIE,
                        node.getName());

        circularSeries.setHighlightColor(Colors.GREEN);
        circularSeries.setBorderColor(Colors.WHITE);
        
        Node rootNode = circularSeries.getRootNode();
        Map<String, Color> colors = new HashMap<String, Color>();
        addNodes(colors, rootNode, node, node.getChildren(), getModel().isSecuritiesInPieChartExcluded());
        for (Map.Entry<String, Color> entry : colors.entrySet()) {
            circularSeries.setColor(entry.getKey(), entry.getValue());
        }
        chart.redraw();
    }

    private void addNodes(Map<String, Color> colors, Node node, TaxonomyNode parentNode, List<TaxonomyNode> children, boolean excludeSecurities)
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
            if (!(excludeSecurities && child.isAssignment())) {
                Node childNode = node.addChild(child.getName(), child.getActual().getAmount() / Values.Amount.divider());

                Color color = Colors.getColor(ColorConversion.hex2RGB(child.isAssignment() ? ColorConversion.brighter(parentColor) : child.getColor()));
                colors.put(child.getName(), color);
                if (!child.getChildren().isEmpty()) {
                    addNodes(colors, childNode, child, child.getChildren(), excludeSecurities);
                }
            }
        }
    }
}
