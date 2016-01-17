package name.abuchen.portfolio.ui.views.taxonomy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.AssignmentNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.ClassificationNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.UnassignedContainerNode;

public final class TaxonomyModel
{
    public interface NodeVisitor
    {
        void visit(TaxonomyNode node);
    }

    public interface TaxonomyModelChangeListener
    {
        void nodeChange(TaxonomyNode node);
    }

    private final Taxonomy taxonomy;
    private final ClientSnapshot snapshot;
    private final CurrencyConverter converter;

    private TaxonomyNode rootNode;
    private TaxonomyNode unassignedNode;
    private Map<InvestmentVehicle, Assignment> investmentVehicle2weight = new HashMap<InvestmentVehicle, Assignment>();

    private boolean excludeUnassignedCategoryInCharts = false;
    private boolean orderByTaxonomyInStackChart = false;
    private String expansionStateDefinition;
    private String expansionStateRebalancing;

    private List<TaxonomyModelChangeListener> listeners = new ArrayList<TaxonomyModelChangeListener>();

    @Inject
    /* package */TaxonomyModel(ExchangeRateProviderFactory factory, Client client, Taxonomy taxonomy)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(taxonomy);

        this.taxonomy = taxonomy;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        this.snapshot = ClientSnapshot.create(client, converter, LocalDate.now());

        Classification root = taxonomy.getRoot();
        rootNode = new ClassificationNode(null, root);

        LinkedList<TaxonomyNode> stack = new LinkedList<TaxonomyNode>();
        stack.add(rootNode);

        while (!stack.isEmpty())
        {
            TaxonomyNode m = stack.pop();

            Classification classification = m.getClassification();

            for (Classification c : classification.getChildren())
            {
                TaxonomyNode cm = new ClassificationNode(m, c);
                stack.push(cm);
                m.getChildren().add(cm);
            }

            for (Assignment assignment : classification.getAssignments())
                m.getChildren().add(new AssignmentNode(m, assignment));

            Collections.sort(m.getChildren(), new Comparator<TaxonomyNode>()
            {
                @Override
                public int compare(TaxonomyNode o1, TaxonomyNode o2)
                {
                    return o1.getRank() > o2.getRank() ? 1 : o1.getRank() == o2.getRank() ? 0 : -1;
                }
            });
        }

        unassignedNode = new UnassignedContainerNode(rootNode, new Classification(root, Classification.UNASSIGNED_ID,
                        Messages.LabelWithoutClassification));
        rootNode.getChildren().add(unassignedNode);

        // add unassigned
        addUnassigned(client);

        // calculate actuals
        visitActuals(snapshot, rootNode);

