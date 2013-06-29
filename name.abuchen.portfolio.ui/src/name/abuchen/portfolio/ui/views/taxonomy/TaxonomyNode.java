package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.util.Colors;

public abstract class TaxonomyNode
{
    /* protected */static class ClassificationNode extends TaxonomyNode
    {
        private Classification classification;

        public ClassificationNode(TaxonomyNode parent, Classification classification)
        {
            super(parent);
            this.classification = classification;
        }

        @Override
        public Classification getClassification()
        {
            return classification;
        }

        @Override
        public Assignment getAssignment()
        {
            return null;
        }

        @Override
        public int getWeight()
        {
            return classification.getWeight();
        }

        @Override
        public void setWeight(int weight)
        {
            classification.setWeight(weight);
        }

        @Override
        public int getRank()
        {
            return classification.getRank();
        }

        @Override
        public void setRank(int rank)
        {
            classification.setRank(rank);
        }

        @Override
        public String getName()
        {
            return classification.getName();
        }

        @Override
        public void setName(String name)
        {
            classification.setName(name);
        }

        @Override
        public String getId()
        {
            return classification.getId();
        }

        @Override
        public String getColor()
        {
            return classification.getColor();
        }

        @Override
        public boolean hasWeightError()
        {
            if (isRoot())
                return Classification.ONE_HUNDRED_PERCENT != getClassification().getWeight();

            return Classification.ONE_HUNDRED_PERCENT != getClassification().getParent().getChildrenWeight();
        }
    }

    /* protected */static class AssignmentNode extends TaxonomyNode
    {
        private Assignment assignment;

        public AssignmentNode(TaxonomyNode parent, Assignment assignment)
        {
            super(parent);
            this.assignment = assignment;
        }

        @Override
        public Security getBackingSecurity()
        {
            if (assignment.getInvestmentVehicle() instanceof Security)
                return (Security) assignment.getInvestmentVehicle();
            else
                return null;
        }

        @Override
        public Classification getClassification()
        {
            return null;
        }

        @Override
        public Assignment getAssignment()
        {
            return assignment;
        }

        @Override
        public int getWeight()
        {
            return assignment.getWeight();
        }

        @Override
        public void setWeight(int weight)
        {
            assignment.setWeight(weight);
        }

        @Override
        public int getRank()
        {
            return assignment.getRank();
        }

        @Override
        public void setRank(int rank)
        {
            assignment.setRank(rank);
        }

        @Override
        public String getName()
        {
            return assignment.getInvestmentVehicle().toString();
        }

        @Override
        public void setName(String name)
        {
            Object investmentVehicle = assignment.getInvestmentVehicle();
            if (investmentVehicle instanceof Security)
                ((Security) investmentVehicle).setName(name);
            else
                ((Account) investmentVehicle).setName(name);
        }

        @Override
        public String getId()
        {
            Object vehicle = assignment.getInvestmentVehicle();
            return vehicle instanceof Security ? ((Security) vehicle).getUUID() : ((Account) vehicle).getUUID();
        }

        @Override
        public String getColor()
        {
            if (assignment.getInvestmentVehicle() instanceof Security)
                return Colors.EQUITY.asHex();
            else
                return Colors.CASH.asHex();
        }
    }

    /* protected */static class UnassignedContainerNode extends ClassificationNode
    {
        public UnassignedContainerNode(TaxonomyNode parent, Classification classification)
        {
            super(parent, classification);
        }

        @Override
        public boolean isUnassignedCategory()
        {
            return true;
        }

        @Override
        public String getColor()
        {
            return Colors.OTHER_CATEGORY.asHex();
        }

        @Override
        public boolean hasWeightError()
        {
            return false;
        }
    }

    private TaxonomyNode parent;

    private List<TaxonomyNode> children = new ArrayList<TaxonomyNode>();
    private long actual;

