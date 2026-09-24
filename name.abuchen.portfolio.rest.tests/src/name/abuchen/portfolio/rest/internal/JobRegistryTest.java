package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Test;

import com.google.gson.JsonObject;

@SuppressWarnings("nls")
public class JobRegistryTest
{
    /** a clock the test moves forward */
    private static final class MutableClock extends Clock
    {
        private Instant now = Instant.parse("2026-09-24T10:00:00Z");

        @Override
        public ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return now;
        }

        void advance(Duration duration)
        {
            now = now.plus(duration);
        }
    }

    @Test
    public void testLifecycle()
    {
        var clock = new MutableClock();
        var jobs = new JobRegistry(clock);

        var job = jobs.start("update-quotes", "/a");
        assertThat(job.state(), is(JobRegistry.State.RUNNING));
        assertThat(job.toJson().get("status").getAsString(), is("running"));
        assertThat(job.toJson().has("finishedAt"), is(false));
        assertThat(job.await(Duration.ofMillis(10)), is(false));

        var summary = new JsonObject();
        summary.addProperty("instruments", 3);
        clock.advance(Duration.ofSeconds(5));
        jobs.done(job, summary);

        var json = job.toJson();
        assertThat(json.get("jobId").getAsString(), is(job.id()));
        assertThat(json.get("kind").getAsString(), is("update-quotes"));
        assertThat(json.get("status").getAsString(), is("done"));
        assertThat(json.get("startedAt").getAsString(), is("2026-09-24T10:00:00Z"));
        assertThat(json.get("finishedAt").getAsString(), is("2026-09-24T10:00:05Z"));
        assertThat(json.getAsJsonObject("summary").get("instruments").getAsInt(), is(3));
        assertThat(job.await(Duration.ZERO), is(true));

        // a job finishes only once
        jobs.failed(job, null, "late");
        assertThat(job.toJson().get("status").getAsString(), is("done"));
    }

    @Test
    public void testFailedJobCarriesTheError()
    {
        var jobs = new JobRegistry();
        var job = jobs.start("update-quotes", "/a");
        jobs.failed(job, null, "cancelled");

        assertThat(job.toJson().get("status").getAsString(), is("failed"));
        assertThat(job.toJson().get("error").getAsString(), is("cancelled"));
    }

    @Test
    public void testJobsAreScopedToTheirFile()
    {
        var jobs = new JobRegistry();
        var job = jobs.start("update-quotes", "/a");

        assertThat(jobs.find("/a", job.id()).isPresent(), is(true));
        assertThat(jobs.find("/b", job.id()).isPresent(), is(false));
        assertThat(jobs.find("/a", "unknown").isPresent(), is(false));
    }

    @Test
    public void testFinishedJobsExpire()
    {
        var clock = new MutableClock();
        var jobs = new JobRegistry(clock);

        var finished = jobs.start("update-quotes", "/a");
        var running = jobs.start("update-quotes", "/a");
        jobs.done(finished, null);

        clock.advance(JobRegistry.RETENTION.plusSeconds(1));

        assertThat(jobs.find("/a", finished.id()).isPresent(), is(false));
        // a running job is never forgotten
        assertThat(jobs.find("/a", running.id()).isPresent(), is(true));
    }

    @Test
    public void testAwaitReturnsWhenTheJobFinishesOnAnotherThread() throws Exception
    {
        var jobs = new JobRegistry();
        var job = jobs.start("update-quotes", "/a");

        var thread = new Thread(() -> jobs.done(job, null));
        thread.start();

        assertThat(job.await(Duration.ofSeconds(10)), is(true));
        thread.join();
    }
}
