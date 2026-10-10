package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.rest.Messages;

/**
 * Watchlists. The model gives a watchlist no identifier, so the API addresses
 * it by its name, which it keeps unique: a create or rename to a name that is
 * taken is refused.
 */
public final class WatchlistsHandler
{
    private static final String KIND = "watchlist"; //$NON-NLS-1$

    private WatchlistsHandler()
    {
    }

    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getWatchlists(), EntityJson::toJson);
    }

    public static JsonElement get(Client client, String name)
    {
        return EntityJson.toJson(find(client, name));
    }

    /**
     * Creates a watchlist {@code {name, instruments: [uuid...]}}. With a
     * {@code clientRef} that an earlier create used, answers that watchlist
     * ({@code replayed: true}). Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult create(WriteContext context, IdempotencyIndex idempotency,
                    JsonObject body)
    {
        var client = context.client();

        var existing = idempotency.findObject(context.file().getPath(), KIND, context.clientRef())
                        .filter(w -> client.getWatchlists().stream().anyMatch(other -> other == w))
                        .map(Watchlist.class::cast);
        if (existing.isPresent())
            return MasterDataWrites.replayed(EntityJson.toJson(existing.get()), context.dryRun());

        var watchlist = new Watchlist();

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);
        if (!json.has("name")) //$NON-NLS-1$
            json.add(new ApiException.FieldError("name", "required", "name is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        var setters = stage(client, null, json);
        json.throwIfErrors();

        setters.forEach(setter -> setter.accept(watchlist));

        if (context.dryRun())
            return MasterDataWrites.dryRun(EntityJson.toJson(watchlist));

        client.addWatchlist(watchlist);
        client.markDirty();
        idempotency.rememberObject(context.file().getPath(), KIND, context.clientRef(), watchlist);

        ChangeLog.recordEvent(Messages.MsgApiEntityCreated, KIND, watchlist.getName(), context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(watchlist), true);
    }

    /**
     * Applies a JSON Merge Patch: {@code name} renames the watchlist,
     * {@code instruments} replaces its members. A patch that changes nothing
     * does not mark the file dirty. Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult patch(WriteContext context, String name, JsonObject body)
    {
        var client = context.client();
        var watchlist = find(client, name);

        var json = new Json(body);
        var setters = stage(client, watchlist, json);
        json.throwIfErrors();

        return update(context, watchlist, setters);
    }

    /** adds the instrument to the watchlist, unless it already is a member */
    public static MasterDataWrites.WriteResult addInstrument(WriteContext context, String name, String uuid)
    {
        var watchlist = find(context.client(), name);
        var security = SecuritiesHandler.find(context.client(), uuid);

        return update(context, watchlist, List.of(w -> w.addSecurity(security)));
    }

    /** removes the instrument from the watchlist; 404 if it is not a member */
    public static MasterDataWrites.WriteResult removeInstrument(WriteContext context, String name, String uuid)
    {
        var watchlist = find(context.client(), name);
        var security = watchlist.getSecurities().stream().filter(s -> s.getUUID().equals(uuid)).findFirst()
                        .orElseThrow(ApiException::notFound);

        return update(context, watchlist, List.of(w -> w.getSecurities().remove(security)));
    }

    /** previews the change on a copy, then applies it unless it is a dry run or changes nothing */
    private static MasterDataWrites.WriteResult update(WriteContext context, Watchlist watchlist,
                    List<Consumer<Watchlist>> setters)
    {
        var preview = copyOf(watchlist);
        setters.forEach(setter -> setter.accept(preview));

        var before = EntityJson.toJson(watchlist);
        var after = EntityJson.toJson(preview);

        if (context.dryRun())
            return MasterDataWrites.dryRun(after);

        var changes = MasterDataWrites.diff(before, after);
        if (changes.isEmpty())
            return new MasterDataWrites.WriteResult(before, false);

        var oldName = watchlist.getName();
        setters.forEach(setter -> setter.accept(watchlist));
        context.client().markDirty();

        ChangeLog.recordChanges(changes, Messages.MsgApiEntityChanged, KIND, oldName, context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(watchlist), true);
    }

    private static List<Consumer<Watchlist>> stage(Client client, Watchlist current, Json json)
    {
        var setters = new ArrayList<Consumer<Watchlist>>();

        if (json.has("name")) //$NON-NLS-1$
        {
            var name = json.requireString("name"); //$NON-NLS-1$
            if (name != null && client.getWatchlists().stream()
                            .anyMatch(w -> w != current && name.equals(w.getName())))
                json.add(new ApiException.FieldError("name", "already-exists", //$NON-NLS-1$ //$NON-NLS-2$
                                MessageFormat.format("a watchlist named {0} already exists", name))); //$NON-NLS-1$
            else if (name != null)
                setters.add(w -> w.setName(name));
        }

        if (json.has("instruments")) //$NON-NLS-1$
        {
            var securities = parseInstruments(client, json);
            if (securities != null)
                setters.add(w -> {
                    w.getSecurities().clear();
                    w.getSecurities().addAll(securities);
                });
        }

        json.rejectUnknownFields();
        return setters;
    }

    /** the instruments of an array of UUIDs; null (with errors recorded) if any is invalid */
    private static List<Security> parseInstruments(Client client, Json json)
    {
        var element = json.body().get("instruments"); //$NON-NLS-1$
        if (element.isJsonNull())
            return List.of();

        if (!element.isJsonArray())
        {
            json.add(new ApiException.FieldError("instruments", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                            "instruments must be an array of instrument uuids")); //$NON-NLS-1$
            return null; // NOSONAR
        }

        var securities = new ArrayList<Security>();
        var seen = new HashSet<String>();
        var valid = true;

        var array = element.getAsJsonArray();
        for (int ii = 0; ii < array.size(); ii++)
        {
            var field = "instruments[" + ii + "]"; //$NON-NLS-1$ //$NON-NLS-2$
            var item = array.get(ii);
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString())
            {
                json.add(new ApiException.FieldError(field, "invalid-type", "an instrument uuid must be a string")); //$NON-NLS-1$ //$NON-NLS-2$
                valid = false;
                continue;
            }

            var uuid = item.getAsString();
            var security = client.getSecurities().stream().filter(s -> s.getUUID().equals(uuid)).findFirst()
                            .orElse(null);
            if (security == null)
            {
                json.add(new ApiException.FieldError(field, "unknown-reference", //$NON-NLS-1$
                                MessageFormat.format("no instrument with uuid {0}", uuid))); //$NON-NLS-1$
                valid = false;
            }
            else if (!seen.add(uuid))
            {
                json.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                                MessageFormat.format("instrument {0} is listed more than once", uuid))); //$NON-NLS-1$
                valid = false;
            }
            else
            {
                securities.add(security);
            }
        }

        return valid ? securities : null;
    }

    /**
     * Deletes the watchlist; its instruments are not affected. A dry run
     * answers the watchlist that would be removed; a real delete answers
     * null. Must be called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String name)
    {
        var client = context.client();
        var watchlist = find(client, name);

        if (context.dryRun())
            return MasterDataWrites.deletePreview(EntityJson.toJson(watchlist));

        client.removeWatchlist(watchlist);
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiEntityDeleted, KIND, watchlist.getName(), context.file().getLabel());
        return null;
    }

    /**
     * The watchlist with the given name; 404 if there is none, 409
     * {@code ambiguous-name} if the file has several (the application does
     * not enforce unique names).
     */
    /* package */ static Watchlist find(Client client, String name)
    {
        var matches = client.getWatchlists().stream().filter(w -> name.equals(w.getName())).toList();
        if (matches.isEmpty())
            throw ApiException.notFound();
        if (matches.size() > 1)
            throw ApiException.conflict("ambiguous-name", "Several entities have this name", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("{0} watchlists are named {1}; rename them in the application", //$NON-NLS-1$
                                            matches.size(), name),
                            List.of());
        return matches.get(0);
    }

    private static Watchlist copyOf(Watchlist watchlist)
    {
        var copy = new Watchlist();
        copy.setName(watchlist.getName());
        copy.getSecurities().addAll(watchlist.getSecurities());
        return copy;
    }
}
