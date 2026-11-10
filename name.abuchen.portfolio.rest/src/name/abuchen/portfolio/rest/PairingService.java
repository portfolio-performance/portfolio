package name.abuchen.portfolio.rest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

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
    private final Map<String, PollResult> outcomes = new HashMap<>();
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
        expireIfDue(clock.get());

        if (pending != null && pending.id.equals(id))
            return new PollResult(PairingStatus.PENDING, null);

        var outcome = outcomes.remove(id);
        if (outcome == null)
            throw ApiException.notFound();
        return outcome;
    }

    private synchronized void decide(String id, PairingStatus decision, boolean session)
    {
        var now = clock.get();
        expireIfDue(now);

        if (pending == null || !pending.id.equals(id))
            return; // expired or already decided

        if (decision == PairingStatus.APPROVED)
        {
            var token = session ? clientStore.addSessionClient(pending.clientName)
                            : clientStore.addPersistentClient(pending.clientName);
            outcomes.put(id, new PollResult(PairingStatus.APPROVED, token));
        }
        else
        {
            outcomes.put(id, new PollResult(PairingStatus.DENIED, null));
            cooldownUntil = now.plus(DECLINE_COOLDOWN);
        }
        pending = null;
    }

    private void expireIfDue(Instant now)
    {
        if (pending != null && !now.isBefore(pending.expiresAt))
        {
            outcomes.put(pending.id, new PollResult(PairingStatus.EXPIRED, null));
            pending = null;
        }
    }

    private static long secondsUntil(Instant now, Instant until)
    {
        return (Duration.between(now, until).toMillis() + 999) / 1000;
    }
}
