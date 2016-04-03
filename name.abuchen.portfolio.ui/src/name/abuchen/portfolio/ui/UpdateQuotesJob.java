package name.abuchen.portfolio.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import name.abuchen.portfolio.online.impl.HTMLTableQuoteFeed;

public final class UpdateQuotesJob extends AbstractClientJob
{
    public enum Target
    {
        LATEST, HISTORIC
    }

    private static class JobSpec
    {
        private final String feedId;
        private final List<Security> securities;
        private ISchedulingRule rule;

        public JobSpec(String feedId, List<Security> securities)
        {
            this.feedId = feedId;
            this.securities = securities;
        }

        public JobSpec(String feedId)
        {
            this(feedId, new ArrayList<>());
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
            catch (URISyntaxException e)
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
        this.securities = new ArrayList<Security>(securities);
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

        // update latest quotes
        if (target.contains(Target.LATEST))
            doUpdateLatestQuotes(monitor);

        // update historical quotes
        if (target.contains(Target.HISTORIC))
            doUpdateHistoricalQuotes(monitor);

        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

        if (repeatPeriod > 0)
            schedule(repeatPeriod);

        return Status.OK_STATUS;
    }

    private void doUpdateLatestQuotes(IProgressMonitor monitor)
    {
        List<JobSpec> specs = collectJobSpecs();

        JobGroup jobGroup = new JobGroup(Messages.JobLabelUpdating, 10, specs.size());

        boolean[] isDirty = new boolean[1];

        for (JobSpec spec : specs)
        {
            QuoteFeed feed = Factory.getQuoteFeedProvider(spec.feedId);
            if (feed == null)
                continue;

            if (monitor.isCanceled())
                return;

            Job job = new Job(feed.getName())
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    ArrayList<Exception> exceptions = new ArrayList<>();

                    if (feed.updateLatestQuotes(spec.securities, exceptions))
                        isDirty[0] = true;

                    if (!exceptions.isEmpty())
                        PortfolioPlugin.log(createErrorStatus(feed.getName(), exceptions));

                    return Status.OK_STATUS;
                }
            };

            job.setRule(spec.rule);
            job.setJobGroup(jobGroup);
            job.schedule();
        }

        try
        {
            jobGroup.join(10000, monitor);
        }
        catch (InterruptedException ignore)
        {
            // ignore
        }

        // if the job is cancelled, do not mark the client dirty as it would
        // trigger the creation of a new Display
        if (isDirty[0] && !monitor.isCanceled())
            getClient().markDirty();
    }

    private List<JobSpec> collectJobSpecs()
    {
        List<JobSpec> specs = new ArrayList<>();

        Map<String, JobSpec> feed2securities = new HashMap<>();
        for (Security s : securities)
        {
            // if configured, use feed for latest quotes
            // otherwise use the default feed used by historical quotes as well
            String feedId = s.getLatestFeed();
            if (feedId == null)
                feedId = s.getFeed();

            // the HTML download makes request per URL (per security) -> execute
            // as parallel jobs (although the scheduling rule ensures that only
            // one request is made per host at a given time)
            if (HTMLTableQuoteFeed.ID.equals(feedId))
            {
                JobSpec spec = new JobSpec(feedId, Arrays.asList(s));
                spec.rule = HostSchedulingRule
                                .createFor(s.getLatestFeedURL() == null ? s.getFeedURL() : s.getLatestFeedURL());
                specs.add(spec);
            }
            else
            {
                feed2securities.computeIfAbsent(feedId, key -> new JobSpec(key)).securities.add(s);
            }
        }

        specs.addAll(feed2securities.values());

        return specs;
    }

    private void doUpdateHistoricalQuotes(IProgressMonitor monitor)
    {
        boolean[] isDirty = new boolean[1];

        // randomize list in case LRU cache size of HTMLTableQuote feed is too
        // small; otherwise entries would be evicted in order
        Collections.shuffle(securities);

        JobGroup jobGroup = new JobGroup(Messages.JobLabelUpdating, 10, securities.size());

        for (Security security : securities)
        {
            if (monitor.isCanceled())
                return;

            Job job = new Job(security.getName())
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    monitor.subTask(MessageFormat.format(Messages.JobMsgUpdatingQuotesFor, security.getName()));

                    QuoteFeed feed = Factory.getQuoteFeedProvider(security.getFeed());
                    if (feed == null)
                        return Status.OK_STATUS;

                    ArrayList<Exception> exceptions = new ArrayList<>();

                    if (feed.updateHistoricalQuotes(security, exceptions))
                        isDirty[0] = true;

                    if (!exceptions.isEmpty())
                        PortfolioPlugin.log(createErrorStatus(security.getName(), exceptions));

                    return Status.OK_STATUS;
                }
            };

            if (HTMLTableQuoteFeed.ID.equals(security.getFeed()))
                job.setRule(HostSchedulingRule.createFor(security.getFeedURL()));

            job.setJobGroup(jobGroup);
            job.schedule();
        }

        try
        {
            jobGroup.join(240000, monitor);
        }
        catch (InterruptedException ignore)
        {
            // ignore
        }

        // if the job is cancelled, do not mark the client dirty as it would
        // trigger the creation of a new Display
        if (isDirty[0] && !monitor.isCanceled())
            getClient().markDirty();
    }

    private IStatus createErrorStatus(String label, List<Exception> exceptions)
    {
        MultiStatus status = new MultiStatus(PortfolioPlugin.PLUGIN_ID, IStatus.ERROR, label, null);
        for (Exception exception : exceptions)
            status.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, exception.getMessage(), exception));
        return status;
    }

}
