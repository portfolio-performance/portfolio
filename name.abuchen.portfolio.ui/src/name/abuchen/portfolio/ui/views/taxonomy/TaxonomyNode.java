package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
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
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.Dates;

/* package */class TaxonomyNode
{
    private TaxonomyNode parent;
    private List<TaxonomyNode> children = new ArrayList<TaxonomyNode>();

    private final Object subject;
    private long actual;

    private TaxonomyNode(TaxonomyNode parent, Object subject)
    {
        this.parent = parent;
        this.subject = subject;
    }

    public static TaxonomyNode create(Client client, Taxonomy taxonomy)
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, Dates.today());

        Classification root = taxonomy.getRoot();
        TaxonomyNode modelRoot = new TaxonomyNode(null, root);

        LinkedList<TaxonomyNode> stack = new LinkedList<TaxonomyNode>();
        stack.add(modelRoot);

        while (!stack.isEmpty())
        {
            TaxonomyNode m = stack.pop();

            Classification classification = (Classification) m.getSubject();

            for (Classification c : classification.getChildren())
            {
                TaxonomyNode cm = new TaxonomyNode(m, c);
                stack.push(cm);
                m.children.add(cm);
            }

            for (Assignment assignment : classification.getAssignments())
                m.children.add(new TaxonomyNode(m, assignment));
        }

        // calculate actuals
        visitActuals(snapshot, modelRoot);

        return modelRoot;
    }

    public TaxonomyNode getParent()
    {
        return parent;
    }

    public List<TaxonomyNode> getChildren()
    {
        return children;
    }

    private Object getSubject()
    {
        return subject;
    }

    public Security getBackingSecurity()
    {
        if (subject instanceof Assignment)
        {
            Object investmentVehicle = ((Assignment) subject).getInvestmentVehicle();
            if (investmentVehicle instanceof Security)
                return (Security) investmentVehicle;
        }
        return null;
    }

    public long getActual()
    {
        return actual;
    }

    public String getName()
    {
        if (subject instanceof Classification)
            return ((Classification) subject).getName();
        else
            return ((Assignment) subject).getInvestmentVehicle().toString();
    }

    public String getColor()
    {
        if (subject instanceof Classification)
            return ((Classification) subject).getColor();

        Assignment assignment = (Assignment) subject;
        if (assignment.getInvestmentVehicle() instanceof Security)
            return Colors.EQUITY.asHex();
        else
            return Colors.CASH.asHex();
    }

    public List<TaxonomyNode> getPath()
    {
        LinkedList<TaxonomyNode> path = new LinkedList<TaxonomyNode>();

        TaxonomyNode item = this;
        while (item != null)
        {
            path.addFirst(item);
            item = item.getParent();
        }

        return path;
    }

    private static void visitActuals(ClientSnapshot snapshot, TaxonomyNode model)
    {
        long actual = 0;

        for (TaxonomyNode child : model.children)
        {
            visitActuals(snapshot, child);
            actual += child.actual;
        }

        if (model.getSubject() instanceof Assignment)
        {
            Assignment assignment = (Assignment) model.getSubject();
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
                    if (s.getAccount() == assignment.getInvestmentVehicle())
                        actual += s.getFunds();
                }
            }
            else
            {
                throw new RuntimeException("unknown element: " + assignment.getClass().getName()); //$NON-NLS-1$
            }
        }

        model.actual = actual;
    }
}
