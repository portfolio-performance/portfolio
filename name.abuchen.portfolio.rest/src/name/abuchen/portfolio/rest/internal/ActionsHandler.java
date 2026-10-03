package name.abuchen.portfolio.rest.internal;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.rest.Messages;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.rest.spi.PriceUpdateTarget;

/**
 * File-level actions that run in the background: the online price update.
 * The update is started through the host (the application's own price update
 * job) and tracked in the {@link JobRegistry}; a client either waits for it
 * ({@code ?wait=<seconds>}) or polls {@code GET /v1/files/{file}/jobs/{id}}.
 */
public final class ActionsHandler
{
    /* package */ static final String KIND_UPDATE_QUOTES = "update-quotes"; //$NON-NLS-1$
    /* package */ static final int MAX_WAIT_SECONDS = 60;

    /**
     * The outcome of starting an action: either the answer of a dry run, or
     * the started job.
     */
    public record Started(JsonObject dryRun, JobRegistry.Job job)
    {
    }

    /** the prices of an instrument before the update, to tell what the update changed */
    private record Prices(SecurityPrice latest, int count, SecurityPrice last)
    {
        static Prices of(Security security)
        {
            var prices = security.getPrices();
            // copies: an update may change a price object in place
            return new Prices(copy(security.getLatest()), prices.size(),
                            prices.isEmpty() ? null : copy(prices.get(prices.size() - 1)));
        }

        private static SecurityPrice copy(SecurityPrice price)
        {
            return price == null ? null : new SecurityPrice(price.getDate(), price.getValue());
        }

        boolean latestDiffers(Prices other)
        {
            return !same(latest, other.latest);
        }

        boolean historicDiffers(Prices other)
        {
            return count != other.count || !same(last, other.last);
        }

        private static boolean same(SecurityPrice a, SecurityPrice b)
        {
            if (a == null || b == null)
                return a == b;
            return Objects.equals(a.getDate(), b.getDate()) && a.getValue() == b.getValue();
        }
    }

    private ActionsHandler()
    {
    }

    /**
     * The {@code wait} query parameter: 0 (the default) to 60 seconds; 400
     * otherwise.
     */
    public static int waitSeconds(String param)
    {
        if (param == null || param.isEmpty())
            return 0;
        try
        {
            var seconds = Integer.parseInt(param);
            if (seconds >= 0 && seconds <= MAX_WAIT_SECONDS)
                return seconds;
        }
        catch (NumberFormatException e)
        {
            // reported below
        }
        throw ApiException.badRequest(List.of(new ApiException.FieldError("wait", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        MessageFormat.format("wait must be a number of seconds from 0 to {0}", MAX_WAIT_SECONDS)))); //$NON-NLS-1$
    }

    /**
     * Starts an online price update of the file's instruments, body
     * {@code {instruments?: [uuid], targets?: ["latest", "historic"]}}; all
     * instruments and both targets by default. A dry run answers what would
     * be updated. Must be called on the UI thread.
     */
    public static Started updateQuotes(WriteContext context, HostApplication host, JobRegistry jobs,
                    JsonObject body)
    {
        var client = context.client();
        var json = new Json(body);

        List<Security> securities = new ArrayList<>(client.getSecurities());
        if (json.has("instruments") && !json.isNull("instruments")) //$NON-NLS-1$ //$NON-NLS-2$
            securities = instruments(json, client.getSecurities(), body.get("instruments")); //$NON-NLS-1$

        Set<PriceUpdateTarget> targets = EnumSet.allOf(PriceUpdateTarget.class);
        if (json.has("targets") && !json.isNull("targets")) //$NON-NLS-1$ //$NON-NLS-2$
            targets = targets(json, body.get("targets")); //$NON-NLS-1$

        json.rejectUnknownFields();
        json.throwIfErrors();

        if (context.dryRun())
        {
            var result = new JsonObject();
            result.addProperty("dryRun", true); //$NON-NLS-1$
            result.add("instruments", instrumentsJson(securities)); //$NON-NLS-1$
            result.add("targets", targetsJson(targets)); //$NON-NLS-1$
            return new Started(result, null);
        }

        var file = context.file();
        var job = jobs.start(KIND_UPDATE_QUOTES, file.getPath());

        var before = new IdentityHashMap<Security, Prices>();
        securities.forEach(security -> before.put(security, Prices.of(security)));

        try
        {
            host.startPriceUpdate(file, List.copyOf(securities), Set.copyOf(targets),
                            status -> finish(host, jobs, job, file, before, status));
        }
        catch (RuntimeException e)
        {
            jobs.failed(job, null, e.getMessage());
            throw e;
        }

        return new Started(null, job);
    }

