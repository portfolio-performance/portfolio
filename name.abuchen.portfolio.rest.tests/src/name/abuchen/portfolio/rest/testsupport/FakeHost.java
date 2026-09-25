package name.abuchen.portfolio.rest.testsupport;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.rest.spi.ApiAccessRequest;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.rest.spi.MigrationRequiredException;
import name.abuchen.portfolio.rest.spi.PasswordRequiredException;
import name.abuchen.portfolio.rest.spi.PriceUpdateTarget;

public class FakeHost implements HostApplication
{
    /**
     * An open file. Like the application's ClientInput, it becomes dirty
     * whenever the client fires a property change (e.g.
     * {@link Client#markDirty()}); {@link #save()} clears the flag.
     */
    public static class FakeOpenFile implements OpenFile
    {
        private final String path;
        private final String label;
        private final Client client;
        private final ExchangeRateProviderFactory factory;

        private boolean dirty = false;
        private int saveCount = 0;
        private IOException saveFailure;

        private boolean backgroundUpdateFinishes = true;
        private Duration awaitedTimeout;
        private BooleanSupplier uiThreadProbe = () -> false;
        private boolean awaitedOnUIThread = false;

        public FakeOpenFile(String path, String label, Client client, ExchangeRateProviderFactory factory)
        {
            this.path = path;
            this.label = label;
            this.client = client;
            this.factory = factory;

            client.addPropertyChangeListener(event -> dirty = true);
        }

        public FakeOpenFile(String path, String label, Client client)
        {
            this(path, label, client, new ExchangeRateProviderFactory(client));
        }

        @Override
        public String getPath()
        {
            return path;
        }

        @Override
        public String getLabel()
        {
            return label;
        }

        @Override
        public Client getClient()
        {
            return client;
        }

        @Override
        public ExchangeRateProviderFactory getExchangeRateProviderFactory()
        {
            return factory;
        }

        @Override
        public boolean isDirty()
        {
            return dirty;
        }

        public void setDirty(boolean dirty)
        {
            this.dirty = dirty;
        }

        @Override
        public void save() throws IOException
        {
            if (saveFailure != null)
                throw saveFailure;

            saveCount++;
            dirty = false;
        }

        @Override
        public boolean awaitBackgroundUpdates(Duration timeout)
        {
            awaitedTimeout = timeout;
            awaitedOnUIThread |= uiThreadProbe.getAsBoolean();
            return backgroundUpdateFinishes;
        }

        /** whether a simulated background update finishes within the timeout */
        public void setBackgroundUpdateFinishes(boolean finishes)
        {
            this.backgroundUpdateFinishes = finishes;
        }

        /** the timeout of the last wait for background updates, null if none */
        public Duration awaitedTimeout()
        {
            return awaitedTimeout;
        }

        /** lets the file detect a wait on the (simulated) UI thread */
        public void setUIThreadProbe(BooleanSupplier probe)
        {
            this.uiThreadProbe = probe;
        }

        /** true if the wait for background updates ran on the UI thread */
        public boolean awaitedOnUIThread()
        {
            return awaitedOnUIThread;
        }

        /** how often the file was saved */
        public int saveCount()
        {
            return saveCount;
        }

        /** lets the next saves fail with the given exception */
        public void failSaveWith(IOException failure)
        {
            this.saveFailure = failure;
        }
    }

    /** how {@link FakeHost#openFile(Path)} behaves for a registered file */
    public enum OpenBehavior
    {
        /** the file is loaded immediately */
        LOADED,
        /** loading never finishes */
        PENDING,
        /** loading fails */
        FAILS,
        /** the file is encrypted and needs a password */
        ENCRYPTED,
        /** the file loads but needs the base currency migration first */
        MIGRATION_REQUIRED
    }

    private record Openable(Client client, OpenBehavior behavior)
    {
    }

    /** a price update the API started; a test completes it with {@link #complete} */
    public static final class PriceUpdate
    {
        private final OpenFile file;
        private final List<Security> securities;
        private final Set<PriceUpdateTarget> targets;
        private final Consumer<IStatus> onDone;

        private PriceUpdate(OpenFile file, List<Security> securities, Set<PriceUpdateTarget> targets,
                        Consumer<IStatus> onDone)
        {
            this.file = file;
            this.securities = securities;
            this.targets = targets;
            this.onDone = onDone;
        }

        public OpenFile file()
        {
            return file;
        }

        public List<Security> securities()
        {
            return securities;
        }

        public Set<PriceUpdateTarget> targets()
        {
            return targets;
        }

