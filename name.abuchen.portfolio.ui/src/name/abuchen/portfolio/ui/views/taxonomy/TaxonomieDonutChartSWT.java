package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.PieChart;
import name.abuchen.portfolio.ui.views.charts.IPieChart;
import name.abuchen.portfolio.util.ColorConversion;

public class TaxonomieDonutChartSWT implements IPieChart
{
    private PieChart chart;
    private AbstractChartPage chartPage;
    private AbstractFinanceView financeView;
    private ChartType chartType;

    private Map<String, NodeData> nodeDataMap;

    private class NodeData
    {
        TaxonomyNode position;
        Double percentage;
        String percentageString;
        String shares;
        String valueSingle;
        String value;
    }

    public TaxonomieDonutChartSWT(AbstractChartPage page, AbstractFinanceView view, ChartType type)
    {
        this.chartPage = page;
        this.financeView = view;
        this.chartType = type;
        nodeDataMap = new HashMap<String, TaxonomieDonutChartSWT.NodeData>();
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new PieChart(parent, chartType);
        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.RIGHT);

        // Listen on mouse clicks to update information pane
        ((Composite)chart.getPlotArea()).addListener(SWT.MouseUp, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                Node node = chart.getNodeAt(event.x, event.y);
                if (node == null) {
                    return;
                }
                NodeData nodeData = nodeDataMap.get(node.getId());
                if (nodeData != null) {
                    financeView.setInformationPaneInput(nodeData.position);
                }
            }
        });

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
        nodeDataMap.clear();
        TaxonomyNode node = chartPage.getModel().getVirtualRootNode();

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(
                        ChartType.DONUT == chartType ? SeriesType.DOUGHNUT : SeriesType.PIE,
                        node.getName());

        circularSeries.setHighlightColor(Colors.GREEN);
        circularSeries.setBorderColor(Colors.WHITE);

        // TODO: integrate for tooltip
        long total = getModel().getChartRenderingRootNode().getActual().getAmount();
        
        Node rootNode = circularSeries.getRootNode();
        Map<String, Color> colors = new HashMap<String, Color>();
        addNodes(nodeDataMap, colors, rootNode, node, node.getChildren());
        for (Map.Entry<String, Color> entry : colors.entrySet()) {
            circularSeries.setColor(entry.getKey(), entry.getValue());
        }
        chart.redraw();
    }

    private void addNodes(Map<String, NodeData> dataMap, Map<String, Color> colors, Node node, TaxonomyNode parentNode, List<TaxonomyNode> children)
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
                String nodeId = child.getName();
                node.addChild(nodeId, child.getActual().getAmount() / Values.Amount.divider());
                Color color = Colors.getColor(ColorConversion.hex2RGB(ColorConversion.brighter(parentColor)));
                colors.put(child.getName(), color);
                NodeData data = new NodeData();
                // TODO: add useful data
                data.position = child;
                data.value = Values.Money.format(child.getActual());
                dataMap.put(nodeId, data);
            }


            if (!child.getChildren().isEmpty()) {
                addNodes(dataMap, colors, node, child, child.getChildren());
            }
        }
    }

    private TaxonomyModel getModel()
    {
        return chartPage.getModel();
    }
}
