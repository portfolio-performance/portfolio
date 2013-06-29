package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.AssignmentNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.ClassificationNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.UnassignedContainerNode;
import name.abuchen.portfolio.util.Dates;

public final class TaxonomyModel
{
    public abstract static class NodeVisitor
    {
        public abstract void visit(TaxonomyNode node);

        public final void startWith(TaxonomyNode root)
        {
            LinkedList<TaxonomyNode> stack = new LinkedList<TaxonomyNode>();
            stack.add(root);

            while (!stack.isEmpty())
            {
                TaxonomyNode node = stack.pop();
                visit(node);
                stack.addAll(node.getChildren());
            }
        }
    }

    private ClientSnapshot snapshot;

    private TaxonomyNode rootNode;
    private TaxonomyNode unassignedNode;

    /* package */TaxonomyModel(Client client, Taxonomy taxonomy)
    {
        snapshot = ClientSnapshot.create(client, Dates.today());

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

        unassignedNode = new UnassignedContainerNode(rootNode, new Classification(root, "$unassigned$",
                        "Ohne Klassifizierung"));
        rootNode.getChildren().add(unassignedNode);

        // add unassigned
        addUnassigned(client);

        // calculate actuals
        visitActuals(snapshot, rootNode);
    }

    private void addUnassigned(Client client)
    {
        final Map<Object, Assignment> vehicles = new HashMap<Object, Assignment>();

        for (Security security : client.getSecurities())
            vehicles.put(security, new Assignment(security));
        for (Account account : client.getAccounts())
            vehicles.put(account, new Assignment(account));

        new NodeVisitor()
        {
            @Override
            public void visit(TaxonomyNode node)
            {
                if (node.isUnassignedCategory())
                    return;

                Assignment assignment = node.getAssignment();
                if (assignment == null)
                    return;

                Assignment left = vehicles.remove(assignment.getInvestmentVehicle());

                int weight = left.getWeight() - assignment.getWeight();
                if (weight > 0)
                {
                    left.setWeight(weight);
                    vehicles.put(assignment.getInvestmentVehicle(), left);
                }
            }

        }.startWith(rootNode);

        List<Assignment> unassigned = new ArrayList<Assignment>(vehicles.values());
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
                    actual += p.calculateValue();
            }
            else if (assignment.getInvestmentVehicle() instanceof Account)
            {
                for (AccountSnapshot s : snapshot.getAccounts())
                {
                    if (s.getAccount().equals(assignment.getInvestmentVehicle()))
                    {
                        actual += s.getFunds();
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
    }
}
