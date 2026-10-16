package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON Schema subset (type, properties, required, items, enum,
 * nullable) used to declare response shapes and write-operation inputs. The
 * same declaration is rendered by <code>describe</code> and checked against
 * real output by the consistency test.
 */
final class Schema
{
    private final String type;
    private final String description;
    private final Map<String, Schema> properties = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();
    private Schema items;
    private List<String> enumValues;
    private boolean nullable;
    private boolean freeform;
    private Schema(String type, String description)
    {
        this.type = type;
        this.description = description;
    }

    static Schema string(String description)
    {
        return new Schema("string", description);
    }

    static Schema integer(String description)
    {
        return new Schema("integer", description);
    }

    static Schema number(String description)
    {
        return new Schema("number", description);
    }

    static Schema bool(String description)
    {
        return new Schema("boolean", description);
    }

    static Schema enumOf(String description, List<String> values)
    {
        var schema = new Schema("string", description);
        schema.enumValues = List.copyOf(values);
        return schema;
    }

    static Schema array(String description, Schema items)
    {
        var schema = new Schema("array", description);
        schema.items = items;
        return schema;
    }

    static Schema object(String description)
    {
        return new Schema("object", description);
    }

    /** An object whose members aren't constrained, e.g. an embedded schema. */
    static Schema anyObject(String description)
    {
        var schema = new Schema("object", description);
        schema.freeform = true;
        return schema;
    }

    /** Adds a property that must be present. */
    Schema property(String name, Schema schema)
    {
        properties.put(name, schema);
        required.add(name);
        return this;
    }

    /** Adds a property that may be absent. */
    Schema optionalProperty(String name, Schema schema)
    {
        properties.put(name, schema);
        return this;
    }

    /** Allows <code>null</code> in addition to the declared type. */
    Schema nullable()
    {
        this.nullable = true;
        return this;
    }

    Map<String, Object> toJson()
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("type", nullable ? List.of(type, "null") : type);
        if (description != null)
            json.put("description", description);
        if (enumValues != null)
            json.put("enum", enumValues);
        if (items != null)
            json.put("items", items.toJson());
        if ("object".equals(type) && !freeform)
        {
            var rendered = new LinkedHashMap<String, Object>();
            properties.forEach((name, schema) -> rendered.put(name, schema.toJson()));
            json.put("properties", rendered);
            json.put("required", List.copyOf(required));
            json.put("additionalProperties", false);
        }
        return json;
    }
}
