package name.abuchen.portfolio.cli;

import com.google.gson.JsonObject;

/**
 * Reads the input of one operation, or one object nested in it, and reports
 * every problem as a field-level failure of the operation. A nested reader
 * names its fields after the object, such as <code>units[0].amount</code>.
 */
final class OperationInput
{
    private final OperationContext context;
    private final JsonObject json;
    private final String prefix;

    OperationInput(OperationContext context, JsonObject json)
    {
        this(context, json, "");
    }

    private OperationInput(OperationContext context, JsonObject json, String prefix)
    {
        this.context = context;
        this.json = json;
        this.prefix = prefix;
    }

    JsonObject json()
    {
        return json;
    }

    /** Reports every member that the schema of this object does not declare. */
    void rejectUnknownFields(Schema schema)
    {
        for (var member : json.keySet())
        {
            if (!schema.declares(member))
                context.fail(field(member), "unknown-field", (prefix.isEmpty() ? "The operation" : "The object")
                                + " has no field '" + member + "'.");
        }
    }

    /** The field path of the member, or of this object itself if the member is empty. */
    String field(String member)
    {
        if (prefix.isEmpty())
            return member;
        return member.isEmpty() ? prefix : prefix + "." + member;
    }
}
