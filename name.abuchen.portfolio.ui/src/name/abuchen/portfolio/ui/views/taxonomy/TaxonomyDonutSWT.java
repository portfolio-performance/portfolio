package name.abuchen.portfolio.ui.views.taxonomy;

import java.text.MessageFormat;
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

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.PieChart;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.util.ColorConversion;

public class TaxonomyDonutSWT implements IPieChart
{
    private PieChart chart;
    private AbstractChartPage chartPage;
    private AbstractFinanceView financeView;
    private ChartType chartType;

    private Map<String, TaxonomyPieChartSWT.NodeData> nodeDataMap;

    public TaxonomyDonutSWT(AbstractChartPage page, AbstractFinanceView view, ChartType type)
    {
        this.chartPage = page;
        this.financeView = view;
        this.chartType = type;
        nodeDataMap = new HashMap<>();
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new PieChart(parent, chartType);

        // set customized tooltip builder
        chart.getToolTip().setToolTipBuilder(new TaxonomyPieChartSWT.TaxonomyTooltipBuilder(this.nodeDataMap));

        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.RIGHT);

        // Listen on mouse clicks to update information pane
        ((Composite) chart.getPlotArea()).addListener(SWT.MouseUp, event -> {
            Node node = chart.getNodeAt(event.x, event.y);
            if (node == null)
                return;
            TaxonomyPieChartSWT.NodeData nodeData = nodeDataMap.get(node.getId());
            if (nodeData != null)
                financeView.setInformationPaneInput(nodeData.position);
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
                        ChartType.DONUT == chartType ? SeriesType.DOUGHNUT : SeriesType.PIE, node.getName());

        circularSeries.setHighlightColor(Colors.GREEN);
        circularSeries.setBorderColor(Colors.WHITE);

        Money total = getModel().getChartRenderingRootNode().getActual();

        Node rootNode = circularSeries.getRootNode();
        Map<String, Color> colors = new HashMap<>();
        addNodes(nodeDataMap, colors, rootNode, node, node.getChildren(), total);
        for (Map.Entry<String, Color> entry : colors.entrySet())
        {
            circularSeries.setColor(entry.getKey(), entry.getValue());
        }
        chart.redraw();
    }

    private void addNodes(Map<String, TaxonomyPieChartSWT.NodeData> dataMap, Map<String, Color> colors, Node node,
                    TaxonomyNode parentNode, List<TaxonomyNode> children, Money total)
    {
        String parentColor = parentNode.getColor();
        for (TaxonomyNode child : children)
        {
            if (child.getActual().isZero())
                continue;

            if (getModel().isUnassignedCategoryInChartsExcluded() && getModel().getUnassignedNode().equals(child))
                continue;

            if (child.isAssignment())
            {
                long actual = child.isRoot() ? total.getAmount() : child.getActual().getAmount();
                long base = child.isRoot() ? total.getAmount() : child.getParent().getActual().getAmount();

                String nodeId = child.getName();
                node.addChild(nodeId, child.getActual().getAmount() / Values.Amount.divider());
                Color color = Colors.getColor(ColorConversion.hex2RGB(ColorConversion.brighter(parentColor)));
                colors.put(child.getName(), color);
                TaxonomyPieChartSWT.NodeData data = new TaxonomyPieChartSWT.NodeData();
                data.position = child;
                if (child.getParent() != null && !child.getParent().isRoot())
                {
                    data.totalPercentage = MessageFormat.format(Messages.LabelTotalValuePercent,
                                    Values.Percent2.format(actual / (double) total.getAmount()));
                }
                data.percentage = Values.Percent2.format(actual / (double) base);
                if (child.getParent() != null)
                {
                    data.percentage = String.format("%s %s", //$NON-NLS-1$
                                    data.percentage, child.getParent().getName());
                }

                data.value = Values.Money.format(child.getActual());
                dataMap.put(nodeId, data);
            }

            if (!child.getChildren().isEmpty())
            {
                addNodes(dataMap, colors, node, child, child.getChildren(), total);
            }
        }
    }

    private TaxonomyModel getModel()
    {
        return chartPage.getModel();
    }
}
