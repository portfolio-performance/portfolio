package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

public final class SecuritiesHandler
{
    /** validates one field of the patch; returns null if the value is acceptable */
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

    private record WritableField(Validator validator, Setter setter)
    {
    }

    /**
     * The writable fields of an instrument. Validating and applying a field are
     * kept side by side so that the two cannot drift apart; a field that is not
     * listed here is not writable.
     * <p/>
     * The retired flag is deliberately absent: the model calls it "retired"
     * while the UI speaks of activating and deactivating an instrument. The
     * vocabulary must be settled before it becomes part of the API contract -
     * a published field name is hard to take back.
     */
    private static final Map<String, WritableField> WRITABLE_FIELDS = Map.of( //
                    "name", new WritableField(SecuritiesHandler::requireText, //$NON-NLS-1$
                                    (security, value) -> security.setName(value.getAsString())), //
                    "isin", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setIsin(stringOrNull(value))), //
                    "wkn", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setWkn(stringOrNull(value))), //
                    "tickerSymbol", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setTickerSymbol(stringOrNull(value))), //
                    "note", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setNote(stringOrNull(value))), //
                    "currencyCode", new WritableField(SecuritiesHandler::requireCurrency, //$NON-NLS-1$
                                    (security, value) -> security.setCurrencyCode(value.getAsString())));

    private SecuritiesHandler()
    {
    }

    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getSecurities(), EntityJson::toJson);
    }

    public static JsonElement get(Client client, String uuid)
    {
        return EntityJson.toJson(find(client, uuid));
    }

    /**
     * Applies a JSON Merge Patch (RFC 7386) to the security: absent fields
     * stay untouched, null clears optional fields. All violations are
     * collected and reported at once; nothing is applied unless everything
     * validates.
     */
    public static JsonElement patch(Client client, String uuid, JsonObject body)
    {
        var security = find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();

        for (var entry : body.entrySet())
        {
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

        // an empty patch changes nothing; do not mark the file dirty and thereby
        // prompt the user to save a file the API did not touch
        if (body.isEmpty())
            return EntityJson.toJson(security);

        for (var entry : body.entrySet())
            WRITABLE_FIELDS.get(entry.getKey()).setter().set(security, entry.getValue());

        client.markDirty();
        return EntityJson.toJson(security);
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

    private static ApiException.FieldError requireCurrency(Client client, Security security, String field,
                    JsonElement value)
    {
        if (value.isJsonNull() || !isString(value))
            return new ApiException.FieldError(field, "required", field + " must be a string"); //$NON-NLS-1$ //$NON-NLS-2$

        var code = value.getAsString();

        if (CurrencyUnit.getInstance(code) == null)
            return new ApiException.FieldError(field, "unknown-currency", code + " is not a known currency"); //$NON-NLS-1$ //$NON-NLS-2$

        if (!code.equals(security.getCurrencyCode()) && security.hasTransactions(client))
            return new ApiException.FieldError(field, "locked-by-transactions", //$NON-NLS-1$
                            "currency cannot be changed while the instrument has transactions"); //$NON-NLS-1$

        return null;
    }

    /**
     * Deletes the security only if it is not referenced by transactions or
     * investment plans; Client#removeSecurity would cascade into deleting
     * transaction history the API client may never have seen.
     */
    public static void delete(Client client, String uuid)
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
}
