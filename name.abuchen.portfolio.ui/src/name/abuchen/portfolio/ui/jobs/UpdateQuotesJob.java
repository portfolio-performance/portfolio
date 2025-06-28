package name.abuchen.portfolio.ui.jobs;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.oauth.AccessToken;
import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.AuthenticationExpiredException;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.FeedConfigurationException;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;
import name.abuchen.portfolio.online.impl.HTMLTableQuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.AuthenticationRequiredDialog;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;
import name.abuchen.portfolio.ui.preferences.Experiments;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public final class UpdateQuotesJob extends AbstractClientJob
{
    public enum Target
    {
        LATEST, HISTORIC
    }

    /**
     * Throttles the dirty notification to the user interface. Background:
     * marking the client dirty after every job sends too many update events to
     * the user interface.
     */
    private static class Dirtyable
    {
        private static final long TIME_THRESHOLD_MS = 250;

        private final Client client;
        private final AtomicBoolean dirtyState = new AtomicBoolean(false);
        private long lastUpdate = System.currentTimeMillis();

        public Dirtyable(Client client)
        {
            this.client = client;
        }

        public synchronized void markDirty()
        {
            long now = System.currentTimeMillis();
            if (now - lastUpdate >= TIME_THRESHOLD_MS)
            {
                client.markDirty();
                dirtyState.set(false);
                lastUpdate = now;
            }
            else
            {
                dirtyState.set(true);
            }
        }

        public synchronized void flushIfDue()
        {
            if (dirtyState.get())
            {
                client.markDirty();
                dirtyState.set(false);
            }
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
            return rule instanceof HostSchedulingRule hostSchedulingRule && hostSchedulingRule.host.equals(this.host);
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

    private final OAuthClient oauthClient = OAuthClient.INSTANCE;

    private final Set<Target> target;
    private final Predicate<Security> filter;
    private long repeatPeriod;

    public UpdateQuotesJob(Client client, Set<Target> target)
    {
        this(client, s -> true, target);
    }

    public UpdateQuotesJob(Client client, Security security)
    {
        this(client, s -> s.equals(security), EnumSet.allOf(Target.class));
    }

    public UpdateQuotesJob(Client client, List<Security> securities)
    {
        this(client, securities::contains, EnumSet.allOf(Target.class));
    }

    public UpdateQuotesJob(Client client, Predicate<Security> filter, Set<Target> target)
    {
        super(client, Messages.JobLabelUpdateQuotes);

        this.target = target;
        this.filter = filter;
    }

    public UpdateQuotesJob repeatEvery(long milliseconds)
    {
        this.repeatPeriod = milliseconds;
        return this;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        var isExperimentEnabled = new Experiments().isEnabled(Experiments.Feature.JULY26_REFACTORED_PRICE_UPDATE);
        if (isExperimentEnabled)
        {
            var job = new UpdatePricesJob(getClient(), filter, target);
            job.repeatEvery(repeatPeriod);
            job.schedule();
            return Status.OK_STATUS;
        }

        monitor.beginTask(Messages.JobLabelUpdating, IProgressMonitor.UNKNOWN);

        List<Security> securities = getClient().getSecurities().stream().filter(filter).collect(toMutableList());

        Optional<AccessToken> accessToken = Optional.empty();

        // try to get the access token
        try
        {
            if (oauthClient.isAuthenticated())
                accessToken = oauthClient.getAPIAccessToken();
        }
        catch (AuthenticationException e)
        {
            PortfolioPlugin.log(e);
            // unable to refresh access token --> user needs to re-authenticate
        }

        if (accessToken.isEmpty())
        {
            // check if any of the jobs need an authenticated user

            var feed = Factory.getQuoteFeed(PortfolioPerformanceFeed.class);
            var feedNeedsUser = feed.requiresAuthentication(securities);

            if (feedNeedsUser)
            {
                // inform the user but go ahead with the remaining updates
                Display.getDefault().asyncExec(
                                () -> AuthenticationRequiredDialog.open(Display.getDefault().getActiveShell()));
            }
        }

        Dirtyable dirtyable = new Dirtyable(getClient());
        List<Job> jobs = new ArrayList<>();

        // include historical quotes
        if (target.contains(Target.HISTORIC))
        {
            var groupedByFeed = securities.stream()
                            .filter(security -> security.getFeed() != null && !security.getFeed().isEmpty()
                                            && !QuoteFeed.MANUAL.equals(security.getFeed()))
                            .collect(Collectors.groupingBy(Security::getFeed));

            // schedule feeds as single jobs that require a rate limit
            var scheduleSingleJobs = List.of(PortfolioPerformanceFeed.ID, PortfolioReportQuoteFeed.ID,
                            CoinGeckoQuoteFeed.ID, AlphavantageQuoteFeed.ID);

            for (var entry : groupedByFeed.entrySet())
            {
                var feed = Factory.getQuoteFeedProvider(entry.getKey());
                if (feed == null)
                    continue;

                // update instruments first that have not been update yet. Then
                // instruments ordered by last update time

                var securitiesPerFeed = entry.getValue().stream()
                                .sorted(Comparator.comparing(s -> s.getEphemeralData().getFeedLastUpdate().orElse(null),
                                                Comparator.nullsFirst(Comparator.naturalOrder())))
                                .toList();

                if (scheduleSingleJobs.contains(feed.getId()))
                {
                    addSingleHistoricalQuotesJob(feed, securitiesPerFeed, dirtyable, jobs);
                }
                else
                {
                    addHistoricalQuotesJobs(feed, securitiesPerFeed, dirtyable, jobs);
                }
            }
        }

        // include latest quotes
        if (target.contains(Target.LATEST))
            addLatestQuotesJobs(securities, dirtyable, jobs);

        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

        if (!jobs.isEmpty())
            runJobs(monitor, jobs);

        dirtyable.flushIfDue();

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

    private void addLatestQuotesJobs(List<Security> securities, Dirtyable dirtyable, List<Job> jobs)
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
            if (QuoteFeed.MANUAL.equals(feed.getId()))
                continue;

            // skip download if the latest quotes are downloaded as part of the
            // download of the historic quotes
            if (feed.mergeDownloadRequests() && target.contains(Target.HISTORIC))
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
        return new Job(feed.getName() + ": " + security.getName() + " " + Messages.EditWizardLatestQuoteFeedTitle) //$NON-NLS-1$ //$NON-NLS-2$
        {
            /** number of reschedules before failing permanently */
            int count = feed.getMaxRateLimitAttempts();

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
                    count--;

                    if (count >= 0 && e.getRetryAfter().isPositive())
                    {
                        schedule(e.getRetryAfter().toMillis());
                        return Status.OK_STATUS;
                    }
                    else
                    {
                        return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage());
                    }
                }
                catch (QuoteFeedException e)
                {
                    PortfolioPlugin.log(e);
                    return Status.OK_STATUS;
                }
            }
        };
    }

    private void addHistoricalQuotesJobs(QuoteFeed feed, List<Security> securities, Dirtyable dirtyable, List<Job> jobs)
    {
        for (Security security : securities)
        {
            Job job = new Job(feed.getName() + ": " + security.getName()) //$NON-NLS-1$
            {
                /** number of reschedules before failing permanently */
                int count = feed.getMaxRateLimitAttempts();

                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    if (security.getEphemeralData().hasPermanentError())
                        return Status.OK_STATUS;

                    try
                    {
                        QuoteFeedData data = feed.getHistoricalQuotes(security, false);
                        security.getEphemeralData().touchFeedLastUpdate();

                        if (security.addAllPrices(data.getPrices()))
                            dirtyable.markDirty();

                        if (!data.getErrors().isEmpty())
                            PortfolioPlugin.log(createErrorStatus(security.getName(), data.getErrors()));

                        // download latest quotes if the download should be done
                        // together (and the job includes the download of latest
                        // quotes and the latest feed configuration is the same
                        // feed)

                        String feedId = security.getLatestFeed();
                        if (feedId == null)
                            feedId = security.getFeed();
                        if (feed.mergeDownloadRequests() && target.contains(Target.LATEST)
                                        && Objects.equals(feedId, feed.getId()))
                        {
                            feed.getLatestQuote(security).ifPresent(p -> {
                                if (security.setLatest(p))
                                    dirtyable.markDirty();
                            });
                        }

                        return Status.OK_STATUS;
                    }
                    catch (FeedConfigurationException e)
                    {
                        security.getEphemeralData().setHasPermanentError();
                        PortfolioPlugin.log(MessageFormat.format(Messages.MsgInstrumentWithConfigurationIssue,
                                        security.getName()), e);
                        return Status.OK_STATUS;
                    }
                    catch (RateLimitExceededException e)
                    {
                        count--;

                        if (count >= 0 && e.getRetryAfter().isPositive())
                        {
                            PortfolioPlugin.log(MessageFormat.format(Messages.MsgRateLimitExceededAndRetrying,
                                            security.getName(), count));
                            schedule(e.getRetryAfter().toMillis());
                            return Status.OK_STATUS;
                        }
                        else
                        {
                            return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage());
                        }

                    }
                    catch (QuoteFeedException e)
                    {
                        PortfolioPlugin.log(e);
                        return Status.OK_STATUS;
                    }
                }
            };

            if (HTMLTableQuoteFeed.ID.equals(security.getFeed()))
                job.setRule(HostSchedulingRule.createFor(security.getFeedURL()));

            jobs.add(job);
        }
    }

    private void addSingleHistoricalQuotesJob(QuoteFeed feed, List<Security> securities, Dirtyable dirtyable,
                    List<Job> jobs)
    {
        if (securities.isEmpty())
            return;

        Job job = new Job(feed.getName() + ": " + securities.size()) //$NON-NLS-1$
        {
            List<Security> candidates = new ArrayList<>(securities);

            /** number of reschedules before failing permanently */
            int count = feed.getMaxRateLimitAttempts();

            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                while (!candidates.isEmpty())
                {
                    var security = candidates.getFirst();

                    if (security.getEphemeralData().hasPermanentError())
                    {
                        candidates.remove(security);
                        continue;
                    }

                    try
                    {
                        QuoteFeedData data = feed.getHistoricalQuotes(security, false);
                        security.getEphemeralData().touchFeedLastUpdate();

                        candidates.remove(security);

                        if (security.addAllPrices(data.getPrices()))
                            dirtyable.markDirty();

                        if (!data.getErrors().isEmpty())
                            PortfolioPlugin.log(createErrorStatus(security.getName(), data.getErrors()));

                        // download latest quotes if the download should be done
                        // together (and the job includes the download of latest
                        // quotes and the latest feed configuration is the same
                        // feed)

                        String feedId = security.getLatestFeed();
                        if (feedId == null)
                            feedId = security.getFeed();
                        if (feed.mergeDownloadRequests() && target.contains(Target.LATEST)
                                        && Objects.equals(feedId, feed.getId()))
                        {
                            feed.getLatestQuote(security).ifPresent(p -> {
                                if (security.setLatest(p))
                                    dirtyable.markDirty();
                            });
                        }
                    }
                    catch (AuthenticationExpiredException e)
                    {
                        PortfolioPlugin.log(e);
                        return Status.OK_STATUS;
                    }
                    catch (FeedConfigurationException e)
                    {
                        security.getEphemeralData().setHasPermanentError();
                        candidates.remove(security);

                        PortfolioPlugin.log(MessageFormat.format(Messages.MsgInstrumentWithConfigurationIssue,
                                        security.getName()), e);
                    }
                    catch (RateLimitExceededException e)
                    {
                        count--;

                        if (count >= 0 && e.getRetryAfter().isPositive())
                        {
                            PortfolioPlugin.log(MessageFormat.format(Messages.MsgRateLimitExceededAndRetrying,
                                            security.getName(), count));
                            schedule(e.getRetryAfter().toMillis());
                            return Status.OK_STATUS;
                        }
                        else
                        {
                            return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage());
                        }
                    }
                    catch (QuoteFeedException e)
                    {
                        candidates.remove(security);
                        PortfolioPlugin.log(e);
                    }
                }

                return Status.OK_STATUS;
            }
        };

        jobs.add(job);
    }

    private IStatus createErrorStatus(String label, List<Exception> exceptions)
    {
        MultiStatus status = new MultiStatus(PortfolioPlugin.PLUGIN_ID, IStatus.ERROR, label, null);
        for (Exception exception : exceptions)
            status.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, exception.getMessage(),
                            exception instanceof WebAccessException ? null : exception));

        return status;
    }

}
