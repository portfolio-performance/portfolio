package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

@Deprecated(since = "2013")
/* package */class Category
{
    private String uuid;
    private String name;
    private int percentage;

    @SuppressWarnings("unused")
    private Category parent; // needed during XStream deserialization

    private List<Category> children = new ArrayList<Category>();
    private List<Object> elements = new ArrayList<Object>();

    @Deprecated(since = "2013")
    public String getUUID()
    {
        return uuid;
    }

    @Deprecated(since = "2013")
    /* package */void generateUUID()
    {
        // needed to assign UUIDs when loading older versions from XML
        uuid = UUID.randomUUID().toString();
    }

    @Deprecated(since = "2013")
    public String getName()
    {
        return name;
    }

    @Deprecated(since = "2013")
    public int getPercentage()
    {
        return percentage;
    }

    @Deprecated(since = "2013")
    public List<Category> getChildren()
    {
        return Collections.unmodifiableList(children);
    }

    @Deprecated(since = "2013")
    public List<Object> getElements()
    {
        return elements;
    }

    @Deprecated(since = "2013")
    public List<Category> flatten()
    {
        List<Category> answer = new ArrayList<Category>();
        answer.add(this);

        Stack<Category> stack = new Stack<Category>();
        stack.push(this);

        while (!stack.isEmpty())
        {
            Category c = stack.pop();
            answer.addAll(c.children);
            stack.addAll(c.children);
        }

        return answer;
    }

    @Override
    @Deprecated(since = "2013")
    public String toString()
    {
        return name;
    }
}
