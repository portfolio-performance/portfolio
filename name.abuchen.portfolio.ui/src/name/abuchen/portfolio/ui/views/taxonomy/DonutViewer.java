package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.AssignmentNode;
import name.abuchen.portfolio.util.Pair;

/* package */class DonutViewer extends AbstractChartPage
{
    private AbstractFinanceView view;

    private IPieChart chart;

    @Inject
    @Preference(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS)
    boolean useSWTCharts;

    @Inject
    public DonutViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
        this.view = view;
    }

    @Override
    public Control createControl(Composite container)
    {
        if (this.useSWTCharts)
        {
            chart = new TaxonomyDonutSWT(this, view);
        }
        else
        {
            chart = new TaxonomyDonutBrowser(make(EmbeddedBrowser.class), this, view);
        }
        return chart.createControl(container);
    }

    /**
     * Computes the list of nodes to be displayed. If investment vehicles are
     * assigned to multiple nodes, then the node is merged. The left side of the
     * pair contains the relevant parent (top-level classification); the right
     * side the child node.
     */
    /* package */ List<Pair<TaxonomyNode, TaxonomyNode>> computeNodeList()
    {
        List<Pair<TaxonomyNode, TaxonomyNode>> answer = new ArrayList<>();

        // classified nodes
        TaxonomyNode node = getModel().getClassificationRootNode();
        addChildren(answer, node);

        // add unclassified if included
        if (!getModel().isUnassignedCategoryInChartsExcluded())
        {
            TaxonomyNode unassigned = getModel().getUnassignedNode();
            addChildren(answer, unassigned);
        }

        return answer;
    }

    private void addChildren(List<Pair<TaxonomyNode, TaxonomyNode>> answer, TaxonomyNode parent)
    {
        for (TaxonomyNode child : parent.getChildren())
        {
            if (child.getActual().isZero())
                continue;

            if (child.isAssignment())
            {
                answer.add(new Pair<>(parent, child));
            }
            else if (child.isClassification())
            {
                List<TaxonomyNode> grandchildren = new ArrayList<>();
                child.accept(n -> {
                    if (n.isAssignment())
                        grandchildren.add(n);
                });

                // merge investment vehicles that have been assigned to multiple
                // sub-node into one assignment
                grandchildren.stream() //
                                .filter(n -> !n.getActual().isZero())
                                .collect(Collectors.toMap(TaxonomyNode::getBackingInvestmentVehicle, n -> n, (r, l) -> {

                                    Assignment mergedAssignment = new Assignment(r.getBackingInvestmentVehicle(),
                                                    r.getAssignment().getWeight() + r.getAssignment().getWeight());

                                    TaxonomyNode mergedNode = new AssignmentNode(null, mergedAssignment);
                                    mergedNode.setActual(r.getActual().add(l.getActual()));

                                    return mergedNode;
                                })) //
                                .values().stream() //
                                .sorted((l, r) -> Long.compare(r.getActual().getAmount(), l.getActual().getAmount())) //
                                .forEach(grandchild -> answer.add(new Pair<>(child, grandchild)));
            }
        }
    }

    @Override
    public void beforePage() // NOSONAR
    {
    }

    @Override
    public void afterPage() // NOSONAR
    {
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        chart.refresh(null);
    }
}