        // calculate targets
        recalculateTargets();
    }

    private void addUnassigned(Client client)
    {
        for (Security security : client.getSecurities())
        {
            Assignment assignment = new Assignment(security);
            assignment.setWeight(0);
            investmentVehicle2weight.put(security, assignment);
        }

        for (Account account : client.getAccounts())
        {
            Assignment assignment = new Assignment(account);
            assignment.setWeight(0);
            investmentVehicle2weight.put(account, assignment);
        }

        visitAll(new NodeVisitor()
        {
            @Override
            public void visit(TaxonomyNode node)
            {
                if (!(node instanceof AssignmentNode))
                    return;

                Assignment assignment = node.getAssignment();
                Assignment count = investmentVehicle2weight.get(assignment.getInvestmentVehicle());
                count.setWeight(count.getWeight() + assignment.getWeight());
            }
        });

        List<Assignment> unassigned = new ArrayList<Assignment>();
        for (Assignment assignment : investmentVehicle2weight.values())
        {
            if (assignment.getWeight() >= Classification.ONE_HUNDRED_PERCENT)
                continue;

            Assignment a = new Assignment(assignment.getInvestmentVehicle());
            a.setWeight(Classification.ONE_HUNDRED_PERCENT - assignment.getWeight());
            unassigned.add(a);

            assignment.setWeight(Classification.ONE_HUNDRED_PERCENT);
        }

        Collections.sort(unassigned, new Comparator<Assignment>()
        {
            @Override
            public int compare(Assignment o1, Assignment o2)
            {
                return o1.getInvestmentVehicle().toString().compareToIgnoreCase(o2.getInvestmentVehicle().toString());
            }
        });

        for (Assignment assignment : unassigned)
            unassignedNode.addChild(assignment);
    }

    private void visitActuals(ClientSnapshot snapshot, TaxonomyNode node)
    {
        MutableMoney actual = MutableMoney.of(snapshot.getCurrencyCode());

        for (TaxonomyNode child : node.getChildren())
        {
            visitActuals(snapshot, child);
            actual.add(child.getActual());
        }

        if (node.isAssignment())
        {
            Assignment assignment = node.getAssignment();

            AssetPosition p = snapshot.getPositionsByVehicle().get(assignment.getInvestmentVehicle());

            if (p != null)
            {
                Money valuation = p.getValuation();
                actual.add(Money.of(
                                valuation.getCurrencyCode(),
                                Math.round(valuation.getAmount() * assignment.getWeight()
                                                / (double) Classification.ONE_HUNDRED_PERCENT)));
            }
        }

        node.setActual(actual.toMoney());
    }

    private void recalculateTargets()
    {
        // see #124
        // only assigned assets go into the target value in order to allow an
        // asset allocation only for assigned securities
        rootNode.setTarget(rootNode.getActual().subtract(unassignedNode.getActual()));

        visitAll(new NodeVisitor()
        {
            @Override
            public void visit(TaxonomyNode node)
            {
                if (node.isClassification() && !node.isRoot())
                {
                    Money parent = node.getParent().getTarget();
                    Money target = Money.of(
                                    parent.getCurrencyCode(),
                                    Math.round(parent.getAmount() * node.getWeight()
                                                    / (double) Classification.ONE_HUNDRED_PERCENT));
                    node.setTarget(target);
                }
            }
        });
    }

    public boolean isUnassignedCategoryInChartsExcluded()
    {
        return excludeUnassignedCategoryInCharts;
    }

    public void setExcludeUnassignedCategoryInCharts(boolean excludeUnassignedCategoryInCharts)
    {
        this.excludeUnassignedCategoryInCharts = excludeUnassignedCategoryInCharts;
    }

    public boolean isOrderByTaxonomyInStackChart()
    {
        return orderByTaxonomyInStackChart;
    }

    public void setOrderByTaxonomyInStackChart(boolean orderByTaxonomyInStackChart)
    {
        this.orderByTaxonomyInStackChart = orderByTaxonomyInStackChart;
    }

    public String getExpansionStateDefinition()
    {
        return expansionStateDefinition;
    }

    public void setExpansionStateDefinition(String expansionStateDefinition)
    {
        this.expansionStateDefinition = expansionStateDefinition;
    }

    public String getExpansionStateRebalancing()
    {
        return expansionStateRebalancing;
    }

    public void setExpansionStateRebalancing(String expansionStateRebalancing)
    {
        this.expansionStateRebalancing = expansionStateRebalancing;
    }

    public Taxonomy getTaxonomy()
    {
        return taxonomy;
    }

    public TaxonomyNode getRootNode()
    {
        return rootNode;
    }

    public TaxonomyNode getUnassignedNode()
    {
        return unassignedNode;
    }

    public Client getClient()
    {
        return snapshot.getClient();
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public String getCurrencyCode()
    {
        return converter.getTermCurrency();
    }

    public void recalculate()
    {
        rootNode.setActual(snapshot.getMonetaryAssets());
        visitActuals(snapshot, rootNode);
        recalculateTargets();
    }

    public void visitAll(NodeVisitor visitor)
    {
        rootNode.accept(visitor);
    }

    public void addListener(TaxonomyModelChangeListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(TaxonomyModelChangeListener listener)
    {
        listeners.remove(listener);
    }

    public void fireTaxonomyModelChange(TaxonomyNode node)
    {
        for (TaxonomyModelChangeListener listener : listeners)
            listener.nodeChange(node);
    }

    public int getWeightByInvestmentVehicle(InvestmentVehicle vehicle)
    {
        return investmentVehicle2weight.get(vehicle).getWeight();
    }

    public void setWeightByInvestmentVehicle(InvestmentVehicle vehicle, int weight)
    {
        investmentVehicle2weight.get(vehicle).setWeight(weight);
    }

    public boolean hasWeightError(TaxonomyNode node)
    {
        if (node.isUnassignedCategory())
        {
            return false;
        }
        else if (node.isClassification())
        {
            if (node.isRoot())
                return node.getWeight() != Classification.ONE_HUNDRED_PERCENT;
            else
                return node.getClassification().getParent().getChildrenWeight() != Classification.ONE_HUNDRED_PERCENT;
        }
        else
        {
            // node is assignment
            return getWeightByInvestmentVehicle(node.getAssignment().getInvestmentVehicle()) != Classification.ONE_HUNDRED_PERCENT;
        }
    }
}
