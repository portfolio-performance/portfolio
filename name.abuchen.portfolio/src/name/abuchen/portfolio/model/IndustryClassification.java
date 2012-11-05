package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import name.abuchen.portfolio.Messages;

public class IndustryClassification
{
    public static class Category
    {
        public static final String OTHER_ID = "-1"; //$NON-NLS-1$

        private String id;
        private String label;
        private String description;

        private Category parent;
        private List<Category> children = new ArrayList<Category>();

        public Category(String id, Category parent, String label)
        {
            this.id = id;
            this.parent = parent;
            this.label = label;
        }

        private Category(String id, Category parent)
        {
            this.id = id;
            this.parent = parent;
        }

        public String getId()
        {
            return id;
        }

        public String getLabel()
        {
            return label;
        }

        public String getPathLabel()
        {
            List<Category> path = getPath();
            StringBuilder buf = new StringBuilder();
            for (Category c : path)
            {
                if (c.isRoot())
                    continue;

                if (buf.length() > 0)
                    buf.append(" » "); //$NON-NLS-1$

                buf.append(c.getLabel());
            }
            return buf.toString();
        }

        public String getDescription()
        {
            return description;
        }

        public Category getParent()
        {
            return parent;
        }

        public List<Category> getChildren()
        {
            return children;
        }

        public boolean isRoot()
        {
            return getParent() == null;
        }

        public List<Category> getPath()
        {
            LinkedList<Category> path = new LinkedList<Category>();

            Category c = this;
            while (c != null)
            {
                path.addFirst(c);
                c = c.getParent();
            }

            return path;
        }

        @Override
        public String toString()
        {
            return getLabel();
        }
    }

    private static final Map<String, IndustryClassification> CLASSIFICATIONS;

    static
    {
        CLASSIFICATIONS = new HashMap<String, IndustryClassification>();

        for (String id : new String[] { "industry-classification" })
        {
            IndustryClassification c = new IndustryClassification(id);
            c.load();
            CLASSIFICATIONS.put(c.getIdentifier(), c);
        }
    }

    public static IndustryClassification lookup(String identifier)
    {
        if (identifier == null)
            return CLASSIFICATIONS.get("industry-classification");

        return CLASSIFICATIONS.get(identifier);
    }

    private final String identifier;
    private Category root;

    private IndustryClassification(String identifier)
    {
        this.identifier = identifier;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public Category getRootCategory()
    {
        return root;
    }

    public Category getCategoryById(String id)
    {
        if (id == null)
            return null;

        LinkedList<Category> stack = new LinkedList<Category>();
        stack.addAll(getRootCategory().getChildren());

        while (!stack.isEmpty())
        {
            Category c = stack.removeFirst();
            if (id.equals(c.getId()))
                return c;
            stack.addAll(c.getChildren());
        }

        return null;
    }

    private void load()
    {
        ResourceBundle bundle = ResourceBundle.getBundle("name.abuchen.portfolio.model." + identifier); //$NON-NLS-1$

        root = new Category("0", null); //$NON-NLS-1$
        root.label = Messages.LabelIndustryClassification;
        readCategory(bundle, root);
    }

    private void readCategory(ResourceBundle bundle, Category parent)
    {
        String children = getString(bundle, parent.id + ".children"); //$NON-NLS-1$
        if (children == null)
            return;

        for (String childId : children.split(",")) //$NON-NLS-1$
        {
            Category child = new Category(childId, parent);
            child.label = getString(bundle, childId + ".label"); //$NON-NLS-1$
            if (child.label == null)
                continue;

            child.description = getString(bundle, childId + ".description"); //$NON-NLS-1$

            parent.children.add(child);
            child.parent = parent;

            readCategory(bundle, child);
        }
    }

    private String getString(ResourceBundle bundle, String key)
    {
        try
        {
            return bundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return null;
        }
    }

}