        /** finishes the update with the given outcome, e.g. {@link Status#OK_STATUS} */
        public void complete(IStatus status)
        {
            onDone.accept(status);
        }
    }

    private final List<PriceUpdate> priceUpdates = new ArrayList<>();
    private Consumer<PriceUpdate> priceUpdateBehavior = update -> {
    };

    private final List<OpenFile> openFiles;
    private final Map<String, Openable> openable = new HashMap<>();
    private final Set<String> openedPaths = new HashSet<>();
    private boolean userEditing = false;
    private ApiAccessRequest lastAccessRequest;

    private int syncExecDepth = 0;
    private boolean accessedOutsideUIThread = false;
    private final List<Object> syncExecResults = new ArrayList<>();

    public FakeHost(List<OpenFile> openFiles)
    {
        this.openFiles = new ArrayList<>(openFiles);
    }

    public void setUserEditing(boolean userEditing)
    {
        this.userEditing = userEditing;
    }

    /**
     * Makes a file known that is not open yet but can be opened with
     * {@link #openFile(Path)}.
     */
    public void addOpenableFile(String path, Client client, OpenBehavior behavior)
    {
        openable.put(path, new Openable(client, behavior));
    }

    /** the paths {@link #openFile(Path)} was asked to open */
    public Set<String> openedPaths()
    {
        return openedPaths;
    }

    /**
     * Whether the open files were read without going through
     * {@link #syncExec(Callable)}, i.e. off the UI thread in the real
     * application.
     */
    public boolean hasAccessedOutsideUIThread()
    {
        return accessedOutsideUIThread;
    }

    /** true while a {@link #syncExec(Callable)} callable runs */
    public boolean isInSyncExec()
    {
        return syncExecDepth > 0;
    }

    @Override
    public List<OpenFile> listOpenFiles()
    {
        if (syncExecDepth == 0)
            accessedOutsideUIThread = true;

        return List.copyOf(openFiles);
    }

    /**
     * The values the syncExec callables returned - lets a test verify what was
     * (and was not) computed on the UI thread.
     */
    public List<Object> syncExecResults()
    {
        return syncExecResults;
    }

    @Override
    public <T> T syncExec(Callable<T> callable) throws Exception
    {
        syncExecDepth++;

        try
        {
            T result = callable.call();
            syncExecResults.add(result);
            return result;
        }
        finally
        {
            syncExecDepth--;
        }
    }

    @Override
    public boolean isUserEditing()
    {
        return userEditing;
    }

    @Override
    public void requestApiAccessApproval(ApiAccessRequest request)
    {
        this.lastAccessRequest = request;
    }

    /** the most recent access request the service asked the user about */
    public ApiAccessRequest lastAccessRequest()
    {
        return lastAccessRequest;
    }

    /** the price updates the API started, oldest first */
    public List<PriceUpdate> priceUpdates()
    {
        return priceUpdates;
    }

    /**
     * What happens when the API starts a price update, e.g. change some
     * prices and complete it right away; by default the update keeps running
     * until the test completes it.
     */
    public void onPriceUpdate(Consumer<PriceUpdate> behavior)
    {
        this.priceUpdateBehavior = behavior;
    }

    @Override
    public void startPriceUpdate(OpenFile file, List<Security> securities, Set<PriceUpdateTarget> targets,
                    Consumer<IStatus> onDone)
    {
        if (syncExecDepth == 0)
            accessedOutsideUIThread = true;

        var update = new PriceUpdate(file, securities, targets, onDone);
        priceUpdates.add(update);
        priceUpdateBehavior.accept(update);
    }

    @Override
    public CompletableFuture<OpenFile> openFile(Path path) throws IOException
    {
        if (syncExecDepth == 0)
            accessedOutsideUIThread = true;

        var key = path.toString();
        openedPaths.add(key);

        var candidate = openable.get(key);
        if (candidate == null)
            throw new NoSuchFileException(key);

        return switch (candidate.behavior())
        {
            case LOADED -> {
                var file = new FakeOpenFile(key, path.getFileName().toString(), candidate.client());
                openFiles.add(file);
                yield CompletableFuture.completedFuture(file);
            }
            case PENDING -> new CompletableFuture<>();
            case FAILS -> CompletableFuture.failedFuture(new IOException("corrupt file")); //$NON-NLS-1$
            case ENCRYPTED -> throw new PasswordRequiredException(key);
            case MIGRATION_REQUIRED -> CompletableFuture.failedFuture(new MigrationRequiredException(key));
        };
    }
}
