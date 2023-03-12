package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.google.protobuf.NullValue;

import name.abuchen.portfolio.model.AttributeType.ProtoConverter;
import name.abuchen.portfolio.model.proto.v1.PAnyValue;
import name.abuchen.portfolio.model.proto.v1.PKeyValue;

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

    public Stream<Object> getAllValues()
    {
        return map.values().stream();
    }
    
    public Map<String, Object> getMap()
    {
        return Collections.unmodifiableMap(map);
    }
    
    public boolean isEmpty()
    {
        return map.isEmpty();
    }
    
    public Attributes copy()
    {
        Attributes copy = new Attributes();
        copy.map.putAll(this.map);
        return copy;
    }

    /* protobuf only */ List<PKeyValue> toProto(Client client)
    {
        if (map.isEmpty())
            return Collections.emptyList();

        List<PKeyValue> answer = new ArrayList<>();

        for (Entry<String, Object> entry : map.entrySet())
        {
            if (entry.getValue() == null)
            {
                // shortcut: we can save null values without knowing the actual
                // attribute type

                answer.add(PKeyValue.newBuilder().setKey(entry.getKey())
                                .setValue(PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE)).build());
            }
            else
            {
                client.getSettings().getAttributeTypes().filter(a -> entry.getKey().equals(a.getId())).findAny()
                                .ifPresent(at -> answer.add(PKeyValue.newBuilder().setKey(entry.getKey()).setValue(
                                                ((ProtoConverter) at.getConverter()).toProto(entry.getValue()))
                                                .build()));
            }
        }

        return answer;
    }

    /* protobuf only */ void fromProto(List<PKeyValue> attributes, Client client)
    {
        if (attributes.isEmpty())
            return;

        for (PKeyValue entry : attributes)
        {
            client.getSettings().getAttributeTypes().filter(a -> entry.getKey().equals(a.getId())).findAny()
                            .ifPresent(at -> map.put(at.getId(),
                                            ((ProtoConverter) at.getConverter()).fromProto(entry.getValue())));
        }
    }
}
