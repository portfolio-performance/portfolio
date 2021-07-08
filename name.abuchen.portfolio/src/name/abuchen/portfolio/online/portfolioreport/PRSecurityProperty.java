package name.abuchen.portfolio.online.portfolioreport;

import name.abuchen.portfolio.model.SecurityProperty;

public class PRSecurityProperty
{
    public String name;
    public String type;
    public String value;

    public PRSecurityProperty(SecurityProperty property)
    {
        this.type = property.getType().toString();
        this.name = property.getName();
        this.value = property.getValue();
    }

}
