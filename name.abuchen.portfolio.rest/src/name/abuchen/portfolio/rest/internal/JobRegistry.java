package name.abuchen.portfolio.rest.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

/**
 * The background jobs the API started (online price updates), so that a
 * client can poll a job's state with {@code GET /v1/files/{file}/jobs/{id}}.
 * Kept in memory; a finished job is forgotten one hour after it finished.
 * Thread-safe: jobs are created on the UI thread, finished on a job thread
 * and read on the HTTP worker threads.
 */
public final class JobRegistry
{
    /** how long a finished job can still be polled */
    /* package */ static final Duration RETENTION = Duration.ofHours(1);

    public enum State
    {
        RUNNING, DONE, FAILED;

        /** the wire value: {@code running}, {@code done}, {@code failed} */
        public String wireName()
        {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** one job; its state changes exactly once, from running to done or failed */
    public static final class Job
    {
        private final String id;
        private final String kind;
        private final String filePath;
        private final Instant startedAt;
        private final CountDownLatch finished = new CountDownLatch(1);

        private volatile State state = State.RUNNING;
        private volatile Instant finishedAt;
        private volatile JsonObject summary;
        private volatile String error;

        private Job(String id, String kind, String filePath, Instant startedAt)
        {
            this.id = id;
            this.kind = kind;
            this.filePath = filePath;
            this.startedAt = startedAt;
        }

        public String id()
        {
            return id;
        }

        public State state()
        {
            return state;
        }

        /** waits up to the given time for the job to finish; true if it has */
        public boolean await(Duration timeout)
        {
            try
            {
                return finished.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return state != State.RUNNING;
            }
        }

        /** the job as reported by the API */
        public synchronized JsonObject toJson()
        {
            var json = new JsonObject();
            json.addProperty("jobId", id); //$NON-NLS-1$
            json.addProperty("kind", kind); //$NON-NLS-1$
            json.addProperty("status", state.wireName()); //$NON-NLS-1$
            json.addProperty("startedAt", startedAt.toString()); //$NON-NLS-1$
            if (finishedAt != null)
                json.addProperty("finishedAt", finishedAt.toString()); //$NON-NLS-1$
            if (summary != null)
                json.add("summary", summary.deepCopy()); //$NON-NLS-1$
            if (error != null)
                json.addProperty("error", error); //$NON-NLS-1$
            return json;
        }

        private synchronized boolean finish(State newState, JsonObject summary, String error, Instant now)
        {
            if (state != State.RUNNING)
                return false;
            this.summary = summary;
            this.error = error;
            this.finishedAt = now;
            this.state = newState;
            finished.countDown();
            return true;
        }
    }

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final Clock clock;

    public JobRegistry()
    {
        this(Clock.systemUTC());
    }

    /* package */ JobRegistry(Clock clock)
    {
        this.clock = clock;
    }

    /** registers a new running job of the given kind for the file */
    public Job start(String kind, String filePath)
    {
        expire();
        var job = new Job(UUID.randomUUID().toString(), kind, filePath, clock.instant());
        jobs.put(job.id(), job);
        return job;
    }

    /** marks the job as done; ignored if it already finished */
    public void done(Job job, JsonObject summary)
    {
        job.finish(State.DONE, summary, null, clock.instant());
    }

    /** marks the job as failed; ignored if it already finished */
    public void failed(Job job, JsonObject summary, String error)
    {
        job.finish(State.FAILED, summary, error, clock.instant());
    }

    /** the job with the given id, if it was started for the file and not yet forgotten */
    public Optional<Job> find(String filePath, String id)
    {
        expire();
        return Optional.ofNullable(jobs.get(id)).filter(job -> job.filePath.equals(filePath));
    }

    private void expire()
    {
        var limit = clock.instant().minus(RETENTION);
        jobs.values().removeIf(job -> job.finishedAt != null && job.finishedAt.isBefore(limit));
    }
}
