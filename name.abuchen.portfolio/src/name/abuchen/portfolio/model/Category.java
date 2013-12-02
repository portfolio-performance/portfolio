package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

@Deprecated
/* package */class Category
{
    private String uuid;
    private String name;
    private int percentage;

    @SuppressWarnings("unused")
    private Category parent; // needed during XStream deserialization

    private List<Category> children = new ArrayList<Category>();
    private List<Object> elements = new ArrayList<Object>();

    public String getUUID()
    {
        return uuid;
    }

    /* package */void generateUUID()
    {
        // needed to assign UUIDs when loading older versions from XML
        uuid = UUID.randomUUID().toString();
    }

    public String getName()
    {
        return name;
    }

    public int getPercentage()
    {
        return percentage;
    }

    public List<Category> getChildren()
    {
        return Collections.unmodifiableList(children);
    }

    public List<Object> getElements()
    {
        return elements;
    }

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
    public String toString()
    {
        return name;
    }
}
