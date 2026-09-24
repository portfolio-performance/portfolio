package name.abuchen.portfolio.rest.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import name.abuchen.portfolio.datatransfer.Extractor;

/**
 * The extracted items of an import between its preview and its commit, so
 * that the commit applies exactly the objects the client saw. Kept in memory
 * for 15 minutes; a commit consumes the session.
 */
public final class ImportSessionRegistry
{
    /* package */ static final Duration TIME_TO_LIVE = Duration.ofMinutes(15);

    /**
     * One extraction: {@code kind} is {@code pdf} or {@code csv}, and
     * {@code items} are indexed as the preview reported them.
     */
    public record Session(String id, String filePath, String kind, List<Extractor.Item> items, Instant createdAt)
    {
        public Instant expiresAt()
        {
            return createdAt.plus(TIME_TO_LIVE);
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Clock clock;

    public ImportSessionRegistry()
    {
        this(Clock.systemUTC());
    }

    /* package */ ImportSessionRegistry(Clock clock)
    {
        this.clock = clock;
    }

    /** registers the extracted items of the file; {@code kind} is {@code pdf} or {@code csv} */
    public Session create(String filePath, String kind, List<Extractor.Item> items)
    {
        expire();
        var session = new Session(UUID.randomUUID().toString(), filePath, kind, List.copyOf(items), clock.instant());
        sessions.put(session.id(), session);
        return session;
    }

    /** the session with the given id, if it belongs to the file and has not expired or been consumed */
    public Optional<Session> find(String filePath, String id)
    {
        expire();
        return Optional.ofNullable(sessions.get(id)).filter(session -> session.filePath().equals(filePath));
    }

    /** removes the session, e.g. after its commit */
    public void remove(Session session)
    {
        sessions.remove(session.id());
    }

    private void expire()
    {
        var now = clock.instant();
        sessions.values().removeIf(session -> !session.expiresAt().isAfter(now));
    }
}
