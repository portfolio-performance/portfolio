package name.abuchen.portfolio.model;

public class Exchange
{
    private String id;
    private String name;

    public Exchange()
    {
    }

    public Exchange(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
