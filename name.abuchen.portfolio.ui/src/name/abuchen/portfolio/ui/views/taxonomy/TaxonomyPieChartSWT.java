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
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsAlongAngle;
import name.abuchen.portfolio.ui.util.chart.CircularChartToolTip;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.util.ColorConversion;

public class TaxonomyPieChartSWT implements IPieChart
{
    private CircularChart chart;
    private AbstractChartPage chartPage;
    private AbstractFinanceView financeView;

    protected static final class TaxonomyTooltipBuilder implements CircularChartToolTip.IToolTipBuilder
    {
        @Override
        public void build(Composite container, Node chartNode)
        {
            TaxonomyNode rootNode = (TaxonomyNode) chartNode.getDataModel().getRootNode().getData();
            TaxonomyNode taxonomyNode = (TaxonomyNode) chartNode.getData();

            final Composite area = new Composite(container, SWT.NONE);
            area.setLayout(new RowLayout(SWT.VERTICAL));

            if (rootNode == null || taxonomyNode == null)
            {
                Label assetLabel = new Label(area, SWT.NONE);
                assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                assetLabel.setText(chartNode.getId());
            }
            else
            {
                Label assetLabel = new Label(area, SWT.NONE);
                assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                assetLabel.setText(taxonomyNode.getName());

                Label info = new Label(area, SWT.NONE);
                info.setText(Values.Money.format(taxonomyNode.getActual()));

                if (!taxonomyNode.isRoot())
                {
                    info = new Label(area, SWT.NONE);

                    info.setText(String.format("%s %s", //$NON-NLS-1$
                                    Values.Percent2.format(taxonomyNode.getActual().getAmount()
                                                    / (double) taxonomyNode.getParent().getActual().getAmount()),
                                    taxonomyNode.getParent().getName()));
                }

                if (taxonomyNode.getParent() != rootNode)
                {
                    info = new Label(area, SWT.NONE);
                    info.setText(MessageFormat.format(Messages.LabelTotalValuePercent, Values.Percent2.format(
                                    taxonomyNode.getActual().getAmount() / (double) rootNode.getActual().getAmount())));
                }
            }
        }
    }

    public TaxonomyPieChartSWT(AbstractChartPage page, AbstractFinanceView view)
    {
        this.chartPage = page;
        this.financeView = view;
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new CircularChart(parent, SeriesType.PIE, node -> ((TaxonomyNode) node.getData()).getName());
        chart.addLabelPainter(new RenderLabelsAlongAngle(chart));

        // set customized tooltip builder
        chart.getToolTip().setToolTipBuilder(new TaxonomyTooltipBuilder());

        // Listen on mouse clicks to update information pane
        ((Composite) chart.getPlotArea()).addListener(SWT.MouseUp,
                        event -> chart.getNodeAt(event.x, event.y).ifPresent(node -> {
                            TaxonomyNode taxonomyNode = (TaxonomyNode) node.getData();
                            if (taxonomyNode != null)
                                financeView.setInformationPaneInput(taxonomyNode);
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
        TaxonomyNode taxRoot = getModel().getVirtualRootNode();

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.PIE,
                        taxRoot.getName());

        circularSeries.setSliceColor(chart.getPlotArea().getBackground());

        Node rootNode = circularSeries.getRootNode();
        rootNode.setData(taxRoot);

        Map<String, Color> id2color = new HashMap<>();
        addNodes(id2color, rootNode, taxRoot, taxRoot.getChildren(), taxRoot.getActual(),
                        getModel().isSecuritiesInPieChartExcluded());

        id2color.entrySet().forEach(e -> circularSeries.setColor(e.getKey(), e.getValue()));

        chart.updateAngleBounds();

        chart.redraw();
    }

    private void addNodes(Map<String, Color> id2color, Node node, TaxonomyNode parentNode, List<TaxonomyNode> children,
                    Money total, boolean excludeSecurities)
    {
        String parentColor = parentNode.getColor();
        for (TaxonomyNode child : children)
        {
            if (child.getActual().isZero())
                continue;

            if (getModel().isUnassignedCategoryInChartsExcluded() && getModel().getUnassignedNode().equals(child))
                continue;

            if (!(excludeSecurities && child.isAssignment()))
            {
                // aggregated id b/c vehicle can be assigned to multiple
                // classifications

                String id = parentNode.getId() + child.getId();

                Node childNode = node.addChild(id, child.getActual().getAmount() / Values.Amount.divider());
                childNode.setData(child);

                Color color = Colors.getColor(ColorConversion.hex2RGB(
                                child.isAssignment() ? ColorConversion.brighter(parentColor) : child.getColor()));
                id2color.put(id, color);

                if (!child.getChildren().isEmpty())
                    addNodes(id2color, childNode, child, child.getChildren(), total, excludeSecurities);
            }
        }
    }
}
