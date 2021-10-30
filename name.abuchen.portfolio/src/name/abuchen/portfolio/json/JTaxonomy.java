package name.abuchen.portfolio.json;

import name.abuchen.portfolio.model.Taxonomy;

public class JTaxonomy
{
    private String name;
    private String id;
    
    public String getName()
    {
        return name;
    }

    public String getId()
    {
        return id;
    }
    
    public static JTaxonomy from(Taxonomy taxonomy)
    {
        JTaxonomy t = new JTaxonomy();
        t.name = taxonomy.getName();
        t.id = taxonomy.getId();
        return t;
    }
}
