package name.abuchen.portfolio.rest.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;

/**
 * Finds what an earlier request with the same idempotency key ({@code
 * clientRef}) created, so that a retried create returns the existing object
 * instead of creating a duplicate.
 * <p/>
 * Transactions carry the key durably in their {@code source} field as
 * {@code api:<clientRef>} - it is saved with the file and survives restarts.
 * Master data (instruments, accounts, ...) has no such field; for those the
 * keys are kept in memory for the lifetime of the server only.
 */
public final class IdempotencyIndex
{
    private static final String SOURCE_PREFIX = "api:"; //$NON-NLS-1$

    private record Key(String fileId, String kind, String clientRef)
    {
    }

    private final Map<Key, String> entities = new ConcurrentHashMap<>();
    private final Map<Key, Object> objects = new ConcurrentHashMap<>();

    /** the {@code source} value that marks a transaction created with the key */
    public static String source(String clientRef)
    {
        return SOURCE_PREFIX + clientRef;
    }

    /**
     * The idempotency key a transaction was created with, given its
     * {@code source}; null if it was not created through the API with a key.
     */
    public static String clientRef(String source)
    {
        return source != null && source.startsWith(SOURCE_PREFIX) ? source.substring(SOURCE_PREFIX.length()) : null;
    }

    /**
     * The transaction created with the given key, if any; must be called on
     * the UI thread. Either leg of a buy/sell or transfer carries the key, and
     * {@link Client#getAllTransactions()} reports each such pair once.
     */
    public static Optional<TransactionPair<?>> findTransaction(Client client, String clientRef)
    {
        if (clientRef == null)
            return Optional.empty();

        var source = source(clientRef);
        return client.getAllTransactions().stream() //
                        .filter(pair -> source.equals(pair.getTransaction().getSource())) //
                        .<TransactionPair<?>>map(pair -> pair) //
                        .findFirst();
    }

    /** the UUID of the master data entity of the given kind created with the key, if any */
    public Optional<String> findEntity(String fileId, String kind, String clientRef)
    {
        if (clientRef == null)
            return Optional.empty();
        return Optional.ofNullable(entities.get(new Key(fileId, kind, clientRef)));
    }

    /** remembers that the master data entity with the given UUID was created with the key */
    public void rememberEntity(String fileId, String kind, String clientRef, String uuid)
    {
        if (clientRef != null)
            entities.put(new Key(fileId, kind, clientRef), uuid);
    }

    /**
     * The model object of the given kind created with the key, if any. For
     * entities without a stable identifier (watchlists, investment plans,
     * events), which are addressed by a name the client may change later.
     */
    public Optional<Object> findObject(String fileId, String kind, String clientRef)
    {
        if (clientRef == null)
            return Optional.empty();
        return Optional.ofNullable(objects.get(new Key(fileId, kind, clientRef)));
    }

    /** remembers that the model object was created with the key */
    public void rememberObject(String fileId, String kind, String clientRef, Object entity)
    {
        if (clientRef != null)
            objects.put(new Key(fileId, kind, clientRef), entity);
    }
}
