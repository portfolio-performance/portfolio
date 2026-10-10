package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.rest.Messages;

/**
 * The events of an instrument: stock splits, notes, and the dividend payments
 * a dividend feed reports. Events have no identifier; a delete names them by
 * date, type and optionally details.
 * <p/>
 * Adding a {@code stock-split} event only records the split, as the
 * application's event wizard does - it does not adjust transactions or
 * prices.
 */
public final class SecurityEventsHandler
{
    private static final String KIND = "event"; //$NON-NLS-1$
    private static final Pattern SPLIT_RATIO = Pattern.compile("(\\d+(\\.\\d+)?):(\\d+(\\.\\d+)?)"); //$NON-NLS-1$

    private SecurityEventsHandler()
    {
    }

    /** the wire name of an event type */
    public static String wireType(SecurityEvent.Type type)
    {
        return switch (type)
        {
            case STOCK_SPLIT -> "stock-split"; //$NON-NLS-1$
            case NOTE -> "note"; //$NON-NLS-1$
            case DIVIDEND_PAYMENT -> "dividend-payment"; //$NON-NLS-1$
        };
    }

    private static SecurityEvent.Type parseType(String wireType)
    {
        for (var type : SecurityEvent.Type.values())
        {
            if (wireType(type).equals(wireType))
                return type;
        }
        return null;
    }

    public static JsonElement list(Client client, String uuid)
    {
        var security = SecuritiesHandler.find(client, uuid);
        return EntityJson.envelope(security.getEvents(), EntityJson::toJson);
    }

    /**
     * Adds a {@code stock-split} or {@code note} event. With a
     * {@code clientRef} that an earlier create used, answers that event
     * ({@code replayed: true}). Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult create(WriteContext context, IdempotencyIndex idempotency,
                    String uuid, JsonObject body)
    {
        var client = context.client();
        var security = SecuritiesHandler.find(client, uuid);

        var existing = idempotency.findObject(context.file().getPath(), KIND, context.clientRef())
                        .filter(SecurityEvent.class::isInstance).map(SecurityEvent.class::cast)
                        .filter(e -> security.getEvents().stream().anyMatch(other -> other == e));
        if (existing.isPresent())
            return MasterDataWrites.replayed(EntityJson.toJson(existing.get()), context.dryRun());

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);

        var typeName = json.requireString("type"); //$NON-NLS-1$
        var date = json.requireDate("date"); //$NON-NLS-1$
        var details = json.requireString("details"); //$NON-NLS-1$
        json.rejectUnknownFields();

        SecurityEvent.Type type = null;
        if (typeName != null)
        {
            type = parseType(typeName);
            if (type != SecurityEvent.Type.STOCK_SPLIT && type != SecurityEvent.Type.NOTE)
                json.add(new ApiException.FieldError("type", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                MessageFormat.format("type must be stock-split or note, got {0}", typeName))); //$NON-NLS-1$
        }

        if (type == SecurityEvent.Type.STOCK_SPLIT && details != null)
            validateSplitRatio(json, details);

        json.throwIfErrors();

        var event = new SecurityEvent(date, type, details);

        if (context.dryRun())
            return MasterDataWrites.dryRun(EntityJson.toJson(event));

        security.addEvent(event);
        client.markDirty();
        idempotency.rememberObject(context.file().getPath(), KIND, context.clientRef(), event);

        ChangeLog.recordChanges(List.of(new ChangeLog.Change("events", null, describe(event))), //$NON-NLS-1$
                        Messages.MsgApiInstrumentChanged, security.getName(), context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(event), true);
    }

    /**
     * A split ratio is stored as {@code new:old}, as the split wizard does;
     * both must be positive and differ.
     */
    private static void validateSplitRatio(Json json, String details)
    {
        var matcher = SPLIT_RATIO.matcher(details);
        if (!matcher.matches())
        {
            json.add(new ApiException.FieldError("details", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "the details of a stock split are the ratio new:old, e.g. 2:1")); //$NON-NLS-1$
            return;
        }

        var newShares = new BigDecimal(matcher.group(1));
        var oldShares = new BigDecimal(matcher.group(3));
        if (newShares.signum() <= 0 || oldShares.signum() <= 0 || newShares.compareTo(oldShares) == 0)
            json.add(new ApiException.FieldError("details", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "both sides of the split ratio must be positive and differ")); //$NON-NLS-1$
    }

    /**
     * Removes the events with the given date and type, and - if given - the
     * given details. A dry run answers what would be removed. 404 if nothing
     * matches. Must be called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String uuid, String dateParam, String typeParam,
                    String details)
    {
        var client = context.client();
        var security = SecuritiesHandler.find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        LocalDate date = null;
        if (dateParam == null)
            errors.add(new ApiException.FieldError("date", "required", "date is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        else
        {
            try
            {
                date = LocalDate.parse(dateParam);
            }
            catch (DateTimeParseException e)
            {
                errors.add(new ApiException.FieldError("date", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "date must be an ISO 8601 date (YYYY-MM-DD)")); //$NON-NLS-1$
            }
        }

        SecurityEvent.Type type = null;
        if (typeParam == null)
            errors.add(new ApiException.FieldError("type", "required", "type is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        else if ((type = parseType(typeParam)) == null)
            errors.add(new ApiException.FieldError("type", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("{0} is not an event type", typeParam))); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var wantedDate = date;
        var wantedType = type;
        var matching = security.getEvents().stream() //
                        .filter(e -> Objects.equals(e.getDate(), wantedDate) && e.getType() == wantedType)
                        .filter(e -> details == null || details.equals(e.getDetails())) //
                        .toList();

        if (matching.isEmpty())
            throw ApiException.notFound();

        var removed = new JsonArray();
        matching.forEach(e -> removed.add(EntityJson.toJson(e)));

        var json = new JsonObject();
        if (context.dryRun())
            json.addProperty("dryRun", true); //$NON-NLS-1$
        json.add("removed", removed); //$NON-NLS-1$

        if (context.dryRun())
            return json;

        security.removeEventIf(e -> matching.stream().anyMatch(m -> m == e));
        client.markDirty();

        ChangeLog.recordChanges(matching.stream().map(e -> new ChangeLog.Change("events", describe(e), null)) //$NON-NLS-1$
                        .toList(), Messages.MsgApiInstrumentChanged, security.getName(), context.file().getLabel());

        return json;
    }

    private static String describe(SecurityEvent event)
    {
        return wireType(event.getType()) + " " + event.getDate() //$NON-NLS-1$
                        + (event.getDetails() != null ? " " + event.getDetails() : ""); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
