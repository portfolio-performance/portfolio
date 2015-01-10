package name.abuchen.portfolio.ui.views.taxonomy;

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
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.AssignmentNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.ClassificationNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.UnassignedContainerNode;
import name.abuchen.portfolio.util.Dates;

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

    private Taxonomy taxonomy;
    private ClientSnapshot snapshot;

    private TaxonomyNode rootNode;
    private TaxonomyNode unassignedNode;
    private Map<InvestmentVehicle, Assignment> investmentVehicle2weight = new HashMap<InvestmentVehicle, Assignment>();

    private boolean excludeUnassignedCategoryInCharts = false;

    private List<TaxonomyModelChangeListener> listeners = new ArrayList<TaxonomyModelChangeListener>();

    @Inject
    /* package */TaxonomyModel(ExchangeRateProviderFactory factory, Client client, Taxonomy taxonomy)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(taxonomy);

        this.taxonomy = taxonomy;
        CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        this.snapshot = ClientSnapshot.create(client, converter, Dates.today());

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
                return o1.getInvestmentVehicle().toString().compareTo(o2.getInvestmentVehicle().toString());
            }
        });

        for (Assignment assignment : unassigned)
            unassignedNode.addChild(assignment);
    }

    private void visitActuals(ClientSnapshot snapshot, TaxonomyNode node)
    {
        long actual = 0;

        for (TaxonomyNode child : node.getChildren())
        {
            visitActuals(snapshot, child);
            actual += child.getActual();
        }

        if (node.isAssignment())
        {
            Assignment assignment = node.getAssignment();
            if (assignment.getInvestmentVehicle() instanceof Security)
            {
                PortfolioSnapshot portfolio = snapshot.getJointPortfolio();
                SecurityPosition p = portfolio.getPositionsBySecurity().get(assignment.getInvestmentVehicle());
                if (p != null)
                    actual += Math.round(p.calculateValue().getAmount() * assignment.getWeight()
                                    / (double) Classification.ONE_HUNDRED_PERCENT);
            }
            else if (assignment.getInvestmentVehicle() instanceof Account)
            {
                for (AccountSnapshot s : snapshot.getAccounts())
                {
                    if (s.getAccount().equals(assignment.getInvestmentVehicle()))
                    {
                        actual += Math.round(s.getFunds().getAmount() * assignment.getWeight() // FIXME
                                                                                               // c
                                        / (double) Classification.ONE_HUNDRED_PERCENT);
                        break;
                    }
                }
            }
            else
            {
                throw new UnsupportedOperationException(
                                "unknown element: " + assignment.getInvestmentVehicle().getClass().getName()); //$NON-NLS-1$
            }
        }

        node.setActual(actual);
    }

    private void recalculateTargets()
    {
        // see #124
        // only assigned assets go into the target value in order to allow an
        // asset allocation only for assigned securities
        rootNode.setTarget(rootNode.getActual() - unassignedNode.getActual());

        visitAll(new NodeVisitor()
        {
            @Override
            public void visit(TaxonomyNode node)
            {
                if (node.isClassification() && !node.isRoot())
                {
                    long target = Math.round(node.getParent().getTarget() * node.getWeight()
                                    / (double) Classification.ONE_HUNDRED_PERCENT);
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

    public void recalculate()
    {
        rootNode.setActual(snapshot.getAssets());
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
