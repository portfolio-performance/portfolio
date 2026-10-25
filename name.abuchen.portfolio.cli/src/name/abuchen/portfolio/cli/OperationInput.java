package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.util.function.Consumer;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.money.Values;

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

    /** Reports a failure of the member, or of this object itself if the member is empty. */
    void fail(String member, String code, String message)
    {
        context.fail(field(member), code, message);
    }

    /** Whether the member is present and not null. */
    boolean has(String member)
    {
        return json.has(member) && !json.get(member).isJsonNull();
    }

    /**
     * The string value of the member, or null if it is absent, null or blank;
     * reports a member that is not a string.
     */
    String text(String member)
    {
        if (has(member) && !OperationContext.isText(json.get(member)))
        {
            fail(member, "invalid-type", "The " + member + " must be a string.");
            return null;
        }
        return rawText(member);
    }

    /** Like {@link #text(String)}, and reports <code>required</code> if the member is absent, null or blank. */
    String requiredText(String member, String requiredMessage)
    {
        var text = text(member);
        if (text == null && (!has(member) || OperationContext.isText(json.get(member))))
            fail(member, "required", requiredMessage);
        return text;
    }

    /** The string value of the member, or null if it is absent, null, blank or not a string; reports nothing. */
    String rawText(String member)
    {
        var element = json.get(member);
        if (element == null || !OperationContext.isText(element))
            return null;
        var text = element.getAsString();
        return text.isBlank() ? null : text;
    }

    /**
     * The member as a number, or null after reporting that it is not a number,
     * is not positive if that is asked for, or is absent and a
     * <code>requiredMessage</code> is given.
     */
    BigDecimal number(String member, String requiredMessage, boolean positive)
    {
        var value = rawNumber(member);
        if (value == null)
        {
            if (has(member))
                fail(member, "invalid-type", "The " + member + " must be a number.");
            else if (requiredMessage != null)
                fail(member, "required", requiredMessage);
            return null;
        }

        if (positive && value.signum() <= 0)
        {
            fail(member, "not-positive", "The " + member + " must be positive.");
            return null;
        }
        return value;
    }

    /**
     * Like {@link #number(String, String, boolean)}, read as the stored minor
     * units of the given values, e.g. cents; more decimal places than those
     * values keep are reported.
     */
    Long amount(String member, Values<Long> values, boolean positive, String requiredMessage)
    {
        var value = number(member, requiredMessage, positive);
        if (value == null)
            return null;

        var stored = Decimals.toStored(value, values);
        if (stored.isEmpty())
        {
            fail(member, "invalid-amount", "The " + member + " has more decimal places than are stored.");
            return null;
        }
        return stored.get();
    }

    /** The member as a number, or null if it is absent or not a number; reports nothing. */
    BigDecimal rawNumber(String member)
    {
        var element = json.get(member);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                        ? element.getAsBigDecimal()
                        : null;
    }

    /**
     * The existing entity whose UUID is the member, or null after reporting
     * that the member is missing, is not a string or names no such entity.
     */
    <T> T existing(String member, EntityKind<T> kind, String requiredMessage)
    {
        var uuid = requiredText(member, requiredMessage);
        return uuid == null ? null : find(member, uuid, kind);
    }

    /** The existing entity with the UUID, which the member gave, or null after reporting that there is none. */
    <T> T find(String member, String uuid, EntityKind<T> kind)
    {
        var found = kind.find(context, uuid);
        if (found.isEmpty())
            fail(member, "reference-not-found", "No " + kind.noun() + " with UUID " + uuid + " in the file.");
        return found.orElse(null);
    }

    /**
     * Hands a reader of each object in the array member to the consumer, in
     * order, and reports each element that is not an object.
     *
     * @return false if the member is absent or null, or, after reporting it,
     *         not an array
     */
    boolean eachObject(String member, String notObjectMessage, Consumer<OperationInput> each)
    {
        if (!has(member))
            return false;
        if (!json.get(member).isJsonArray())
        {
            fail(member, "invalid-type", "The " + member + " must be an array.");
            return false;
        }

        var array = json.get(member).getAsJsonArray();
        for (var index = 0; index < array.size(); index++)
        {
            var path = field(member) + "[" + index + "]";
            if (array.get(index).isJsonObject())
                each.accept(new OperationInput(context, array.get(index).getAsJsonObject(), path));
            else
                context.fail(path, "invalid-type", notObjectMessage);
        }
        return true;
    }

    /** The field path of the member, or of this object itself if the member is empty. */
    String field(String member)
    {
        if (prefix.isEmpty())
            return member;
        return member.isEmpty() ? prefix : prefix + "." + member;
    }
}
