package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.NodeVisitor;

public abstract class TaxonomyNode implements Adaptable
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
        public <T> T adapt(Class<T> type)
        {
            if (type == Named.class || type == Annotated.class)
                return type.cast(classification);
            else
                return super.adapt(type);
        }
        
        @Override
        public String toString()
        {
            return getName();
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
            return assignment.getInvestmentVehicle().getName();
        }

        @Override
        public void setName(String name)
        {
            assignment.getInvestmentVehicle().setName(name);
        }

        @Override
        public String getId()
        {
            return assignment.getInvestmentVehicle().getUUID();
        }

        @Override
        public String getColor()
        {
            if (assignment.getInvestmentVehicle() instanceof Security)
                return Colors.toHex(Colors.EQUITY);
            else
                return Colors.toHex(Colors.CASH);
        }

        @Override
        public <T> T adapt(Class<T> type)
        {
            if (type == Named.class || type == Annotated.class)
                return type.cast(assignment.getInvestmentVehicle());
            else
                return super.adapt(type);
        }
    }

    /* protected */static class UnassignedContainerNode extends ClassificationNode
    {
        public UnassignedContainerNode(TaxonomyNode parent, Classification classification)
        {
            super(parent, classification);
            super.setWeight(0);
        }

        @Override
        public boolean isUnassignedCategory()
        {
            return true;
        }

        @Override
        public String getColor()
        {
            return Colors.toHex(Colors.OTHER_CATEGORY);
        }
    }

    private TaxonomyNode parent;

    private List<TaxonomyNode> children = new ArrayList<>();
    private Money actual;
    private Money target;

    /* package */ TaxonomyNode(TaxonomyNode parent)
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

    public TaxonomyNode getChildByInvestmentVehicle(InvestmentVehicle investmentVehicle)
    {
        for (TaxonomyNode child : children)
        {
            if (child.isAssignment() && investmentVehicle.equals(child.getAssignment().getInvestmentVehicle()))
                return child;
        }

        return null;
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

    public Money getActual()
    {
        return actual;
    }

    public void setActual(Money actual)
    {
        this.actual = actual;
    }

    public Money getTarget()
    {
        return target;
    }

    public void setTarget(Money target)
    {
        this.target = target;
    }

    public abstract String getId();

    public abstract String getName();

    public abstract void setName(String name);

    public abstract int getWeight();

    public abstract void setWeight(int weight);

    public abstract int getRank();

    public abstract void setRank(int rank);

    public abstract String getColor();

    public List<TaxonomyNode> getPath()
    {
        LinkedList<TaxonomyNode> path = new LinkedList<>();

        TaxonomyNode item = this;
        while (item != null)
        {
            path.addFirst(item);
            item = item.getParent();
        }

        return path;
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Security.class)
            return type.cast(getBackingSecurity());
        else if (type == Attributable.class)
            return type.cast(getBackingSecurity());
        else
            return null;
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

        // set actuals and target; will be calculated later but must not be null
        newChild.setActual(Money.of(actual.getCurrencyCode(), 0));
        newChild.setTarget(Money.of(target.getCurrencyCode(), 0));

        // unclassified node shall stay at the end
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

    private void moveTo(int index, TaxonomyNode target)
    {
        Classification classification = target.getClassification();
        if (classification == null)
            throw new UnsupportedOperationException();

        // if node is assignment *and* target contains assignment for same
        // investment vehicle *then* merge nodes

        if (isAssignment())
        {
            InvestmentVehicle investmentVehicle = getAssignment().getInvestmentVehicle();
            TaxonomyNode sibling = target.getChildByInvestmentVehicle(investmentVehicle);

            if (sibling != null)
            {
                sibling.absorb(this);
                return;
            }
        }

        // change parent, update children collections and rank

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

    private void absorb(TaxonomyNode node)
    {
        if (!node.getAssignment().getInvestmentVehicle().equals(getAssignment().getInvestmentVehicle()))
            throw new UnsupportedOperationException();

        node.getParent().removeChild(node);

        int weight = Math.min(getWeight() + node.getWeight(), Classification.ONE_HUNDRED_PERCENT);
        setWeight(weight);
    }

    /* package */int getTopRank()
    {
        if (children.isEmpty())
            return -1;
        return children.get(children.size() - 1).getRank();
    }

    public void accept(NodeVisitor visitor)
    {
        visitor.visit(this);

        for (TaxonomyNode child : new ArrayList<TaxonomyNode>(children))
            child.accept(visitor);
    }
}
