package name.abuchen.portfolio.model;

import java.util.LinkedList;
import java.util.List;

import name.abuchen.portfolio.model.Classification.Assignment;

public class Taxonomy
{
    public static class Visitor
    {
        public void visit(Classification classification)
        {}

        public void visit(Classification classification, Assignment assignment)
        {}
    }

    private String id;
    private String name;

    private List<String> dimensions;
    private Classification root;

    public Taxonomy(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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

        LinkedList<Classification> stack = new LinkedList<Classification>();
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

    public void foreach(Visitor visitor)
    {
        root.accept(visitor);
    }
}
