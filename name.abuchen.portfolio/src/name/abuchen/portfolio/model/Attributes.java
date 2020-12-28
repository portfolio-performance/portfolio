package name.abuchen.portfolio.model;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Attributes
{
    private Map<AttributeType, Object> map = new HashMap<>();

    public Object put(AttributeType attribute, Object value)
    {
        return map.put(attribute, value);
    }

    public Object get(AttributeType attribute)
    {
        return map.get(attribute);
    }

    public Object remove(AttributeType attribute)
    {
        return map.remove(attribute);
    }

    public boolean exists(AttributeType attribute)
    {
        return map.containsKey(attribute);
    }

    public Stream<Object> getAllValues()
    {
        return map.values().stream();
    }

    public Map<AttributeType, Object> getAll()
    {
        return map;
    }

}
