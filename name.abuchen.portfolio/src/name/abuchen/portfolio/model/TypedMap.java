package name.abuchen.portfolio.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.protobuf.NullValue;

import name.abuchen.portfolio.model.proto.v1.PAnyValue;
import name.abuchen.portfolio.model.proto.v1.PKeyValue;
import name.abuchen.portfolio.model.proto.v1.PMap;
import name.abuchen.portfolio.money.Money;

public class TypedMap extends HashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    @Override
    public Object put(String key, Object value)
    {
        validate(value);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> mapToCopy)
    {
        Iterator<?> it = mapToCopy.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
            Object value = entry.getValue();
            validate(value);
        }
        super.putAll(mapToCopy);
    }

    private void validate(Object value)
    {
        if (value == null)
            return;
        if (value instanceof Boolean)
            return;
        if (value instanceof String)
            return;
        if (value instanceof Money)
            return;

        throw new IllegalArgumentException(value.getClass().getName());
    }

    public boolean getBoolean(String key)
    {
        Object answer = get(key);

        if (answer == null)
            return false;

        if (answer instanceof Boolean b)
            return b;
        
        if (answer instanceof String s)
            return Boolean.getBoolean(s);

        throw new IllegalArgumentException(key);
    }

    public void putBoolean(String key, boolean value)
    {
        super.put(key, value);
    }

    public String getString(String key)
    {
        Object answer = get(key);

        if (answer == null)
            return null;

        if (answer instanceof String s)
            return s;

        throw new IllegalArgumentException(key);
    }

    public void putString(String key, String value)
    {
        super.put(key, value);
    }

    /* package */ PMap toProto()
    {
        PMap.Builder map = PMap.newBuilder();

        for (Entry<String, Object> entry : entrySet())
        {
            PKeyValue.Builder newEntry = PKeyValue.newBuilder();
            newEntry.setKey(entry.getKey());

            Object value = entry.getValue();
            if (value == null)
                newEntry.setValue(PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build());
            else if (value instanceof String s)
                newEntry.setValue(PAnyValue.newBuilder().setString(s));
            else if (value instanceof Boolean b)
                newEntry.setValue(PAnyValue.newBuilder().setBool(b));
            else
                throw new IllegalArgumentException(value.getClass().getName());

            map.addEntries(newEntry);
        }

        return map.build();
    }

    /* package */ void fromProto(PMap map)
    {
        this.clear();

        map.getEntriesList().forEach(entry -> {

            String key = entry.getKey();

            Object value = null;

            if (entry.getValue().hasString())
                value = entry.getValue().getString();
            else if (entry.getValue().hasBool())
                value = entry.getValue().getBool();

            // ignore unknown types upon reading

            super.put(key, value);
        });
    }
}