    /* package */TaxonomyNode(TaxonomyNode parent)
    {
        this.parent = parent;
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

    public Security getBackingSecurity()
    {
        return null;
    }

    public boolean isClassification()
    {
        return getClassification() != null;
    }

    public abstract Classification getClassification();

    public boolean isAssignment()
    {
        return getAssignment() != null;
    }

    public abstract Assignment getAssignment();

    public boolean isUnassignedCategory()
    {
        return false;
    }

    public long getActual()
    {
        return actual;
    }

    public void setActual(long actual)
    {
        this.actual = actual;
    }

    public abstract int getWeight();

    public abstract void setWeight(int weight);

    public abstract int getRank();

    public abstract void setRank(int rank);

    public boolean hasWeightError()
    {
        return false;
    }

    public abstract String getName();

    public abstract void setName(String name);

    public abstract String getId();

    public abstract String getColor();

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

    /* package */TaxonomyNode addChild(Classification newClassification)
    {
        Classification classification = getClassification();
        if (classification == null)
            return null;

        newClassification.setWeight(Classification.ONE_HUNDRED_PERCENT - classification.getChildrenWeight());
        newClassification.setParent(classification);
        classification.addChild(newClassification);

        TaxonomyNode newChild = new ClassificationNode(this, newClassification);

        int insertAt = isRoot() ? children.size() - 1 : children.size();
        children.add(insertAt, newChild);
        for (int ii = 0; ii < children.size(); ii++)
            children.get(ii).setRank(ii);

        return newChild;
    }

    /* package */TaxonomyNode addChild(Assignment newAssignment)
    {
        Classification classification = getClassification();
        if (classification == null)
            return null;

        classification.addAssignment(newAssignment);

        TaxonomyNode newChild = new AssignmentNode(this, newAssignment);
        newChild.setRank(getTopRank() + 1);
        children.add(newChild);
        return newChild;
    }

    /* package */void removeChild(TaxonomyNode node)
    {
        Classification classification = getClassification();
        if (classification == null)
            throw new UnsupportedOperationException();

        if (node.isClassification())
            classification.getChildren().remove(node.getClassification());
        else
            classification.getAssignments().remove(node.getAssignment());

        children.remove(node);
    }

    /* package */void moveTo(TaxonomyNode target)
    {
        moveTo(-1, target);
    }

    /* package */void moveTo(int index, TaxonomyNode target)
    {
        Classification classification = target.getClassification();
        if (classification == null)
            throw new UnsupportedOperationException();

        this.getParent().removeChild(this);
        this.parent = target;

        if (isClassification())
        {
            getClassification().setParent(classification);
            classification.getChildren().add(getClassification());
        }
        else
        {
            classification.getAssignments().add(getAssignment());
        }
        List<TaxonomyNode> siblings = target.getChildren();

        if (index == -1)
            index = siblings.size();

        int insertAt = target.isRoot() ? Math.min(index, siblings.size() - 1) : index;
        siblings.add(insertAt, this);

        for (int ii = 0; ii < siblings.size(); ii++)
            siblings.get(ii).setRank(ii);
    }

    /* package */void insertAfter(TaxonomyNode target)
    {
        moveRelativeTo(target, 1);
    }

    /* package */void insertBefore(TaxonomyNode target)
    {
        moveRelativeTo(target, 0);
    }

    private void moveRelativeTo(TaxonomyNode target, int offset)
    {
        if (target.isRoot())
            return;

        if (target.getParent() == getParent())
        {
            List<TaxonomyNode> siblings = getParent().getChildren();
            siblings.remove(this);

            int targetAt = siblings.indexOf(target) + offset;
            int insertAt = target.getParent().isRoot() ? Math.min(targetAt, siblings.size() - 1) : targetAt;

            siblings.add(insertAt, this);

            for (int ii = 0; ii < siblings.size(); ii++)
                siblings.get(ii).setRank(ii);
        }
        else
        {
            int index = target.getParent().getChildren().indexOf(target);
            moveTo(index + offset, target.getParent());
        }
    }

    /* package */int getTopRank()
    {
        if (children.isEmpty())
            return -1;
        return children.get(children.size() - 1).getRank();
    }
}
