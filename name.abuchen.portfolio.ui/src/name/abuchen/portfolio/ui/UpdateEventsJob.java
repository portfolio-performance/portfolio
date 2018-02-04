package name.abuchen.portfolio.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.EventFeed;
import name.abuchen.portfolio.online.impl.HTMLTableEventFeed;

public final class UpdateEventsJob extends AbstractClientJob
{
    public enum Target
    {
        HISTORIC
    }

    /**
     * Keeps dirty state of parallel jobs and marks the client file dirty after
     * 5th dirty result. Background: marking the client dirty after every job
     * sends too many update events to the GUI.
     */
    private static class Dirtyable
    {
        private static final int THRESHOLD = 5;

        private final Client client;
        private AtomicInteger counter;

        public Dirtyable(Client client)
        {
            this.client = client;
            this.counter = new AtomicInteger();
        }

        public void markDirty()
        {
            int count = counter.incrementAndGet();
            if (count % THRESHOLD == 0)
                client.markDirty();
        }

        public boolean isDirty()
        {
            return counter.get() % THRESHOLD != 0;
        }
    }

    /**
     * Ensure that the HTMLTableEventFeed retrieves quotes from one host
     * sequentially. #478
     */
    private static class HostSchedulingRule implements ISchedulingRule
    {
        private final String host;

        private HostSchedulingRule(String host)
        {
            this.host = host;
        }

        @Override
        public boolean contains(ISchedulingRule rule)
        {
            return isConflicting(rule);
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule)
        {
            return rule instanceof HostSchedulingRule && ((HostSchedulingRule) rule).host.equals(this.host);
        }

        public static ISchedulingRule createFor(String url)
        {
            try
            {
                final String hostname = new URI(url).getHost();
                return hostname != null ? new HostSchedulingRule(hostname) : null;
            }
            catch (URISyntaxException e) // NOSONAR
            {
                // ignore syntax exception -> quote feed provide will also
                // complain but with a better error message
                return null;
            }
        }

    }

    private final Set<Target> target;
    private final List<Security> securities;
    private long repeatPeriod;

    public UpdateEventsJob(Client client, Set<Target> target)
    {
        this(client, client.getSecurities(), target);
    }

    public UpdateEventsJob(Client client, Security security)
    {
        this(client, Arrays.asList(security), EnumSet.allOf(Target.class));
    }

    public UpdateEventsJob(Client client, List<Security> securities, Set<Target> target)
    {
        super(client, Messages.JobLabelUpdateEvents);

        this.target = target;
        this.securities = new ArrayList<>(securities);
    }

    public UpdateEventsJob repeatEvery(long milliseconds)
    {
        this.repeatPeriod = milliseconds;
        return this;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdating, IProgressMonitor.UNKNOWN);

        Dirtyable dirtyable = new Dirtyable(getClient());
        List<Job> jobs = new ArrayList<>();

        // include latest quotes
        //if (target.contains(Target.LATEST))
        //    addLatestEventsJobs(dirtyable, jobs);

        // include historical events
        if (target.contains(Target.HISTORIC))
            addHistoricalEventsJobs(dirtyable, jobs);

        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

        if (!jobs.isEmpty())
            runJobs(monitor, jobs);

        if (!monitor.isCanceled() && dirtyable.isDirty())
            getClient().markDirty();

        if (repeatPeriod > 0)
            schedule(repeatPeriod);

        return Status.OK_STATUS;
    }

    private void runJobs(IProgressMonitor monitor, List<Job> jobs)
    {
        JobGroup group = new JobGroup(Messages.JobLabelUpdating, 10, jobs.size());
        for (Job job : jobs)
        {
            job.setJobGroup(group);
            job.schedule();
        }

        try
        {
            group.join(0, monitor);
        }
        catch (InterruptedException ignore) // NOSONAR
        {
            // ignore
        }
    }

    private void addLatestEventsJobs(Dirtyable dirtyable, List<Job> jobs)
    {
        Map<EventFeed, List<Security>> feed2securities = new HashMap<>();

        for (Security s : securities)
        {
            // if configured, use feed for latest quotes
            // otherwise use the default feed used by historical quotes as well
            String feedId = s.getLatestFeed();
            if (feedId == null)
                feedId = s.getFeed();

            EventFeed feed = Factory.getEventFeedProvider(feedId);
            if (feed == null)
                continue;

            // the HTML download makes request per URL (per security) -> execute
            // as parallel jobs (although the scheduling rule ensures that only
            // one request is made per host at a given time)
            if (HTMLTableEventFeed.ID.equals(feedId) && s.getEventFeedURL() != null)
            {
                Job job = createLatestEventJob(dirtyable, feed, s);
                job.setRule(HostSchedulingRule
                                .createFor(s.getEventFeedURL()));
                jobs.add(job);
            }
            else
            {
                feed2securities.computeIfAbsent(feed, key -> new ArrayList<>()).add(s);
            }
        }
    }

    private Job createLatestEventJob(Dirtyable dirtyable, EventFeed feed, Security security)
    {
        return new Job(feed.getName())
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                ArrayList<Exception> exceptions = new ArrayList<>();

                if (feed.updateLatest(security, exceptions))
                    dirtyable.markDirty();

                if (!exceptions.isEmpty())
                    PortfolioPlugin.log(createErrorStatus(feed.getName(), exceptions));

                return Status.OK_STATUS;
            }
        };
    }

    private void addHistoricalEventsJobs(Dirtyable dirtyable, List<Job> jobs)
    {
        // randomize list in case LRU cache size of HTMLTableEvent feed is too
        // small; otherwise entries would be evicted in order
        Collections.shuffle(securities);

        for (Security security : securities)
        {
            Job job = new Job(security.getName())
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    EventFeed feed = Factory.getEventFeedProvider(security.getEventFeed());
                    if (feed == null)
                        return Status.OK_STATUS;

                    ArrayList<Exception> exceptions = new ArrayList<>();

                    if (feed.updateHistorical(security, exceptions))
                        dirtyable.markDirty();

                    if (!exceptions.isEmpty())
                        PortfolioPlugin.log(createErrorStatus(security.getName(), exceptions));

                    return Status.OK_STATUS;
                }
            };

            if (HTMLTableEventFeed.ID.equals(security.getEventFeed()))
                job.setRule(HostSchedulingRule.createFor(security.getEventFeedURL()));

            jobs.add(job);
        }
    }

    private IStatus createErrorStatus(String label, List<Exception> exceptions)
    {
        MultiStatus status = new MultiStatus(PortfolioPlugin.PLUGIN_ID, IStatus.ERROR, label, null);
        for (Exception exception : exceptions)
            status.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, exception.getMessage(), exception));
        return status;
    }

}
