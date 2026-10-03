package name.abuchen.portfolio.rest.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.FileAccessRegistry.FileAccess;
import name.abuchen.portfolio.rest.Messages;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.rest.spi.MigrationRequiredException;
import name.abuchen.portfolio.rest.spi.PasswordRequiredException;

public class FilesHandler
{
    /** how long opening a file waits for the file to be loaded */
    private static final Duration DEFAULT_OPEN_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_WAIT_FOR_UPDATES_SECONDS = 30;

    /**
     * The outcome of the UI-thread part of opening a file: either the finished
     * response (the file was already open) or the pending load.
     */
    private record Opening(Response response, FileAccess access, CompletableFuture<OpenFile> loading)
    {
    }

    private final FileAccessRegistry registry;
    private final HostApplication host;
    private Duration openTimeout = DEFAULT_OPEN_TIMEOUT;

    public FilesHandler(FileAccessRegistry registry, HostApplication host)
    {
        this.registry = registry;
        this.host = host;
    }

    /** for tests: how long {@link #open} waits for a file to load */
    public void setOpenTimeout(Duration openTimeout)
    {
        this.openTimeout = openTimeout;
    }

    /** must be called on the UI thread */
    public Response list(Request request)
    {
        var items = new JsonArray();

        for (var file : host.listOpenFiles())
            registry.byPath(file.getPath()) //
                            .filter(FileAccess::enabled) //
                            .ifPresent(access -> items.add(EntityJson.toJson(access, file)));

        return Response.json(200, EntityJson.envelope(items));
    }

    /** a single file, like one item of {@link #list}; must be called on the UI thread */
    public static JsonObject get(FileResolver.ResolvedFile resolved)
    {
        return EntityJson.toJson(resolved.access(), resolved.file());
    }

    /**
     * Saves the file at its current path, in its current format; must be
     * called on the UI thread.
     */
    public static JsonObject save(FileResolver.ResolvedFile resolved)
    {
        return save(resolved, true);
    }

    /**
     * Saves the file; {@code settled} tells whether the background updates
     * of the file were done before saving. Must be called on the UI thread.
     */
    public static JsonObject save(FileResolver.ResolvedFile resolved, boolean settled)
    {
        var file = resolved.file();

        try
        {
            file.save();
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
            throw ApiException.saveFailed(e.getMessage());
        }

        PortfolioLog.info(MessageFormat.format(Messages.MsgApiFileSaved, file.getLabel()));

        var json = EntityJson.toJson(resolved.access(), file);
        json.addProperty("savedAt", Instant.now().toString()); //$NON-NLS-1$
        json.addProperty("backgroundUpdatesPending", !settled); //$NON-NLS-1$
        return json;
    }

    /**
     * Parses {@code ?waitForUpdates}: seconds to wait for running background
     * price updates before saving, 0 to 60, default 30.
     */
    public static Duration waitForUpdates(String param)
    {
        if (param == null || param.isEmpty())
            return Duration.ofSeconds(DEFAULT_WAIT_FOR_UPDATES_SECONDS);
        try
        {
            var seconds = Integer.parseInt(param);
            if (seconds >= 0 && seconds <= ActionsHandler.MAX_WAIT_SECONDS)
                return Duration.ofSeconds(seconds);
        }
        catch (NumberFormatException e)
        {
            // reported below
        }
        throw ApiException.badRequest(List.of(new ApiException.FieldError("waitForUpdates", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        MessageFormat.format("waitForUpdates must be a number of seconds from 0 to {0}", //$NON-NLS-1$
                                        ActionsHandler.MAX_WAIT_SECONDS))));
    }

    /**
     * Waits for the background updates of the file; must be called on the
     * HTTP worker thread, never on the UI thread.
     *
     * @return true if no background update is running any more
     */
    public static boolean awaitBackgroundUpdates(OpenFile file, Duration timeout)
    {
        if (timeout.isZero())
            return true;
        try
        {
            return file.awaitBackgroundUpdates(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Opens an API-enabled file in the application. Runs on the HTTP worker
     * thread: the model and the list of open files are only touched inside
     * {@link HostApplication#syncExec}, while waiting for the file to load
     * happens outside of it so the UI thread can do the loading.
     */
    public Response open(String path) throws Exception
    {
        var opening = host.syncExec(() -> startOpening(path));

        if (opening.response() != null)
            return opening.response();

        OpenFile file;
        try
        {
            file = opening.loading().get(openTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e)
        {
            throw ApiException.fileLoading();
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof PasswordRequiredException)
                throw ApiException.passwordRequired();
            if (e.getCause() instanceof MigrationRequiredException)
                throw ApiException.migrationRequired();
            var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw ApiException.openFailed(message);
        }

        var body = host.syncExec(() -> EntityJson.toJson(opening.access(), file));

        PortfolioLog.info(MessageFormat.format(Messages.MsgApiFileOpened, file.getLabel()));

        return new Response(201, "application/json", body.toString().getBytes(StandardCharsets.UTF_8), //$NON-NLS-1$
                        Map.of("Location", "/v1/files/" + opening.access().uuid())); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private Opening startOpening(String path)
    {
        // only a path the user enabled may be opened; anything else is
        // indistinguishable from a file that does not exist
        var access = registry.byPath(path).filter(FileAccess::enabled).orElseThrow(ApiException::notFound);

        var alreadyOpen = findOpen(access.path());
        if (alreadyOpen.isPresent())
            return new Opening(Response.json(200, EntityJson.toJson(access, alreadyOpen.get())), access, null);

        if (host.isUserEditing())
            throw ApiException.locked();

        try
        {
            return new Opening(null, access, host.openFile(Path.of(access.path())));
        }
        catch (PasswordRequiredException e)
        {
            throw ApiException.passwordRequired();
        }
        catch (MigrationRequiredException e)
        {
            throw ApiException.migrationRequired();
        }
        catch (NoSuchFileException | FileNotFoundException e)
        {
            throw ApiException.notFound();
        }
        catch (IOException e)
        {
            throw ApiException.openFailed(e.getMessage());
        }
    }

    private Optional<OpenFile> findOpen(String path)
    {
        return host.listOpenFiles().stream().filter(file -> file.getPath().equals(path)).findFirst();
    }

    /** parses and validates the body of {@code POST /v1/files/open}; returns the path */
    public static String openRequestPath(JsonObject body)
    {
        var element = body.get("path"); //$NON-NLS-1$
        if (element == null || element.isJsonNull() || isBlankString(element))
            throw ApiException.validation(
                            List.of(new ApiException.FieldError("path", "required", "path is required"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            throw ApiException.validation(
                            List.of(new ApiException.FieldError("path", "invalid-type", "path must be a string"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return element.getAsString();
    }

    private static boolean isBlankString(JsonElement element)
    {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && element.getAsString().isBlank();
    }
}
