package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Classification
{
    public static class Assignment
    {
        private Object investmentVehicle;
        private int weight;

        public Assignment(Security security)
        {
            this(security, 100 * Values.Weight.factor());
        }

        public Assignment(Account account)
        {
            this(account, 100 * Values.Weight.factor());
        }

        private Assignment(Object investmentVehicle, int weight)
        {
            this.weight = weight;
            this.investmentVehicle = investmentVehicle;
        }

        public int getWeight()
        {
            return weight;
        }

        public void setWeight(int weight)
        {
            this.weight = weight;
        }

        public Object getInvestmentVehicle()
        {
            return investmentVehicle;
        }
    }

    public static final int ONE_HUNDRED_PERCENT = 100 * Values.Weight.factor();

    private String id;
    private String name;
    private String description;
    private String color;

    private Classification parent;
    private List<Classification> children = new ArrayList<Classification>();
    private List<Assignment> assignments = new ArrayList<Assignment>();

    private int weight;

    public Classification(String id, String name)
    {
        this(null, id, name);
    }

    public Classification(Classification parent, String id, String name)
    {
        this.parent = parent;
        this.id = id;
        this.name = name;

        Random r = new Random();
        this.color = '#' + Integer.toHexString(((r.nextInt(128) + 127) << 16) //
                        | ((r.nextInt(128) + 127) << 8) //
                        | (r.nextInt(128) + 127));

        this.weight = 100 * Values.Weight.factor();
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public Classification getParent()
    {
        return parent;
    }

    public List<Classification> getChildren()
    {
        return children;
    }

    public void addChild(Classification classification)
    {
        children.add(classification);
    }

    public List<Assignment> getAssignments()
    {
        return assignments;
    }

    public void addAssignment(Assignment assignment)
    {
        assignments.add(assignment);
    }

    public int getWeight()
    {
        return weight;
    }

    public int getChildrenWeight()
    {
        int sum = 0;
        for (Classification child : children)
            sum += child.getWeight();
        return sum;
    }

    public void setWeight(int weight)
    {
        this.weight = weight;
    }
}
