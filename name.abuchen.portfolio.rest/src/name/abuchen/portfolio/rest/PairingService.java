package name.abuchen.portfolio.rest;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.spi.ApiAccessRequest;
import name.abuchen.portfolio.rest.spi.HostApplication;

/**
 * Interactive pairing: a local program posts an access request, the user
 * approves or declines it in the application, and the program polls for the
 * outcome. At most one request is pending at any time; a decline starts a
 * cool-down against prompt spam. The minted token is delivered exactly once -
 * afterwards the request id is gone.
 */
public class PairingService
{
    public enum PairingStatus
    {
        PENDING, APPROVED, DENIED, EXPIRED;

        public String toJsonValue()
        {
            // Locale.ROOT: the wire value is a fixed contract, not localized text
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** token is non-null only while status is APPROVED */
    public record PollResult(PairingStatus status, String token)
    {
    }

    public static final Duration PENDING_TTL = Duration.ofMinutes(2);
    public static final Duration DECLINE_COOLDOWN = Duration.ofSeconds(60);

    /**
     * How long a decided request stays pollable, and how many at once. The token
     * is delivered exactly once and the id is then gone, so an outcome nothing
     * polls is not history - bounded rather than growing per pairing.
     */
    static final Duration OUTCOME_TTL = Duration.ofMinutes(10);
    static final int MAX_OUTCOMES = 100;

    private final class AccessRequest implements ApiAccessRequest
    {
        private final String id;
        private final String clientName;
        private final Instant expiresAt;

        private AccessRequest(String id, String clientName, Instant expiresAt)
        {
            this.id = id;
            this.clientName = clientName;
            this.expiresAt = expiresAt;
        }

        @Override
        public String getClientName()
        {
            return clientName;
        }

        @Override
        public Instant getExpiresAt()
        {
            return expiresAt;
        }

        @Override
        public void allowForSession()
        {
            decide(id, PairingStatus.APPROVED, true);
        }

        @Override
        public void allowAlways()
        {
            decide(id, PairingStatus.APPROVED, false);
        }

        @Override
        public void decline()
        {
            decide(id, PairingStatus.DENIED, false);
        }
    }

    private final ClientStore clientStore;
    private final HostApplication host;
    private final Supplier<Instant> clock;

    /** the single in-flight request, or null; it is also the SPI handle to the user */
    private AccessRequest pending;

    /**
     * A decision, kept until it is polled or pruned. The token is minted when it
     * is delivered, not when the user approves, so a request the client never
     * polls leaves no client in the store holding a token nobody has.
     */
    private record Outcome(PairingStatus status, String clientName, boolean session, Instant expiresAt)
    {
    }

    private final Map<String, Outcome> outcomes = new LinkedHashMap<>();
    private Instant cooldownUntil;

    public PairingService(ClientStore clientStore, HostApplication host, Supplier<Instant> clock)
    {
        this.clientStore = clientStore;
        this.host = host;
        this.clock = clock;
    }

    public PairingService(ClientStore clientStore, HostApplication host)
    {
        this(clientStore, host, Instant::now);
    }

    /** files a new access request and prompts the user; returns the request id */
    public synchronized String create(String rawClientName)
    {
        var clientName = ClientStore.sanitizeName(rawClientName);
        if (clientName.isEmpty())
            throw ApiException.validation(List.of(new ApiException.FieldError("clientName", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "clientName is required"))); //$NON-NLS-1$
        if (clientName.length() > ClientStore.MAX_NAME_LENGTH)
            throw ApiException.validation(List.of(new ApiException.FieldError("clientName", "too-long", //$NON-NLS-1$ //$NON-NLS-2$
                            "clientName must be at most " + ClientStore.MAX_NAME_LENGTH + " characters"))); //$NON-NLS-1$ //$NON-NLS-2$

        var now = clock.get();
        expireIfDue(now);
        prune(now);

        if (cooldownUntil != null && now.isBefore(cooldownUntil))
            throw ApiException.tooManyRequests("pairing-cooldown", //$NON-NLS-1$
                            "A previous request was declined, try again later", //$NON-NLS-1$
                            secondsUntil(now, cooldownUntil));

        if (pending != null)
            throw ApiException.tooManyRequests("pairing-pending", //$NON-NLS-1$
                            "Another pairing request is awaiting the user's decision", //$NON-NLS-1$
                            secondsUntil(now, pending.expiresAt));

        pending = new AccessRequest(UUID.randomUUID().toString(), clientName, now.plus(PENDING_TTL));
        host.requestApiAccessApproval(pending);
        return pending.id;
    }

    /**
     * The state of a request. Terminal states (and the token, on approval) are
     * delivered exactly once; afterwards - and for ids never issued - 404.
     */
    public synchronized PollResult poll(String id)
    {
        var now = clock.get();
        expireIfDue(now);
        prune(now);

        if (pending != null && pending.id.equals(id))
            return new PollResult(PairingStatus.PENDING, null);

        var outcome = outcomes.remove(id);
        if (outcome == null)
            throw ApiException.notFound();

        // minted on delivery, not at approval
        var token = outcome.status() == PairingStatus.APPROVED
                        ? (outcome.session() ? clientStore.addSessionClient(outcome.clientName) //$NON-NLS-1$
                                        : clientStore.addPersistentClient(outcome.clientName)) //$NON-NLS-1$
                        : null;

        return new PollResult(outcome.status(), token);
    }

    private synchronized void decide(String id, PairingStatus decision, boolean session)
    {
        var now = clock.get();
        expireIfDue(now);
        prune(now);

        if (pending == null || !pending.id.equals(id))
        {
            // The user pressed "allow" on a request that was already gone. That
            // is not a failure of theirs, and a silent return would leave them
            // believing a token was issued - so the log says what happened.
            PortfolioLog.warning("The decision on API access request " + id //$NON-NLS-1$
                    + " arrived after the request had expired or been decided; nothing was granted");
            return;
        }

        if (decision == PairingStatus.APPROVED)
        {
            outcomes.put(id, new Outcome(PairingStatus.APPROVED, pending.clientName, session, now.plus(OUTCOME_TTL))); //$NON-NLS-1$
        }
        else
        {
            outcomes.put(id, new Outcome(PairingStatus.DENIED, pending.clientName, false, now.plus(OUTCOME_TTL))); //$NON-NLS-1$
            cooldownUntil = now.plus(DECLINE_COOLDOWN);
        }
        pending = null;
    }

    private void expireIfDue(Instant now)
    {
        if (pending != null && !now.isBefore(pending.expiresAt))
        {
            outcomes.put(pending.id, new Outcome(PairingStatus.EXPIRED, pending.clientName, false, //$NON-NLS-1$
                            now.plus(OUTCOME_TTL)));
            pending = null;
        }
    }

    /** outcomes are kept only while they can still be polled */
    private void prune(Instant now)
    {
        outcomes.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));

        while (outcomes.size() > MAX_OUTCOMES)
            outcomes.remove(outcomes.keySet().iterator().next());
    }

    private static long secondsUntil(Instant now, Instant until)
    {
        return (Duration.between(now, until).toMillis() + 999) / 1000;
    }
}
