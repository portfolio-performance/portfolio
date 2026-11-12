package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.Schema;

/**
 * Validates a JSON value against the schema subset that {@link Schema}
 * renders: type (or a list of types), enum, properties, required,
 * additionalProperties and items. Anything else in a schema is ignored; the
 * known values (<code>examples</code>) of an open enum are deliberately not
 * enforced. Extending {@link Schema} means extending this too.
 */
public final class SchemaValidator
{
    private SchemaValidator()
    {
    }

    /** @return one message per violation; empty if the value conforms */
    public static List<String> validate(JsonElement value, JsonObject schema)
    {
        var errors = new ArrayList<String>();
        validate(value, schema, "$", errors);
        return errors;
    }

    private static void validate(JsonElement value, JsonObject schema, String path, List<String> errors)
    {
        if (!matchesType(value, schema.get("type")))
        {
            errors.add(path + ": expected type " + schema.get("type") + " but was " + describe(value));
            return;
        }

        if (schema.has("enum") && !schema.getAsJsonArray("enum").contains(value))
            errors.add(path + ": " + value + " is not one of " + schema.get("enum"));

        if (value.isJsonObject() && schema.has("properties"))
            validateObject(value.getAsJsonObject(), schema, path, errors);

        if (value.isJsonArray() && schema.has("items"))
        {
            var items = schema.getAsJsonObject("items");
            var index = 0;
            for (var element : value.getAsJsonArray())
                validate(element, items, path + "[" + index++ + "]", errors);
        }
    }

    private static void validateObject(JsonObject object, JsonObject schema, String path, List<String> errors)
    {
        var properties = schema.getAsJsonObject("properties");

        if (schema.has("required"))
        {
            for (var name : schema.getAsJsonArray("required"))
            {
                if (!object.has(name.getAsString()))
                    errors.add(path + ": missing required property '" + name.getAsString() + "'");
            }
        }

        for (var entry : object.entrySet())
        {
            if (properties.has(entry.getKey()))
                validate(entry.getValue(), properties.getAsJsonObject(entry.getKey()), path + "." + entry.getKey(),
                                errors);
            else if (schema.has("additionalProperties") && !schema.get("additionalProperties").getAsBoolean())
                errors.add(path + ": undeclared property '" + entry.getKey() + "'");
        }
    }

    private static boolean matchesType(JsonElement value, JsonElement type)
    {
        if (type == null)
            return true;

        if (type.isJsonArray())
        {
            for (var candidate : (JsonArray) type)
            {
                if (matchesType(value, candidate))
                    return true;
            }
            return false;
        }

        return switch (type.getAsString())
        {
            case "null" -> value.isJsonNull();
            case "string" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
            case "boolean" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
            case "number" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
            case "integer" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                            && value.getAsBigDecimal().stripTrailingZeros().scale() <= 0;
            case "array" -> value.isJsonArray();
            case "object" -> value.isJsonObject();
            default -> throw new IllegalArgumentException("unsupported schema type " + type);
        };
    }

    private static String describe(JsonElement value)
    {
        return value.isJsonNull() ? "null" : value.toString();
    }
}