    /** records the outcome of the update; called once, on any thread */
    private static void finish(HostApplication host, JobRegistry jobs, JobRegistry.Job job, OpenFile file,
                    Map<Security, Prices> before, IStatus status)
    {
        JsonObject summary = null;
        try
        {
            summary = host.syncExec(() -> summary(file, before));
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
        }

        var ok = status == null || status.getSeverity() != IStatus.ERROR && status.getSeverity() != IStatus.CANCEL;
        if (ok)
            jobs.done(job, summary);
        else
            jobs.failed(job, summary, status.getSeverity() == IStatus.CANCEL ? "cancelled" : status.getMessage()); //$NON-NLS-1$

        if (summary != null)
            ChangeLog.recordEvent(Messages.MsgApiPricesUpdated, file.getLabel(), summary.get("instruments").getAsInt(), //$NON-NLS-1$
                            summary.get("latestUpdated").getAsInt(), summary.get("historicUpdated").getAsInt()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** what the update changed, compared with the snapshot taken when it started; on the UI thread */
    private static JsonObject summary(OpenFile file, Map<Security, Prices> before)
    {
        var updated = new JsonArray();
        var latestUpdated = 0;
        var historicUpdated = 0;

        var securities = new ArrayList<>(before.keySet());
        securities.sort((a, b) -> String.valueOf(a.getName()).compareTo(String.valueOf(b.getName())));

        for (var security : securities)
        {
            var old = before.get(security);
            var now = Prices.of(security);
            var latest = old.latestDiffers(now);
            var historic = old.historicDiffers(now);
            if (!latest && !historic)
                continue;

            if (latest)
                latestUpdated++;
            if (historic)
                historicUpdated++;

            var item = new JsonObject();
            item.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
            item.addProperty("name", security.getName()); //$NON-NLS-1$
            item.addProperty("latest", latest); //$NON-NLS-1$
            item.addProperty("historicPricesAdded", now.count() - old.count()); //$NON-NLS-1$
            if (now.latest() != null)
                item.addProperty("latestDate", now.latest().getDate().toString()); //$NON-NLS-1$
            updated.add(item);
        }

        var json = new JsonObject();
        json.addProperty("instruments", before.size()); //$NON-NLS-1$
        json.addProperty("latestUpdated", latestUpdated); //$NON-NLS-1$
        json.addProperty("historicUpdated", historicUpdated); //$NON-NLS-1$
        json.add("updated", updated); //$NON-NLS-1$
        json.addProperty("dirty", file.isDirty()); //$NON-NLS-1$
        return json;
    }

    /**
     * The answer to a started action: the dry run, or the job - after waiting
     * up to {@code waitSeconds} for it on the calling (HTTP worker) thread:
     * {@code 200} if it finished, else {@code 202} with the job's
     * {@code Location}. Must not be called on the UI thread.
     */
    public static Response answer(Started started, int waitSeconds, String jobsPath)
    {
        if (started.dryRun() != null)
            return Response.json(200, started.dryRun());

        var job = started.job();
        if (waitSeconds > 0)
            job.await(Duration.ofSeconds(waitSeconds));

        var json = job.toJson();
        if (job.state() != JobRegistry.State.RUNNING)
            return Response.json(200, json);

        return new Response(202, "application/json", json.toString().getBytes(StandardCharsets.UTF_8), //$NON-NLS-1$
                        Map.of("Location", jobsPath + job.id())); //$NON-NLS-1$
    }

    /** the job with the given id, started for the file; 404 if there is none. */
    public static JsonElement job(JobRegistry jobs, OpenFile file, String id)
    {
        return jobs.find(file.getPath(), id).orElseThrow(ApiException::notFound).toJson();
    }

    private static List<Security> instruments(Json json, List<Security> all, JsonElement element)
    {
        var result = new ArrayList<Security>();
        if (!element.isJsonArray())
        {
            json.add(new ApiException.FieldError("instruments", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                            "instruments must be an array of instrument UUIDs")); //$NON-NLS-1$
            return result;
        }

        var array = element.getAsJsonArray();
        if (array.isEmpty())
            json.add(new ApiException.FieldError("instruments", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "instruments must not be empty; omit it to update all instruments")); //$NON-NLS-1$

        for (var ii = 0; ii < array.size(); ii++)
        {
            var field = "instruments[" + ii + "]"; //$NON-NLS-1$ //$NON-NLS-2$
            var item = array.get(ii);
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString())
            {
                json.add(new ApiException.FieldError(field, "invalid-type", "an instrument UUID must be a string")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            var uuid = item.getAsString();
            var security = all.stream().filter(s -> s.getUUID().equals(uuid)).findFirst();
            if (security.isEmpty())
                json.add(new ApiException.FieldError(field, "unknown-reference", //$NON-NLS-1$
                                MessageFormat.format("{0} is not an instrument of the file", uuid))); //$NON-NLS-1$
            else if (!result.contains(security.get()))
                result.add(security.get());
        }
        return result;
    }

    private static Set<PriceUpdateTarget> targets(Json json, JsonElement element)
    {
        var result = EnumSet.noneOf(PriceUpdateTarget.class);
        if (!element.isJsonArray() || element.getAsJsonArray().isEmpty())
        {
            json.add(new ApiException.FieldError("targets", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "targets must be a non-empty array of latest and historic")); //$NON-NLS-1$
            return result;
        }

        for (var item : element.getAsJsonArray())
        {
            var value = item.isJsonPrimitive() ? item.getAsString() : String.valueOf(item);
            switch (value)
            {
                case "latest" -> result.add(PriceUpdateTarget.LATEST); //$NON-NLS-1$
                case "historic" -> result.add(PriceUpdateTarget.HISTORIC); //$NON-NLS-1$
                default -> json.add(new ApiException.FieldError("targets", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                MessageFormat.format("{0} is not a target; use latest or historic", value))); //$NON-NLS-1$
            }
        }
        return result;
    }

    private static JsonArray instrumentsJson(List<Security> securities)
    {
        var array = new JsonArray();
        for (var security : securities)
        {
            var item = new JsonObject();
            item.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
            item.addProperty("name", security.getName()); //$NON-NLS-1$
            array.add(item);
        }
        return array;
    }

    private static JsonArray targetsJson(Set<PriceUpdateTarget> targets)
    {
        var array = new JsonArray();
        if (targets.contains(PriceUpdateTarget.LATEST))
            array.add("latest"); //$NON-NLS-1$
        if (targets.contains(PriceUpdateTarget.HISTORIC))
            array.add("historic"); //$NON-NLS-1$
        return array;
    }
}
