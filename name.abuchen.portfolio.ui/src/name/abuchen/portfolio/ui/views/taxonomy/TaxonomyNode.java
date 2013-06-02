package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.util.Colors;

public final class TaxonomyNode
{
    private final TaxonomyNode parent;
    private final Object subject;

    private List<TaxonomyNode> children = new ArrayList<TaxonomyNode>();
    private long actual;

    /* package */TaxonomyNode(TaxonomyNode parent, Classification classification)
    {
        this.parent = parent;
        this.subject = classification;
    }

    /* package */TaxonomyNode(TaxonomyNode parent, Assignment assignment)
    {
        this.parent = parent;
        this.subject = assignment;
    }

    public TaxonomyNode getParent()
    {
        return parent;
    }

    public boolean isRoot()
    {
        return parent == null;
    }

    public List<TaxonomyNode> getChildren()
    {
        return children;
    }

    /* package */Object getSubject()
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

    public boolean isClassification()
    {
        return subject instanceof Classification;
    }

    public Classification getClassification()
    {
        return subject instanceof Classification ? (Classification) subject : null;
    }

    public Assignment getAssignment()
    {
        return subject instanceof Assignment ? (Assignment) subject : null;
    }

    public long getActual()
    {
        return actual;
    }

    public void setActual(long actual)
    {
        this.actual = actual;
    }

    public int getWeight()
    {
        if (subject instanceof Classification)
            return ((Classification) subject).getWeight();
        else
            return ((Assignment) subject).getWeight();
    }

    public void setWeight(int weight)
    {
        if (subject instanceof Classification)
            ((Classification) subject).setWeight(weight);
        else
            ((Assignment) subject).setWeight(weight);
    }

    public boolean hasWeightError()
    {
        if (subject instanceof Assignment)
            return false;

        if (isRoot())
            return Classification.ONE_HUNDRED_PERCENT != getClassification().getWeight();

        return Classification.ONE_HUNDRED_PERCENT != getClassification().getParent().getChildrenWeight();
    }

    public String getName()
    {
        if (subject instanceof Classification)
            return ((Classification) subject).getName();
        else
            return ((Assignment) subject).getInvestmentVehicle().toString();
    }

    public void setName(String name)
    {
        if (subject instanceof Classification)
        {
            ((Classification) subject).setName(name);
        }
        else
        {
            Object investmentVehicle = ((Assignment) subject).getInvestmentVehicle();
            if (investmentVehicle instanceof Security)
                ((Security) investmentVehicle).setName(name);
            else
                ((Account) investmentVehicle).setName(name);
        }
    }

    public String getId()
    {
        if (subject instanceof Classification)
            return ((Classification) subject).getId();
        else
        {
            Object vehicle = ((Assignment) subject).getInvestmentVehicle();
            return vehicle instanceof Security ? ((Security) vehicle).getUUID() : ((Account) vehicle).getUUID();
        }
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

    public TaxonomyNode addChild(Classification newClassification)
    {
        TaxonomyNode newChild = new TaxonomyNode(this, newClassification);
        children.add(newChild);
        return newChild;
    }

    public void removeChild(TaxonomyNode node)
    {
        Classification classification = getClassification();
        if (classification == null)
            throw new UnsupportedOperationException();

        Object subject = node.getSubject();
        if (subject instanceof Classification)
            classification.getChildren().remove(subject);
        else
            classification.getAssignments().remove(subject);

        children.remove(node);
    }
}
