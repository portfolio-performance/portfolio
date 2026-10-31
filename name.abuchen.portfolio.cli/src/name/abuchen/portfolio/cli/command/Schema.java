package name.abuchen.portfolio.cli.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A small JSON Schema subset (type, properties, required, items, enum,
 * nullable, multi-type values, open enum) used to declare response shapes and write-operation inputs. The
 * same declaration is rendered by <code>--help</code> and checked against
 * real output by the consistency test.
 */
public final class Schema
{
    private final String type;
    private final String description;
    private final Map<String, Schema> properties = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();
    private Schema items;
    private List<String> enumValues;
    private List<String> knownValues;
    private boolean nullable;
    private boolean freeform;
    private List<String> alternativeTypes;
    private boolean reference;

    private Schema(String type, String description)
    {
        this.type = type;
        this.description = description;
    }

    /** A value that may be any of the given scalar types, e.g. string or number. */
    public static Schema oneOfTypes(String description, String... types)
    {
        var schema = new Schema(types[0], description);
        schema.alternativeTypes = List.of(types);
        return schema;
    }

    public static Schema string(String description)
    {
        return new Schema("string", description);
    }

    /**
     * A string that names an existing entity by its UUID. It renders as a
     * string; as the input of a write it implies
     * <code>reference-not-found</code>.
     */
    public static Schema reference(String description)
    {
        var schema = new Schema("string", description);
        schema.reference = true;
        return schema;
    }

    public static Schema integer(String description)
    {
        return new Schema("integer", description);
    }

    public static Schema number(String description)
    {
        return new Schema("number", description);
    }

    public static Schema bool(String description)
    {
        return new Schema("boolean", description);
    }

    public static Schema enumOf(String description, List<String> values)
    {
        var schema = new Schema("string", description);
        schema.enumValues = List.copyOf(values);
        return schema;
    }

    /**
     * A string with known values that is deliberately not a closed enum, so
     * that a value added upstream still conforms. Rendered as the standard
     * <code>examples</code> annotation, which no validator enforces.
     */
    public static Schema openEnumOf(String description, List<String> knownValues)
    {
        var schema = new Schema("string", description);
        schema.knownValues = List.copyOf(knownValues);
        return schema;
    }

    public static Schema array(String description, Schema items)
    {
        var schema = new Schema("array", description);
        schema.items = items;
        return schema;
    }

    public static Schema object(String description)
    {
        return new Schema("object", description);
    }

    /** An object whose members aren't constrained, e.g. an embedded schema. */
    public static Schema anyObject(String description)
    {
        var schema = new Schema("object", description);
        schema.freeform = true;
        return schema;
    }

    /** Adds a property that must be present. */
    public Schema property(String name, Schema schema)
    {
        properties.put(name, schema);
        required.add(name);
        return this;
    }

    /** Adds a property that may be absent. */
    public Schema optionalProperty(String name, Schema schema)
    {
        properties.put(name, schema);
        return this;
    }

    /** Allows <code>null</code> in addition to the declared type. */
    public Schema nullable()
    {
        this.nullable = true;
        return this;
    }

    /** The description of the value itself, or null if there is none. */
    public String description()
    {
        return description;
    }

    /**
     * Plain-text help for the members of this object, one line per member
     * (<code>name (type, facts) description</code>) with nested members
     * indented below it.
     */
    public List<String> helpLines(int indent)
    {
        var lines = new ArrayList<String>();
        for (var entry : properties.entrySet())
        {
            var schema = entry.getValue();
            var facts = new ArrayList<String>();
            facts.add(schema.typeLabel());
            if (schema.enumValues != null)
                facts.add("one of " + String.join(", ", schema.enumValues));
            if (schema.knownValues != null)
                facts.add("e.g. " + String.join(", ", schema.knownValues));
            if (!required.contains(entry.getKey()))
                facts.add("optional");

            var line = " ".repeat(indent) + entry.getKey() + " (" + String.join(", ", facts) + ")";
            lines.add(schema.description != null ? line + " " + schema.description : line);
            lines.addAll(schema.element().helpLines(indent + 2));
        }
        return lines;
    }

    /** Whether this object has a member of that name. */
    public boolean declares(String member)
    {
        return properties.containsKey(member);
    }

    /** The names of the members that are required or, if <code>required</code> is false, optional. */
    public List<String> memberNames(boolean required)
    {
        return properties.keySet().stream().filter(name -> this.required.contains(name) == required).toList();
    }

    /**
     * The failure codes that reading this value as the input of a write can
     * give, by what {@link OperationInput} checks of it: a member brings
     * <code>invalid-type</code>, a required one <code>required</code>, a
     * number <code>not-positive</code> and <code>invalid-amount</code>
     * (every number a write takes is a positive amount, rate or weight, or a
     * stored amount) and a {@link #reference(String) reference}
     * <code>reference-not-found</code>. Nested members count.
     */
    public Set<ErrorCode> impliedFailureCodes()
    {
        var codes = EnumSet.noneOf(ErrorCode.class);
        collectFailureCodes(codes);
        return codes;
    }

    private void collectFailureCodes(Set<ErrorCode> codes)
    {
        if (!properties.isEmpty())
            codes.add(ErrorCode.INVALID_TYPE);
        if (!required.isEmpty())
            codes.add(ErrorCode.REQUIRED);
        if ("number".equals(type) || alternativeTypes != null && alternativeTypes.contains("number"))
        {
            codes.add(ErrorCode.NOT_POSITIVE);
            codes.add(ErrorCode.INVALID_AMOUNT);
        }
        if (reference)
            codes.add(ErrorCode.REFERENCE_NOT_FOUND);
        if (items != null)
            items.collectFailureCodes(codes);
        properties.values().forEach(member -> member.collectFailureCodes(codes));
    }

    private String typeLabel()
    {
        if (alternativeTypes != null)
            return String.join(" or ", alternativeTypes);
        if ("array".equals(type))
            return "array of " + items.typeLabel();
        if ("object".equals(type) && freeform)
            return "object, any members";
        return nullable ? type + " or null" : type;
    }

    /** The schema that carries the nested members: the item type of an array, else the schema itself. */
    private Schema element()
    {
        return items != null ? items.element() : this;
    }

    public Map<String, Object> toJson()
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("type", alternativeTypes != null ? alternativeTypes : nullable ? List.of(type, "null") : type);
        if (description != null)
            json.put("description", description);
        if (enumValues != null)
            json.put("enum", enumValues);
        if (knownValues != null)
            json.put("examples", knownValues);
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
