package name.abuchen.portfolio.model;

import java.util.HashMap;
import java.util.Map;

public class Attributes
{
    private Map<String, Object> map = new HashMap<>();

    public Object put(AttributeType attribute, Object value)
    {
        return map.put(attribute.getId(), value);
    }

    public Object get(AttributeType attribute)
    {
        return map.get(attribute.getId());
    }

    public Object remove(AttributeType attribute)
    {
        return map.remove(attribute.getId());
    }

    public boolean exists(AttributeType attribute)
    {
        return map.containsKey(attribute.getId());
    }

}
