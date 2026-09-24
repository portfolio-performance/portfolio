package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.rest.Messages;
import name.abuchen.portfolio.util.TradeCalendarManager;

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

    public record PatchResult(JsonElement entity, String instrumentName, List<InstrumentChangeLog.Change> changes)
    {}

    /** a validated patch, ready to be applied to the instrument or to a detached copy of it */
    private record Staged(List<Map.Entry<String, JsonElement>> fields, List<AttributePatch.Assignment> attributes,
                    Map<String, String> feedProperties)
    {}

    /** the kind under which created instruments are remembered for idempotent replays */
    private static final String KIND = "instrument"; //$NON-NLS-1$

    /**
     * The writable fields of an instrument. Validating and applying a field are
     * kept side by side so that the two cannot drift apart; a field that is not
     * listed here is not writable. Custom attributes and the quote feed
     * properties are nested objects, patched separately.
     * <p/>
     * The retired flag is deliberately absent: the model calls it "retired"
     * while the UI speaks of activating and deactivating an instrument. The
     * vocabulary must be settled before it becomes part of the API contract - a
     * published field name is hard to take back.
     */
    private static final Map<String, WritableField> WRITABLE_FIELDS = Map.ofEntries( //
                    Map.entry("name", new WritableField(SecuritiesHandler::requireText, //$NON-NLS-1$
                                    (security, value) -> security.setName(value.getAsString()), Security::getName)), //
                    Map.entry("isin", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setIsin(stringOrNull(value)), Security::getIsin)), //
                    Map.entry("wkn", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setWkn(stringOrNull(value)), Security::getWkn)), //
                    Map.entry("tickerSymbol", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setTickerSymbol(stringOrNull(value)),
                                    Security::getTickerSymbol)), //
                    Map.entry("note", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setNote(stringOrNull(value)), Security::getNote)), //
                    Map.entry("currencyCode", new WritableField(SecuritiesHandler::allowCurrencyOrNull, // //$NON-NLS-1$
                                    (security, value) -> security.setCurrencyCode(stringOrNull(value)),
                                    Security::getCurrencyCode)), //
                    Map.entry("targetCurrencyCode", new WritableField(SecuritiesHandler::allowTargetCurrency, //$NON-NLS-1$
                                    (security, value) -> security.setTargetCurrencyCode(stringOrNull(value)),
                                    Security::getTargetCurrencyCode)), //
                    Map.entry("feed", new WritableField(SecuritiesHandler::allowFeedOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setFeed(stringOrNull(value)), Security::getFeed)), //
                    Map.entry("feedUrl", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setFeedURL(stringOrNull(value)),
                                    Security::getFeedURL)), //
                    Map.entry("latestFeed", new WritableField(SecuritiesHandler::allowFeedOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setLatestFeed(stringOrNull(value)),
                                    Security::getLatestFeed)), //
                    Map.entry("latestFeedUrl", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setLatestFeedURL(stringOrNull(value)),
                                    Security::getLatestFeedURL)), //
                    Map.entry("calendar", new WritableField(SecuritiesHandler::allowCalendarOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setCalendar(stringOrNull(value)),
                                    Security::getCalendar)));

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
     * Creates an instrument from the same fields a patch accepts; {@code name}
     * is required, {@code currencyCode} defaults to the file's base currency
     * (an explicit null creates a currency-less instrument such as an index)
     * and {@code feed} to the manual quote feed, as the application's "new
     * instrument" dialogs do. A {@code targetCurrencyCode} makes the
     * instrument an exchange rate. With a {@code clientRef} that an earlier
     * create used, answers that instrument ({@code replayed: true}). Must be
     * called on the UI thread.
     */
    public static MasterDataWrites.WriteResult create(WriteContext context, IdempotencyIndex idempotency,
                    JsonObject body)
    {
        var client = context.client();

        var existing = idempotency.findEntity(context.file().getPath(), KIND, context.clientRef())
                        .flatMap(uuid -> client.getSecurities().stream().filter(s -> uuid.equals(s.getUUID()))
                                        .findFirst());
        if (existing.isPresent())
            return MasterDataWrites.replayed(EntityJson.toJson(client, existing.get()), context.dryRun());

        var security = new Security(null, client.getBaseCurrency());
        security.setFeed(QuoteFeed.MANUAL);

        // an exchange rate is recognized by its target currency; set it
        // first so that the other fields validate against an exchange rate
        var target = body.get("targetCurrencyCode"); //$NON-NLS-1$
        if (target != null && isString(target))
            security.setTargetCurrencyCode(target.getAsString());

        var errors = new ArrayList<ApiException.FieldError>();
        if (!body.has("name")) //$NON-NLS-1$
            errors.add(new ApiException.FieldError("name", "required", "name is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        var staged = stage(client, security, body, Set.of(WriteContext.CLIENT_REF_FIELD), errors);
        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        apply(security, staged);

        if (context.dryRun())
            return MasterDataWrites.dryRun(EntityJson.toJson(client, security));

        client.addSecurity(security);
        client.markDirty();
        idempotency.rememberEntity(context.file().getPath(), KIND, context.clientRef(), security.getUUID());

        ChangeLog.recordEvent(Messages.MsgApiEntityCreated, KIND, security.getName(), context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(client, security), true);
    }

    /**
     * Applies a JSON Merge Patch (RFC 7386) to the security: absent fields stay
     * untouched, null clears optional fields. All violations are collected and
     * reported at once; nothing is applied unless everything validates.
     */
    public static PatchResult patch(Client client, String uuid, JsonObject body)
    {
        return patch(client, uuid, body, false);
    }

    /**
     * See {@link #patch(Client, String, JsonObject)}. The patch is first
     * applied to a detached copy: a dry run answers the copy, a patch that
     * changes nothing answers the unchanged instrument without marking the
     * file dirty. For a dry run, the changes are the ones the patch would
     * make.
     */
    public static PatchResult patch(Client client, String uuid, JsonObject body, boolean dryRun)
    {
        var security = find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        var staged = stage(client, security, body, Set.of(), errors);
        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        var preview = copyOf(security);
        var changes = apply(preview, staged);

        if (dryRun)
        {
            var json = EntityJson.toJson(client, preview);
            json.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
            json.addProperty("dryRun", true); //$NON-NLS-1$
            return new PatchResult(json, preview.getName(), changes);
        }

        // a patch that changes nothing does not mark the file dirty and
        // thereby prompt the user to save a file the API did not touch
        if (changes.isEmpty())
            return new PatchResult(EntityJson.toJson(client, security), security.getName(), List.of());

        apply(security, staged);
        client.markDirty();
        return new PatchResult(EntityJson.toJson(client, security), security.getName(), changes);
    }

    /**
     * Validates every field of the body; violations are added to
     * {@code errors}. Fields in {@code ignore} are skipped (e.g. the
     * {@code clientRef} of a create).
     */
    private static Staged stage(Client client, Security security, JsonObject body, Set<String> ignore,
                    List<ApiException.FieldError> errors)
    {
        var fields = new ArrayList<Map.Entry<String, JsonElement>>();
        List<AttributePatch.Assignment> attributes = List.of();
        var feedProperties = new LinkedHashMap<String, String>();

        for (var entry : body.entrySet())
        {
            if (ignore.contains(entry.getKey()))
                continue;

            if ("attributes".equals(entry.getKey())) //$NON-NLS-1$
            {
                attributes = AttributePatch.stage(client, Security.class, "instruments", security.getAttributes(), //$NON-NLS-1$
                                entry.getValue(), errors);
                continue;
            }

            if ("feedProperties".equals(entry.getKey())) //$NON-NLS-1$
            {
                stageFeedProperties(entry.getValue(), errors, feedProperties);
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
            else
                fields.add(entry);
        }

        return new Staged(fields, attributes, feedProperties);
    }

    /**
     * The nested {@code feedProperties} merge patch: the properties a quote
     * feed reads (e.g. the JSON paths of the generic JSON feed), keyed by
     * property name. A string sets the property, null removes it.
     */
    private static void stageFeedProperties(JsonElement element, List<ApiException.FieldError> errors,
                    Map<String, String> staged)
    {
        if (!element.isJsonObject())
        {
            errors.add(new ApiException.FieldError("feedProperties", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                            "feedProperties must be a JSON object")); //$NON-NLS-1$
            return;
        }

        for (var entry : element.getAsJsonObject().entrySet())
        {
            var value = entry.getValue();
            if (value.isJsonNull())
                staged.put(entry.getKey(), null);
            else if (isString(value))
                staged.put(entry.getKey(), value.getAsString());
            else
                errors.add(new ApiException.FieldError("feedProperties." + entry.getKey(), "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "a feed property must be a string or null")); //$NON-NLS-1$
        }
    }

    /** applies a validated patch and answers the changes it made */
    private static List<InstrumentChangeLog.Change> apply(Security target, Staged staged)
    {
        var changes = new ArrayList<InstrumentChangeLog.Change>();

        for (var entry : staged.fields())
        {
            var field = WRITABLE_FIELDS.get(entry.getKey());
            var before = field.getter().get(target);
            field.setter().set(target, entry.getValue());
            var after = field.getter().get(target);

            if (!Objects.equals(before, after))
                changes.add(new InstrumentChangeLog.Change(entry.getKey(), before, after));
        }

        for (var entry : staged.feedProperties().entrySet())
        {
            var before = target.getPropertyValue(SecurityProperty.Type.FEED, entry.getKey()).orElse(null);
            if (target.setPropertyValue(SecurityProperty.Type.FEED, entry.getKey(), entry.getValue()))
                changes.add(new InstrumentChangeLog.Change("feedProperties." + entry.getKey(), before, //$NON-NLS-1$
                                entry.getValue()));
        }

        for (var change : AttributePatch.apply(target.getAttributes(), staged.attributes()))
            changes.add(new InstrumentChangeLog.Change(change.field(), change.from(), change.to()));

        return changes;
    }

    /** a detached copy of the instrument to preview a patch on; it is never added to the client */
    private static Security copyOf(Security security)
    {
        var copy = security.deepCopy();
        copy.setAttributes(security.getAttributes().copy());
        return copy;
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
     * The target currency defines the currency of an exchange rate. As in the
     * application's instrument dialog, it exists only for exchange rates (an
     * instrument created with a target currency) and cannot be cleared.
     */
    private static ApiException.FieldError allowTargetCurrency(Client client, Security security, String field,
                    JsonElement value)
    {
        if (!security.isExchangeRate())
            return new ApiException.FieldError(field, "not-allowed-for-type", //$NON-NLS-1$
                            "only an exchange rate has a target currency"); //$NON-NLS-1$
        if (value.isJsonNull())
            return new ApiException.FieldError(field, "exchange-rate-requires-currency", //$NON-NLS-1$
                            "an exchange rate must keep its target currency"); //$NON-NLS-1$
        if (!isString(value))
            return new ApiException.FieldError(field, "invalid-type", field + " must be a string"); //$NON-NLS-1$ //$NON-NLS-2$
        if (CurrencyUnit.getInstance(value.getAsString()) == null)
            return new ApiException.FieldError(field, "unknown-currency", value.getAsString() + " is not a known currency"); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    /** a quote feed id the application knows, see {@link Factory#getQuoteFeedProvider(String)} */
    private static ApiException.FieldError allowFeedOrNull(Client client, Security security, String field,
                    JsonElement value)
    {
        if (value.isJsonNull())
            return null;
        if (!isString(value))
            return new ApiException.FieldError(field, "invalid-type", field + " must be a string or null"); //$NON-NLS-1$ //$NON-NLS-2$

        var feed = value.getAsString();
        if (!QuoteFeed.MANUAL.equals(feed) && Factory.getQuoteFeedProvider(feed) == null)
            return new ApiException.FieldError(field, "unknown-feed", feed + " is not a known quote feed"); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    /** a trade calendar code the application knows; null means the default calendar */
    private static ApiException.FieldError allowCalendarOrNull(Client client, Security security, String field,
                    JsonElement value)
    {
        if (value.isJsonNull())
            return null;
        if (!isString(value))
            return new ApiException.FieldError(field, "invalid-type", field + " must be a string or null"); //$NON-NLS-1$ //$NON-NLS-2$
        if (TradeCalendarManager.getInstance(value.getAsString()) == null)
            return new ApiException.FieldError(field, "unknown-calendar", //$NON-NLS-1$
                            value.getAsString() + " is not a known trade calendar"); //$NON-NLS-1$
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
        checkDeletable(client, security);

        client.removeSecurity(security);
        client.markDirty();
        return security.getName();
    }

    /**
     * The answer of a dry-run delete: the instrument that would be removed.
     * Fails exactly like {@link #delete(Client, String)} would.
     */
    public static JsonObject deletePreview(Client client, String uuid)
    {
        var security = find(client, uuid);
        checkDeletable(client, security);
        return MasterDataWrites.deletePreview(EntityJson.toJson(client, security));
    }

    private static void checkDeletable(Client client, Security security)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        if (security.hasTransactions(client))
            errors.add(new ApiException.FieldError("transactions", "referenced", "instrument has transactions")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (client.getPlans().stream().anyMatch(plan -> security.equals(plan.getSecurity())))
            errors.add(new ApiException.FieldError("plans", "referenced", "instrument is used by investment plans")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (!errors.isEmpty())
            throw ApiException.conflict("delete-blocked", "Instrument is referenced and cannot be deleted", null, //$NON-NLS-1$ //$NON-NLS-2$
                            errors);
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
