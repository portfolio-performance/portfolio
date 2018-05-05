package name.abuchen.portfolio.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.abuchen.portfolio.model.Classification.Assignment;

public class Taxonomy
{
    public static class Visitor
    {
        public void visit(Classification classification)
        {
            // to be sub-classed
        }

        public void visit(Classification classification, Assignment assignment)
        {
            // to be sub-classed
        }
    }

    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this); // NOSONAR

    private String id;
    private String name;

    private List<String> dimensions;
    private Classification root;

    public Taxonomy()
    {
        // needed for xstream de-serialization
    }

    public Taxonomy(String name)
    {
        this(UUID.randomUUID().toString(), name);
    }

    /* package */ Taxonomy(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    /* package */void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        propertyChangeSupport.firePropertyChange("name", this.name, this.name = name); //$NON-NLS-1$
    }

    public List<String> getDimensions()
    {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions)
    {
        this.dimensions = dimensions;
    }

    public Classification getRoot()
    {
        return root;
    }

    public void setRootNode(Classification node)
    {
        this.root = node;
    }

    public Classification getClassificationById(String id)
    {
        if (id == null)
            return null;

        LinkedList<Classification> stack = new LinkedList<>();
        stack.addAll(getRoot().getChildren());

        while (!stack.isEmpty())
        {
            Classification c = stack.removeFirst();
            if (id.equals(c.getId()))
                return c;
            stack.addAll(c.getChildren());
        }

        return null;
    }

    public List<Classification> getClassifications(final InvestmentVehicle vehicle)
    {
        final List<Classification> answer = new ArrayList<>();

        foreach(new Visitor()
        {
            @Override
            public void visit(Classification classification, Assignment assignment)
            {
                if (vehicle.equals(assignment.getInvestmentVehicle()))
                    answer.add(classification);
            }
        });

        return answer;
    }

    public List<Classification> getAllClassifications()
    {
        List<Classification> answer = new ArrayList<>();

        foreach(new Visitor()
        {
            @Override
            public void visit(Classification classification)
            {
                answer.add(classification);
            }
        });

        return answer;
    }

    public int getHeigth()
    {
        return getHeight(root);
    }

    private int getHeight(Classification classification)
    {
        int answer = 0;

        for (Classification c : classification.getChildren())
            answer = Math.max(getHeight(c), answer);

        return 1 + answer;
    }

    public void foreach(Visitor visitor)
    {
        root.accept(visitor);
    }

    /**
     * Returns a full copy of the taxonomy including all assignments but with
     * newly generated UUIDs.
     */
    public Taxonomy copy()
    {
        Taxonomy copy = new Taxonomy(this.name);
        if (this.dimensions != null)
            copy.setDimensions(new ArrayList<>(this.dimensions));
        copy.setRootNode(this.root.copy());
        return copy;
    }

    private Object readResolve()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
        return this;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
