package name.abuchen.portfolio.ui.jobs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.HTMLTableQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.RateLimitExceededException;

public final class UpdateQuotesJob extends AbstractClientJob
{
    public enum Target
    {
        LATEST, HISTORIC
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
     * Ensure that the HTMLTableQuoteFeed retrieves quotes from one host
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

    public UpdateQuotesJob(Client client, Set<Target> target)
    {
        this(client, client.getSecurities(), target);
    }

    public UpdateQuotesJob(Client client, Security security)
    {
        this(client, Arrays.asList(security), EnumSet.allOf(Target.class));
    }

    public UpdateQuotesJob(Client client, List<Security> securities, Set<Target> target)
    {
        super(client, Messages.JobLabelUpdateQuotes);

        this.target = target;
        this.securities = new ArrayList<>(securities);
    }

    public UpdateQuotesJob repeatEvery(long milliseconds)
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

        // include historical quotes
        if (target.contains(Target.HISTORIC))
            addHistoricalQuotesJobs(dirtyable, jobs);

        // include latest quotes
        if (target.contains(Target.LATEST))
            addLatestQuotesJobs(dirtyable, jobs);

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

    private void addLatestQuotesJobs(Dirtyable dirtyable, List<Job> jobs)
    {
        for (Security s : securities)
        {
            // if configured, use feed for latest quotes
            // otherwise use the default feed used by historical quotes as well
            String feedId = s.getLatestFeed();
            if (feedId == null)
                feedId = s.getFeed();

            QuoteFeed feed = Factory.getQuoteFeedProvider(feedId);
            if (feed == null)
                continue;

            Job job = createLatestQuoteJob(dirtyable, feed, s);
            jobs.add(job);

            // the HTML download makes request per URL (per security) -> execute
            // as parallel jobs (although the scheduling rule ensures that only
            // one request is made per host at a given time)
            if (HTMLTableQuoteFeed.ID.equals(feedId))
            {
                job.setRule(HostSchedulingRule
                                .createFor(s.getLatestFeedURL() == null ? s.getFeedURL() : s.getLatestFeedURL()));
            }
            else if (feedId.startsWith("YAHOO")) //$NON-NLS-1$
            {
                job.setRule(new HostSchedulingRule("finance.yahoo.com")); //$NON-NLS-1$
            }
        }
    }

    private Job createLatestQuoteJob(Dirtyable dirtyable, QuoteFeed feed, Security security)
    {
        return new Job(feed.getName())
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                try
                {
                    feed.getLatestQuote(security).ifPresent(p -> {
                        if (security.setLatest(p))
                            dirtyable.markDirty();
                    });

                    return Status.OK_STATUS;
                }
                catch (RateLimitExceededException e)
                {
                    schedule(2000);
                    return Status.OK_STATUS;
                }
            }
        };
    }

    private void addHistoricalQuotesJobs(Dirtyable dirtyable, List<Job> jobs)
    {
        // randomize list in case LRU cache size of HTMLTableQuote feed is too
        // small; otherwise entries would be evicted in order
        Collections.shuffle(securities);

        int jobCounter = 0;
        
        for (Security security : securities)
        {
            jobCounter++;
            
            Job job = new Job("#" + jobCounter + " / " + security.getName())  //$NON-NLS-1$//$NON-NLS-2$
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    try
                    {
                        QuoteFeed feed = Factory.getQuoteFeedProvider(security.getFeed());
                        if (feed == null)
                            return Status.OK_STATUS;

                        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

                        if (security.addAllPrices(data.getPrices()))
                            dirtyable.markDirty();

                        if (!data.getErrors().isEmpty())
                            PortfolioPlugin.log(createErrorStatus(security.getName(), data.getErrors()));

                        return Status.OK_STATUS;
                    }
                    catch (RateLimitExceededException e)
                    {
                        schedule(2000);
                        return Status.OK_STATUS;
                    }
                }
            };

            if (HTMLTableQuoteFeed.ID.equals(security.getFeed()))
                job.setRule(HostSchedulingRule.createFor(security.getFeedURL()));

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
