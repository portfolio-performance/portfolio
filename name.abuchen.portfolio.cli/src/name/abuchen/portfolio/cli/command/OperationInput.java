package name.abuchen.portfolio.cli.command;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

/**
 * Reads the input of one operation, or one object nested in it, and reports
 * every problem as a {@link FieldFailure} into its sink. A nested reader names
 * its fields after the object, such as <code>units[0].amount</code>.
 */
public final class OperationInput
{
    private final JsonObject json;
    private final Client client;
    private final Predicate<Object> existedBeforeBatch;
    private final Consumer<FieldFailure> sink;
    private final String prefix;

    /**
     * @param client
     *            the evolving model, in which UUIDs are looked up
     * @param existedBeforeBatch
     *            whether an object of the model was in the file before the
     *            batch started; only such an entity can be named
     * @param sink
     *            where the failures go
     */
    public OperationInput(JsonObject json, Client client, Predicate<Object> existedBeforeBatch,
                    Consumer<FieldFailure> sink)
    {
        this(json, client, existedBeforeBatch, sink, "");
    }

    private OperationInput(JsonObject json, Client client, Predicate<Object> existedBeforeBatch,
                    Consumer<FieldFailure> sink, String prefix)
    {
        this.json = json;
        this.client = client;
        this.existedBeforeBatch = existedBeforeBatch;
        this.sink = sink;
        this.prefix = prefix;
    }

    /** Whether the JSON value is a string. */
    public static boolean isText(JsonElement element)
    {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    public JsonObject json()
    {
        return json;
    }

    /** Reports every member that the schema of this object does not declare. */
    public void rejectUnknownFields(Schema schema)
    {
        for (var member : json.keySet())
        {
            if (!schema.declares(member))
                fail(member, ErrorCode.UNKNOWN_FIELD, (prefix.isEmpty() ? "The operation" : "The object")
                                + " has no field '" + member + "'.");
        }
    }

    /** Reports a failure of the member, or of this object itself if the member is empty. */
    public void fail(String member, ErrorCode code, String message)
    {
        sink.accept(new FieldFailure(field(member), code, message, null));
    }

    /** Whether the member is present and not null. */
    public boolean has(String member)
    {
        return json.has(member) && !json.get(member).isJsonNull();
    }

    /**
     * The string value of the member, or null if it is absent, null or blank;
     * reports a member that is not a string.
     */
    public String text(String member)
    {
        if (has(member) && !isText(json.get(member)))
        {
            fail(member, ErrorCode.INVALID_TYPE, "The " + member + " must be a string.");
            return null;
        }
        return rawText(member);
    }

    /** Like {@link #text(String)}, and reports <code>required</code> if the member is absent, null or blank. */
    public String requiredText(String member, String requiredMessage)
    {
        var text = text(member);
        if (text == null && (!has(member) || isText(json.get(member))))
            fail(member, ErrorCode.REQUIRED, requiredMessage);
        return text;
    }

    /** The string value of the member, or null if it is absent, null, blank or not a string; reports nothing. */
    public String rawText(String member)
    {
        var element = json.get(member);
        if (element == null || !isText(element))
            return null;
        var text = element.getAsString();
        return text.isBlank() ? null : text;
    }

    /**
     * The member as a number, or null after reporting that it is not a number,
     * is not positive if that is asked for, or is absent and a
     * <code>requiredMessage</code> is given.
     */
    public BigDecimal number(String member, String requiredMessage, boolean positive)
    {
        var value = rawNumber(member);
        if (value == null)
        {
            if (has(member))
                fail(member, ErrorCode.INVALID_TYPE, "The " + member + " must be a number.");
            else if (requiredMessage != null)
                fail(member, ErrorCode.REQUIRED, requiredMessage);
            return null;
        }

        if (positive && value.signum() <= 0)
        {
            fail(member, ErrorCode.NOT_POSITIVE, "The " + member + " must be positive.");
            return null;
        }
        return value;
    }

    /**
     * Like {@link #number(String, String, boolean)}, read as the stored minor
     * units of the given values, e.g. cents; more decimal places than those
     * values keep are reported.
     */
    public Long amount(String member, Values<Long> values, boolean positive, String requiredMessage)
    {
        var value = number(member, requiredMessage, positive);
        if (value == null)
            return null;

        var stored = Decimals.toStored(value, values);
        if (stored.isEmpty())
        {
            fail(member, ErrorCode.INVALID_AMOUNT, "The " + member + " has more decimal places than are stored.");
            return null;
        }
        return stored.get();
    }

    /** The member as a number, or null if it is absent or not a number; reports nothing. */
    public BigDecimal rawNumber(String member)
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
    public <T> T existing(String member, EntityKind<T> kind, String requiredMessage)
    {
        var uuid = requiredText(member, requiredMessage);
        return uuid == null ? null : find(member, uuid, kind);
    }

    /** The existing entity with the UUID, which the member gave, or null after reporting that there is none. */
    public <T> T find(String member, String uuid, EntityKind<T> kind)
    {
        var found = kind.find(client, existedBeforeBatch, uuid);
        if (found.isEmpty())
            fail(member, ErrorCode.REFERENCE_NOT_FOUND, "No " + kind.noun() + " with UUID " + uuid + " in the file.");
        return found.orElse(null);
    }

    /**
     * Hands a reader of each object in the array member to the consumer, in
     * order, and reports each element that is not an object.
     *
     * @return false if the member is absent or null, or, after reporting it,
     *         not an array
     */
    public boolean eachObject(String member, String notObjectMessage, Consumer<OperationInput> each)
    {
        if (!has(member))
            return false;
        if (!json.get(member).isJsonArray())
        {
            fail(member, ErrorCode.INVALID_TYPE, "The " + member + " must be an array.");
            return false;
        }

        var array = json.get(member).getAsJsonArray();
        for (var index = 0; index < array.size(); index++)
        {
            var path = field(member) + "[" + index + "]";
            if (array.get(index).isJsonObject())
                each.accept(new OperationInput(array.get(index).getAsJsonObject(), client, existedBeforeBatch, sink,
                                path));
            else
                sink.accept(new FieldFailure(path, ErrorCode.INVALID_TYPE, notObjectMessage, null));
        }
        return true;
    }

    /** The field path of the member, or of this object itself if the member is empty. */
    public String field(String member)
    {
        if (prefix.isEmpty())
            return member;
        return member.isEmpty() ? prefix : prefix + "." + member;
    }
}
