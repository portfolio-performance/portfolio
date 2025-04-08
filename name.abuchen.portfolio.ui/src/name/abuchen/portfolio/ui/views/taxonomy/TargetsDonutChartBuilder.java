package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChartToolTip;
import name.abuchen.portfolio.util.ColorConversion;
import name.abuchen.portfolio.util.Pair;

public class TargetsDonutChartBuilder
{
    public void configureToolTip(CircularChartToolTip toolTip)
    {
        toolTip.setToolTipBuilder(new TaxonomyPieChartSWT.TaxonomyTooltipBuilder());
    }

    public void createCircularSeries(CircularChart chart, TaxonomyModel model)
    {
        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                        model.getTaxonomy().getName());

        circularSeries.setSliceColor(chart.getPlotArea().getBackground());

        Node rootNode = circularSeries.getRootNode();
        rootNode.setData(model.getChartRenderingRootNode());

        Map<String, Color> id2color = new HashMap<>();

        computeNodeList(model).forEach(pair -> {
            // create a new identifier because an investment vehicle can be
            // assigned to multiple classifications
            String id = pair.getLeft().getId() + pair.getRight().getId();

            Node childNode = rootNode.addChild(id, getValue(pair.getRight()) / Values.Amount.divider());
            childNode.setData(pair.getRight());

            Color color = Colors.getColor(ColorConversion.hex2RGB(pair.getLeft().getColor()));
            id2color.put(id, color);
        });

        if (id2color.isEmpty())
        {
            circularSeries.setSeries(new String[] { Messages.LabelErrorNoHoldings }, new double[] { 100 });
            circularSeries.setColor(Messages.LabelErrorNoHoldings, Colors.LIGHT_GRAY);
        }

        id2color.forEach(circularSeries::setColor);
    }

    /**
     * Computes the list of nodes to be displayed. If investment vehicles are
     * assigned to multiple nodes, then the node is merged. The left side of the
     * pair contains the relevant parent (top-level classification); the right
     * side the child node.
     */
    public List<Pair<TaxonomyNode, TaxonomyNode>> computeNodeList(TaxonomyModel model)
    {
        List<Pair<TaxonomyNode, TaxonomyNode>> answer = new ArrayList<>();

        // classified nodes
        TaxonomyNode node = model.getClassificationRootNode();
        addChildren(answer, node, node.getChildren());

        // add unclassified if included
        if (!model.isUnassignedCategoryInChartsExcluded())
        {
            TaxonomyNode unassigned = model.getUnassignedNode();
            if (model.isSecuritiesInPieChartExcluded())
            {
                answer.add(new Pair<>(unassigned, unassigned));
            }
            else
            {
                List<TaxonomyNode> children = new ArrayList<>(unassigned.getChildren());
                Collections.sort(children, (r, l) -> Long.compare(getValue(l), getValue(r)));
                addChildren(answer, unassigned, children);
            }
        }

        return answer;
    }

    private void addChildren(List<Pair<TaxonomyNode, TaxonomyNode>> answer, TaxonomyNode parent,
                    List<TaxonomyNode> children)
    {
        for (TaxonomyNode child : children)
        {
            if (getValue(child) == 0)
                continue;

            if (child.isAssignment())
            {
                answer.add(new Pair<>(parent, child));
            }
            else if (child.isClassification())
            {
                answer.add(new Pair<>(child, child));
            }
        }
    }

    private long getValue(TaxonomyNode node)
    {
        return node.getTarget().isZero() ? 0 : node.getTarget().getAmount();
    }

}
