package name.abuchen.portfolio.ui.views.taxonomy;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.PieChart;
import name.abuchen.portfolio.ui.util.chart.PieChart.RenderLabelsAlongAngle;
import name.abuchen.portfolio.ui.util.chart.PieChartToolTip;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.util.ColorConversion;

public class TaxonomyPieChartSWT implements IPieChart
{
    private PieChart chart;
    private AbstractChartPage chartPage;
    private AbstractFinanceView financeView;
    private ChartType chartType;

    private Map<String, NodeData> nodeDataMap;

    protected static final class TaxonomyTooltipBuilder implements PieChartToolTip.IToolTipBuilder
    {
        private Map<String, NodeData> nodeDataMap;

        public TaxonomyTooltipBuilder(Map<String, NodeData> nodeDataMap)
        {
            this.nodeDataMap = nodeDataMap;
        }

        @Override
        public void build(Composite container, Node currentNode)
        {
            final Composite area = new Composite(container, SWT.NONE);
            area.setLayout(new RowLayout(SWT.VERTICAL));

            Label assetLabel = new Label(area, SWT.NONE);
            assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
            assetLabel.setText(currentNode.getId());

            NodeData nodeData = nodeDataMap.get(currentNode.getId());
            if (nodeData != null)
            {
                Label info = new Label(area, SWT.NONE);
                info.setText(nodeData.value);

                if (nodeData.percentage != null)
                {
                    info = new Label(area, SWT.NONE);
                    info.setText(nodeData.percentage);
                }

                if (nodeData.totalPercentage != null)
                {
                    info = new Label(area, SWT.NONE);
                    info.setText(nodeData.totalPercentage);
                }
            }
        }
    }

    protected static class NodeData
    {
        TaxonomyNode position;
        String totalPercentage = null;
        String percentage;
        String value;
    }

    public TaxonomyPieChartSWT(AbstractChartPage page, AbstractFinanceView view, ChartType type)
    {
        this.chartPage = page;
        this.financeView = view;
        this.chartType = type;
        nodeDataMap = new HashMap<>();
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new PieChart(parent, chartType, Node::getId);
        chart.addLabelPainter(new RenderLabelsAlongAngle(chart));

        // set customized tooltip builder
        chart.getToolTip().setToolTipBuilder(new TaxonomyTooltipBuilder(this.nodeDataMap));

        // Listen on mouse clicks to update information pane
        ((Composite) chart.getPlotArea()).addListener(SWT.MouseUp,
                        event -> chart.getNodeAt(event.x, event.y).ifPresent(node -> {
                            NodeData nodeData = nodeDataMap.get(node.getId());
                            if (nodeData != null)
                                financeView.setInformationPaneInput(nodeData.position);
                        }));

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
        nodeDataMap.clear();
        TaxonomyNode taxRoot = getModel().getVirtualRootNode();

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(
                        ChartType.DONUT == chartType ? SeriesType.DOUGHNUT : SeriesType.PIE, taxRoot.getName());

        circularSeries.setBorderColor(Colors.WHITE);

        Node rootNode = circularSeries.getRootNode();
        Map<String, Color> colors = new HashMap<>();
        addNodes(nodeDataMap, colors, rootNode, taxRoot, taxRoot.getChildren(), taxRoot.getActual(),
                        getModel().isSecuritiesInPieChartExcluded());
        for (Map.Entry<String, Color> entry : colors.entrySet())
        {
            circularSeries.setColor(entry.getKey(), entry.getValue());
        }
        chart.redraw();
    }

    private void addNodes(Map<String, NodeData> dataMap, Map<String, Color> colors, Node node, TaxonomyNode parentNode,
                    List<TaxonomyNode> children, Money total, boolean excludeSecurities)
    {
        String parentColor = parentNode.getColor();
        for (TaxonomyNode child : children)
        {
            if (child.getActual().isZero())
            {
                continue;
            }
            if (getModel().isUnassignedCategoryInChartsExcluded() && getModel().getUnassignedNode().equals(child))
            {
                continue;
            }
            if (!(excludeSecurities && child.isAssignment()))
            {
                String nodeId = child.getName();
                Node childNode = node.addChild(nodeId, child.getActual().getAmount() / Values.Amount.divider());
                Color color = Colors.getColor(ColorConversion.hex2RGB(
                                child.isAssignment() ? ColorConversion.brighter(parentColor) : child.getColor()));
                colors.put(child.getName(), color);

                long actual = child.isRoot() ? total.getAmount() : child.getActual().getAmount();
                long base = child.isRoot() ? total.getAmount() : child.getParent().getActual().getAmount();

                NodeData data = new NodeData();
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

                if (!child.getChildren().isEmpty())
                {
                    addNodes(dataMap, colors, childNode, child, child.getChildren(), total, excludeSecurities);
                }
            }
        }
    }
}
