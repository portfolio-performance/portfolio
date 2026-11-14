package name.abuchen.portfolio.rest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.WriteConfirmation;

/**
 * The one place a change asks the user before it is made.
 *
 * Every write route comes through here, and the waiting happens on the HTTP
 * worker thread rather than inside the task the UI thread runs: a dialog shown
 * from a task already on the UI thread would hold that thread for as long as the
 * user reads, and the deadline that keeps a caller from hanging would pass while
 * the answer is still open - a change would then be applied that nobody was told
 * about. So the question is asked first, the answer is waited for while holding
 * no thread, and only a yes queues the change.
 *
 * Three things can come back. A yes applies the change. A no refuses it as
 * {@code write-declined} and leaves the file untouched - that answer is not a
 * failure to reach the application, so it is not retryable. And no answer at all,
 * which is the case the code has to be honest about: after {@link #PENDING_TTL}
 * the request is dead, nothing was changed, and the caller is told
 * {@code write-unanswered} with a retry.
 */
public final class WriteApprovals
{
    /** how long the user has to answer before the request is dropped */
    public static final Duration PENDING_TTL = Duration.ofMinutes(2);

    /** what the user answered, or that nothing was answered */
    public enum Decision
    {
        ALLOW_ONCE, ALLOW_SESSION, DECLINED, UNANSWERED
    }

    /** what is about to change, as facts rather than as a sentence */
    public record Facts(String operation, String fileLabel, String subjectType, String subjectName, String targetName,
                    String detail)
    {
    }

    private final class Request implements WriteConfirmation
    {
        private final Facts facts;
        private final Instant expiresAt;
        private final CompletableFuture<Decision> outcome = new CompletableFuture<>();

        private Request(Facts facts, Instant expiresAt)
        {
            this.facts = facts;
            this.expiresAt = expiresAt;
        }

        @Override
        public String getOperation()
        {
            return facts.operation();
        }

        @Override
        public String getFileLabel()
        {
            return facts.fileLabel();
        }

        @Override
        public String getSubjectType()
        {
            return facts.subjectType();
        }

        @Override
        public String getSubjectName()
        {
            return facts.subjectName();
        }

        @Override
        public String getTargetName()
        {
            return facts.targetName();
        }

        @Override
        public String getDetail()
        {
            return facts.detail();
        }

        @Override
        public Instant getExpiresAt()
        {
            return expiresAt;
        }

        @Override
        public void allowOnce()
        {
            outcome.complete(Decision.ALLOW_ONCE);
        }

        @Override
        public void allowForSession()
        {
            outcome.complete(Decision.ALLOW_SESSION);
        }

        @Override
        public void decline()
        {
            outcome.complete(Decision.DECLINED);
        }
    }

    private final HostApplication host;
    private final Duration ttl;
    private final Set<String> sessionGranted = new HashSet<>();

    /** the single change awaiting an answer, so that two prompts never stack */
    private Request pending;

    public WriteApprovals(HostApplication host)
    {
        this(host, PENDING_TTL);
    }

    /**
     * A shorter window than {@link #PENDING_TTL}, for the tests that have to sit
     * through one expiring.
     */
    WriteApprovals(HostApplication host, Duration ttl)
    {
        this.host = host;
        this.ttl = ttl;
    }

    /** whether this operation was granted for the rest of the session */
    public synchronized boolean isGrantedForSession(String operation)
    {
        return sessionGranted.contains(operation);
    }

    /** the operation currently awaiting an answer, or null; for the prompt and its tests */
    public synchronized String pendingOperation()
    {
        return pending == null ? null : pending.facts.operation();
    }

    /**
     * Asks, and waits for the answer. Returns once the change may be applied;
     * throws when the user refused, when the request went unanswered, or when
     * another change is already awaiting an answer.
     */
    public void await(Facts facts) throws ApiException
    {
        Request request;
        synchronized (this)
        {
            if (sessionGranted.contains(facts.operation()))
                return;

            if (pending != null)
                throw ApiException.tooManyRequests("write-pending", //$NON-NLS-1$
                                "Another change is awaiting the user's decision", //$NON-NLS-1$
                                Math.max(1, Duration.between(Instant.now(), pending.expiresAt).getSeconds()));

            request = new Request(facts, Instant.now().plus(ttl));
            pending = request;
        }

        host.requestModelWriteApproval(request);

        Decision decision;
        try
        {
            decision = request.outcome.get(ttl.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException | ExecutionException e)
        {
            decision = Decision.UNANSWERED;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            decision = Decision.UNANSWERED;
        }
        finally
        {
            synchronized (this)
            {
                if (pending == request)
                    pending = null;
            }
            // a prompt still open on screen gets an answer it can act on: the
            // request is over, and the window is closed by its own expiry timer
            request.outcome.complete(Decision.UNANSWERED);
        }

        switch (decision)
        {
            case ALLOW_ONCE:
                return;
            case ALLOW_SESSION:
                synchronized (this)
                {
                    sessionGranted.add(facts.operation());
                }
                return;
            case DECLINED:
                throw ApiException.writeDeclined(
                                facts.operation() + " was refused by the user in the application"); //$NON-NLS-1$
            case UNANSWERED:
            default:
                throw ApiException.writeUnanswered(facts.operation());
        }
    }
}
