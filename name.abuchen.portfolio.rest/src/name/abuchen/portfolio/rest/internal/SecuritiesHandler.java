package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

public final class SecuritiesHandler
{
    /**
     * validates one field of the patch; returns null if the value is acceptable
     */
    @FunctionalInterface
    private interface Validator
    {
        ApiException.FieldError validate(Client client, Security security, String field, JsonElement value);
    }

    @FunctionalInterface
    private interface Setter
    {
        void set(Security security, JsonElement value);
    }

    @FunctionalInterface
    private interface Getter
    {
        String get(Security security);
    }

    private record WritableField(Validator validator, Setter setter, Getter getter)
    {}

    private record AttributeAssignment(AttributeType type, boolean clear, Object value)
    {}

    public record PatchResult(JsonElement entity, String instrumentName, List<InstrumentChangeLog.Change> changes)
    {}

    /**
     * The writable fields of an instrument. Validating and applying a field are
     * kept side by side so that the two cannot drift apart; a field that is not
     * listed here is not writable.
     * <p/>
     * The retired flag is deliberately absent: the model calls it "retired"
     * while the UI speaks of activating and deactivating an instrument. The
     * vocabulary must be settled before it becomes part of the API contract - a
     * published field name is hard to take back.
     */
    private static final Map<String, WritableField> WRITABLE_FIELDS = Map.of( //
                    "name", new WritableField(SecuritiesHandler::requireText, //$NON-NLS-1$
                                    (security, value) -> security.setName(value.getAsString()), Security::getName), //
                    "isin", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setIsin(stringOrNull(value)), Security::getIsin), //
                    "wkn", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setWkn(stringOrNull(value)), Security::getWkn), //
                    "tickerSymbol", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setTickerSymbol(stringOrNull(value)),
                                    Security::getTickerSymbol), //
                    "note", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setNote(stringOrNull(value)), Security::getNote), //
                    "currencyCode", new WritableField(SecuritiesHandler::allowCurrencyOrNull, // //$NON-NLS-1$
                                    (security, value) -> security.setCurrencyCode(stringOrNull(value)),
                                    Security::getCurrencyCode));

    private SecuritiesHandler()
    {
    }

    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getSecurities(), s -> EntityJson.toJson(client, s));
    }

    public static JsonElement get(Client client, String uuid)
    {
        return EntityJson.toJson(client, find(client, uuid));
    }

    /**
     * The attribute-type definitions that apply to an instrument, so a client
     * can resolve an id to its name and value type. Compound types are listed
     * but flagged unsupported; a type whose converter this API does not
     * recognise is omitted entirely, as it is not part of the contract.
     */
    public static JsonElement attributeTypes(Client client)
    {
        var items = new JsonArray();

        client.getSettings().getAttributeTypes() //
                        .filter(type -> type.supports(Security.class)) //
                        .forEach(type -> {
                            var fieldType = AttributeFieldType.of(type);
                            if (fieldType == null)
                                return;

                            var item = new JsonObject();
                            item.addProperty("id", type.getId()); //$NON-NLS-1$
                            item.addProperty("name", type.getName()); //$NON-NLS-1$
                            if (type.getColumnLabel() != null)
                                item.addProperty("columnLabel", type.getColumnLabel()); //$NON-NLS-1$
                            item.addProperty("type", AttributeCodec.wireType(fieldType)); //$NON-NLS-1$
                            item.addProperty("supported", AttributeCodec.isSupported(fieldType)); //$NON-NLS-1$
                            items.add(item);
                        });

        return EntityJson.envelope(items);
    }

    /**
     * Applies a JSON Merge Patch (RFC 7386) to the security: absent fields stay
     * untouched, null clears optional fields. All violations are collected and
     * reported at once; nothing is applied unless everything validates.
     */
    public static PatchResult patch(Client client, String uuid, JsonObject body)
    {
        var security = find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        var assignments = new ArrayList<AttributeAssignment>();

        for (var entry : body.entrySet())
        {
            if ("attributes".equals(entry.getKey())) //$NON-NLS-1$
            {
                stageAttributes(client, security, entry.getValue(), errors, assignments);
                continue;
            }

            var field = WRITABLE_FIELDS.get(entry.getKey());
            if (field == null)
            {
                errors.add(new ApiException.FieldError(entry.getKey(), "unknown-field", "field is not writable")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            var error = field.validator().validate(client, security, entry.getKey(), entry.getValue());
            if (error != null)
                errors.add(error);
        }

        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        // an empty patch changes nothing; do not mark the file dirty and
        // thereby prompt the user to save a file the API did not touch
        var hasWritableField = body.keySet().stream().anyMatch(WRITABLE_FIELDS::containsKey);
        if (!hasWritableField && assignments.isEmpty())
            return new PatchResult(EntityJson.toJson(client, security), security.getName(), List.of());

        var changes = new ArrayList<InstrumentChangeLog.Change>();

        for (var entry : body.entrySet())
        {
            if ("attributes".equals(entry.getKey())) //$NON-NLS-1$
                continue;

            var field = WRITABLE_FIELDS.get(entry.getKey());
            var before = field.getter().get(security);
            field.setter().set(security, entry.getValue());
            var after = field.getter().get(security);

            if (!Objects.equals(before, after))
                changes.add(new InstrumentChangeLog.Change(entry.getKey(), before, after));
        }

        for (var assignment : assignments)
        {
            var type = assignment.type();
            var before = security.getAttributes().get(type);

            if (assignment.clear())
            {
                security.getAttributes().remove(type);
                if (before != null)
                    changes.add(new InstrumentChangeLog.Change(type.getName(), renderAttribute(type, before), null));
            }
            else
            {
                var after = assignment.value();
                security.getAttributes().put(type, after);
                if (!Objects.equals(before, after))
                    changes.add(new InstrumentChangeLog.Change(type.getName(),
                                    before == null ? null : renderAttribute(type, before),
                                    renderAttribute(type, after)));
            }
        }

        client.markDirty();
        return new PatchResult(EntityJson.toJson(client, security), security.getName(), changes);
    }

    /**
     * Validates and stages the nested attributes merge patch: a present key
     * sets the attribute, an explicit null clears it. Every violation is
     * collected; staged assignments are applied only if the whole patch
     * validates.
     */
    private static void stageAttributes(Client client, Security security, JsonElement element,
                    List<ApiException.FieldError> errors, List<AttributeAssignment> assignments)
    {
        if (!element.isJsonObject())
        {
            errors.add(new ApiException.FieldError("attributes", "invalid-type", "attributes must be a JSON object")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return;
        }

        for (var entry : element.getAsJsonObject().entrySet())
        {
            var id = entry.getKey();
            var field = "attributes." + id; //$NON-NLS-1$

            var type = client.getSettings().getAttributeTypes().filter(a -> id.equals(a.getId())).findAny()
                            .orElse(null);
            if (type == null)
            {
                errors.add(new ApiException.FieldError(field, "unknown-attribute", "no such attribute")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }
            if (!type.supports(Security.class))
            {
                errors.add(new ApiException.FieldError(field, "attribute-not-applicable", //$NON-NLS-1$
                                "attribute does not apply to instruments")); //$NON-NLS-1$
                continue;
            }

            var fieldType = AttributeFieldType.of(type);
            if (fieldType == null || !AttributeCodec.isSupported(fieldType))
            {
                errors.add(new ApiException.FieldError(field, "unsupported-attribute-type", //$NON-NLS-1$
                                "attribute type is not supported by the API")); //$NON-NLS-1$
                continue;
            }

            var value = entry.getValue();
            if (value.isJsonNull())
            {
                if (security.getAttributes().exists(type))
                    assignments.add(new AttributeAssignment(type, true, null));
                continue;
            }

            try
            {
                assignments.add(new AttributeAssignment(type, false, AttributeCodec.decode(fieldType, value)));
            }
            catch (AttributeCodec.InvalidValueException e)
            {
                errors.add(new ApiException.FieldError(field, "invalid-value", e.getMessage())); //$NON-NLS-1$
            }
        }
    }

    private static ApiException.FieldError requireText(Client client, Security security, String field,
                    JsonElement value)
    {
        if (value.isJsonNull() || !isString(value) || value.getAsString().isBlank())
            return new ApiException.FieldError(field, "required", field + " must be a non-empty string"); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    private static ApiException.FieldError allowTextOrNull(Client client, Security security, String field,
                    JsonElement value)
    {
        if (!value.isJsonNull() && !isString(value))
            return new ApiException.FieldError(field, "invalid-type", field + " must be a string or null"); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    /**
     * Validates a currency change, mirroring the master-data page's rules.
     * Clearing the currency (null) marks the instrument as an index; it is
     * refused for an exchange rate - which needs a currency - and, like any
     * currency change, while the instrument has transactions.
     */
    private static ApiException.FieldError allowCurrencyOrNull(Client client, Security security, String field,
                    JsonElement value)
    {
        String code;

        if (value.isJsonNull())
        {
            if (security.isExchangeRate())
                return new ApiException.FieldError(field, "exchange-rate-requires-currency", //$NON-NLS-1$
                                "an exchange rate must keep its currency and cannot be cleared"); //$NON-NLS-1$
            code = null;
        }
        else if (isString(value))
        {
            code = value.getAsString();
            if (CurrencyUnit.getInstance(code) == null)
                return new ApiException.FieldError(field, "unknown-currency", code + " is not a known currency"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            return new ApiException.FieldError(field, "invalid-type", field + " must be a string or null"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (!Objects.equals(code, security.getCurrencyCode()) && security.hasTransactions(client))
            return new ApiException.FieldError(field, "locked-by-transactions", //$NON-NLS-1$
                            "currency cannot be changed while the instrument has transactions"); //$NON-NLS-1$

        return null;
    }

    /**
     * Deletes the security only if it is not referenced by transactions or
     * investment plans; Client#removeSecurity would cascade into deleting
     * transaction history the API client may never have seen.
     */
    public static String delete(Client client, String uuid)
    {
        var security = find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        if (security.hasTransactions(client))
            errors.add(new ApiException.FieldError("transactions", "referenced", "instrument has transactions")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (client.getPlans().stream().anyMatch(plan -> security.equals(plan.getSecurity())))
            errors.add(new ApiException.FieldError("plans", "referenced", "instrument is used by investment plans")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (!errors.isEmpty())
            throw ApiException.conflict("delete-blocked", "Instrument is referenced and cannot be deleted", null, //$NON-NLS-1$ //$NON-NLS-2$
                            errors);

        client.removeSecurity(security);
        return security.getName();
    }

    /* package */ static Security find(Client client, String uuid)
    {
        return Entities.byUuid(client.getSecurities(), Security::getUUID, uuid);
    }

    private static boolean isString(JsonElement value)
    {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    private static String stringOrNull(JsonElement value)
    {
        return value.isJsonNull() ? null : value.getAsString();
    }

    /**
     * Renders a stored attribute value the way the desktop UI shows it. Guarded
     * against a poisoned stored value (wrong runtime type from a corrupted
     * file) so that logging can never break a write.
     */
    private static String renderAttribute(AttributeType type, Object value)
    {
        try
        {
            return type.getConverter().toString(value);
        }
        catch (RuntimeException e)
        {
            return String.valueOf(value);
        }
    }
}
