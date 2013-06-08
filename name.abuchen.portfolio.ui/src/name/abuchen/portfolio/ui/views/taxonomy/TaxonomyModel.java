package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

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
import name.abuchen.portfolio.util.Dates;

public class TaxonomyModel
{
    private ClientSnapshot snapshot;

    private TaxonomyNode rootNode;

    /* package */TaxonomyModel(Client client, Taxonomy taxonomy)
    {
        snapshot = ClientSnapshot.create(client, Dates.today());

        Classification root = taxonomy.getRoot();
        rootNode = new TaxonomyNode(null, root);

        LinkedList<TaxonomyNode> stack = new LinkedList<TaxonomyNode>();
        stack.add(rootNode);

        while (!stack.isEmpty())
        {
            TaxonomyNode m = stack.pop();

            Classification classification = (Classification) m.getSubject();

            for (Classification c : classification.getChildren())
            {
                TaxonomyNode cm = new TaxonomyNode(m, c);
                stack.push(cm);
                m.getChildren().add(cm);
            }

            for (Assignment assignment : classification.getAssignments())
                m.getChildren().add(new TaxonomyNode(m, assignment));

            Collections.sort(m.getChildren(), new Comparator<TaxonomyNode>()
            {
                @Override
                public int compare(TaxonomyNode o1, TaxonomyNode o2)
                {
                    return o1.getRank() > o2.getRank() ? 1 : o1.getRank() == o2.getRank() ? 0 : -1;
                }
            });
        }

        // calculate actuals
        visitActuals(snapshot, rootNode);
    }

    private void visitActuals(ClientSnapshot snapshot, TaxonomyNode model)
    {
        long actual = 0;

        for (TaxonomyNode child : model.getChildren())
        {
            visitActuals(snapshot, child);
            actual += child.getActual();
        }

        if (model.getSubject() instanceof Assignment)
        {
            Assignment assignment = model.getAssignment();
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

        model.setActual(actual);
    }

    public TaxonomyNode getRootNode()
    {
        return rootNode;
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

    public List<TaxonomyNode> getTreeElements()
    {
        List<TaxonomyNode> answer = new ArrayList<TaxonomyNode>();

        LinkedList<TaxonomyNode> stack = new LinkedList<TaxonomyNode>();
        stack.add(rootNode);

        while (!stack.isEmpty())
        {
            TaxonomyNode node = stack.pop();
            answer.add(node);
            stack.addAll(node.getChildren());
        }

        return answer;
    }
}
