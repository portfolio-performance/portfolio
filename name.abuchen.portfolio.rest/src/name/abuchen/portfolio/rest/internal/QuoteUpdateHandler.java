package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.rest.WriteApprovals;
import name.abuchen.portfolio.rest.spi.HostApplication;

/**
 * Starting the application's own price update for a part of the file.
 *
 * Nothing is fetched here. The host schedules the job the "Update quotes" menu
 * item schedules - its progress dialog, its providers, its own {@code markDirty()}
 * when a quote arrives. Settled in this class is which securities are handed
 * over, what the caller is told was started, and what the user is asked about
 * before any of it begins.
 *
 * The scope is the whole meaning of the call, so it is resolved, and refused,
 * before anybody is asked about a change.
 */
public final class QuoteUpdateHandler
{
    private static final List<String> SCOPES = List.of("all", "active", "instrument", "watchlist"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private static final List<String> FIELDS = List.of("scope", "instrument", "watchlist"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private QuoteUpdateHandler()
    {
    }

    /** what was started: the answer to hand back, and whether the file can end up dirty */
    public record Started(JsonObject answer, boolean mightChange)
    {}

    /** the securities one call hands to the job, and the words the prompt names them with */
    private record Scope(String name, List<Security> securities, String subjectName)
    {}

    /** what the prompt will name, with an impossible scope refused before anyone is asked */
    public static WriteApprovals.Facts describe(Client client, JsonObject body, String fileLabel)
    {
        var scope = resolve(client, body);
        return new WriteApprovals.Facts(null, fileLabel, "quote-update", scope.subjectName(), null, //$NON-NLS-1$
                        countText(scope.securities().size()));
    }

    /**
     * Schedules the update and answers what was started. The job runs on past
     * this call, so the answer is 202's worth of truth: what is running, over how
     * many instruments, and that none of it reached the disk.
     */
    public static Started run(Client client, JsonObject body, HostApplication host)
    {
        var scope = resolve(client, body);

        if ("all".equals(scope.name())) //$NON-NLS-1$
            host.updateAllQuotes(client);
        else
            host.updateQuotes(client, scope.securities());

        var withFeed = scope.securities().stream() //
                        .filter(QuoteUpdateHandler::canReceiveQuotes) //
                        .count();

        var json = new JsonObject();
        json.addProperty("scope", scope.name()); //$NON-NLS-1$
        json.addProperty("securities", scope.securities().size()); //$NON-NLS-1$
        json.addProperty("withQuoteFeed", withFeed); //$NON-NLS-1$
        json.addProperty("started", true); //$NON-NLS-1$
        json.addProperty("savedToDisk", false); //$NON-NLS-1$

        // "all" runs over the job's whole set of targets, exchange rates and
        // indices along with the securities, so it can change the file whatever
        // the securities themselves carry
        var mightChange = "all".equals(scope.name()) //$NON-NLS-1$
                        || scope.securities().stream().anyMatch(QuoteUpdateHandler::canReceiveQuotes);

        return new Started(json, mightChange);
    }

    /** whether a quote can arrive for this instrument at all: the same test the job makes */
    private static boolean canReceiveQuotes(Security security)
    {
        var feed = Factory.getQuoteFeedProvider(security.getFeed());
        return feed != null && !QuoteFeed.MANUAL.equals(feed.getId());
    }

    private static String countText(int count)
    {
        return count == 1 ? "1 instrument" : count + " instruments"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static Scope resolve(Client client, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        for (var key : body.keySet())
        {
            if (!FIELDS.contains(key))
                errors.add(new ApiException.FieldError(key, "unknown-field", key + " is not part of a price update" //$NON-NLS-1$ //$NON-NLS-2$
                                + " - the call takes scope, instrument and watchlist")); //$NON-NLS-1$
        }

        var scope = "all"; //$NON-NLS-1$
        if (body.has("scope")) //$NON-NLS-1$
        {
            var given = body.get("scope"); //$NON-NLS-1$
            if (!isText(given))
                errors.add(new ApiException.FieldError("scope", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "scope is one of all, active, instrument, watchlist")); //$NON-NLS-1$
            else
                scope = given.getAsString().trim();
        }

        if (!SCOPES.contains(scope))
        {
            var allowed = String.join(", ", SCOPES); //$NON-NLS-1$
            var message = "scope '" + scope + "' is not one of " + allowed; //$NON-NLS-1$ //$NON-NLS-2$
            errors.add(new ApiException.FieldError("scope", "invalid-value", message)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        Scope resolved;
        if ("instrument".equals(scope)) //$NON-NLS-1$
            resolved = oneInstrument(client, body, errors);
        else if ("watchlist".equals(scope)) //$NON-NLS-1$
            resolved = oneWatchlist(client, body, errors);
        else if ("active".equals(scope)) //$NON-NLS-1$
            resolved = new Scope(scope, client.getSecurities().stream().filter(s -> !s.isRetired()).toList(),
                            "active instruments"); //$NON-NLS-1$
        else
            resolved = new Scope(scope, client.getSecurities(), "all instruments of the file"); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        return resolved;
    }

    private static Scope oneInstrument(Client client, JsonObject body, List<ApiException.FieldError> errors)
    {
        var given = body.get("instrument"); //$NON-NLS-1$
        if (given == null || !isText(given) || given.getAsString().isBlank())
        {
            errors.add(new ApiException.FieldError("instrument", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "instrument names the one instrument whose quotes to refresh")); //$NON-NLS-1$
            return null;
        }

        var uuid = given.getAsString().trim();
        var security = SecuritiesHandler.find(client, uuid);
        if (security == null)
        {
            errors.add(new ApiException.FieldError("instrument", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "no instrument with uuid " + uuid + " - see pp_list_instruments")); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        return new Scope("instrument", List.of(security), security.getName()); //$NON-NLS-1$
    }

    private static Scope oneWatchlist(Client client, JsonObject body, List<ApiException.FieldError> errors)
    {
        var given = body.get("watchlist"); //$NON-NLS-1$
        if (given == null || !isText(given) || given.getAsString().isBlank())
        {
            errors.add(new ApiException.FieldError("watchlist", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "watchlist names the list whose instruments to refresh")); //$NON-NLS-1$
            return null;
        }

        var name = given.getAsString().trim();
        for (var watchlist : client.getWatchlists())
        {
            if (watchlist.getName().equals(name))
                return new Scope("watchlist", watchlist.getSecurities(), name); //$NON-NLS-1$
        }

        errors.add(new ApiException.FieldError("watchlist", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "the file has no watchlist '" + name + "' - see pp_list_watchlists")); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    private static boolean isText(JsonElement element)
    {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }
}
