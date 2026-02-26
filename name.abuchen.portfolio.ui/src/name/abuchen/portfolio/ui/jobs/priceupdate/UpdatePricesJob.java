package name.abuchen.portfolio.ui.jobs.priceupdate;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.oauth.AccessToken;
import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.AuthenticationRequiredDialog;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.jobs.priceupdate.Task.HistoricalTask;
import name.abuchen.portfolio.ui.jobs.priceupdate.Task.LatestTask;

/**
 * Updates prices for the given securities. Experimental. Meant as drop-in
 * replacement for UpdateQuotesJob.
 */
public class UpdatePricesJob extends AbstractClientJob
{
    public enum Target
    {
        LATEST, HISTORIC
    }

    private static final int UI_PROGRESS_UPDATE_INTERVAL = 300;

    private final Set<Target> target;
    private final Predicate<Security> filter;

    private boolean suppressAuthenticationDialog = false;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public UpdatePricesJob(Client client, Set<Target> target)
    {
        this(client, s -> true, target);
    }

    public UpdatePricesJob(Client client, Security security)
    {
        this(client, s -> s.equals(security), EnumSet.allOf(Target.class));
    }

    public UpdatePricesJob(Client client, List<Security> securities)
    {
        this(client, securities::contains, EnumSet.allOf(Target.class));
    }

    public UpdatePricesJob(Client client, Predicate<Security> filter, Set<Target> target)
    {
        super(client, Messages.JobLabelUpdateQuotes);

        this.target = target;
        this.filter = filter;
    }

    public void suppressAuthenticationDialog(boolean suppressAuthenticationDialog)
    {
        this.suppressAuthenticationDialog = suppressAuthenticationDialog;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdating, IProgressMonitor.UNKNOWN);

        List<Security> securities = getClient().getSecurities().stream().filter(filter).collect(toMutableList());

        Optional<AccessToken> accessToken = Optional.empty();

        // try to get the access token
        try
        {
            if (OAuthClient.INSTANCE.isAuthenticated())
                accessToken = OAuthClient.INSTANCE.getAPIAccessToken();
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
            var requireAuthentication = feed.requireAuthentication(securities);

            if (!requireAuthentication.isEmpty() && !suppressAuthenticationDialog)
            {
                Display.getDefault().asyncExec(() -> AuthenticationRequiredDialog
                                .open(Display.getDefault().getActiveShell(), getClient(), requireAuthentication));
            }
        }

        // create price update request

        var request = new PriceUpdateRequest(getClient(), securities, target.contains(Target.LATEST),
                        target.contains(Target.HISTORIC));

        // update instruments first that have not been update yet. The other
        // instruments are ordered by last update time

        Collections.sort(securities, Comparator.comparing(s -> s.getEphemeralData().getFeedLastUpdate().orElse(null),
                        Comparator.nullsFirst(Comparator.naturalOrder())));

        var tasks = new ArrayList<Task>();
        if (request.isIncludeHistorical())
            tasks.addAll(prepareHistoricalTasks(request, securities));
        if (request.isIncludeLatest())
            tasks.addAll(prepareLatestTasks(request, securities));

        // after preparing the tasks, fire an update because we may have
        // identified additional skipped instruments because manual updates or
        // previous permanent errors

        PriceUpdateProgress.getInstance().setLatestJob(getClient(), this);
        fireSnapshot(request);

        // group tasks by grouping criterion and sort biggest groups first

        var groups = tasks.stream().collect(Collectors.groupingBy(t -> t.groupingCriterion)).entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> -e.getValue().size())).toList();

        var jobs = groups.stream().map(group -> new RunTaskGroupJob(group.getKey(), group.getValue(), request))
                        .toList();

        // start periodic UI updates
        ScheduledFuture<?> periodicUpdate = scheduler.scheduleAtFixedRate(() -> {
            fireSnapshot(request);
            if (request.getAndResetDirty())
                request.getClient().markDirty();
        }, 0, UI_PROGRESS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

        JobGroup jobGroup = new JobGroup(Messages.JobLabelUpdating, 10, jobs.size());
        for (Job job : jobs)
        {
            job.setJobGroup(jobGroup);
            job.schedule();
        }

        try
        {
            jobGroup.join(0, monitor);
        }
        catch (InterruptedException ignore) // NOSONAR
        {
            // ignore
        }
        finally
        {
            periodicUpdate.cancel(false);
            if (request.getAndResetDirty())
                request.getClient().markDirty();
            fireSnapshot(request);
        }

        return Status.OK_STATUS;
    }

    private List<Task> prepareHistoricalTasks(PriceUpdateRequest request, List<Security> securities)
    {
        var tasks = new ArrayList<Task>();

        for (var security : securities)
        {
            var status = request.getStatus(security).getHistoricStatus();

            // skip if we previously detected a permanent error, e.g., a 404
            if (security.getEphemeralData().hasPermanentError())
            {
                status.setStatus(UpdateStatus.SKIPPED,
                                MessageFormat.format(Messages.MsgInstrumentWithConfigurationIssue, security.getName()));
                continue;
            }

            var feed = Factory.getQuoteFeedProvider(security.getFeed());

            if (feed == null || QuoteFeed.MANUAL.equals(feed.getId()))
            {
                status.setStatus(UpdateStatus.SKIPPED, null);
            }
            else
            {
                var groupingCriterion = feed.getGroupingCriterion(security);
                tasks.add(new HistoricalTask(groupingCriterion, feed, status, security));
            }
        }

        return tasks;
    }

    private List<Task> prepareLatestTasks(PriceUpdateRequest request, List<Security> securities)
    {
        var tasks = new ArrayList<Task>();

        for (var security : securities)
        {
            var status = request.getStatus(security).getLatestStatus();

            // skip if we previously detected a permanent error, e.g., a 404
            if (security.getEphemeralData().hasPermanentError())
            {
                status.setStatus(UpdateStatus.SKIPPED,
                                MessageFormat.format(Messages.MsgInstrumentWithConfigurationIssue, security.getName()));
                continue;
            }

            // if configured, use feed for latest quotes
            // otherwise use the default feed used by historical quotes as well
            String feedId = security.getLatestFeed();
            if (feedId == null)
                feedId = security.getFeed();

            var feed = Factory.getQuoteFeedProvider(feedId);

            if (feed == null || QuoteFeed.MANUAL.equals(feed.getId()))
            {
                status.setStatus(UpdateStatus.SKIPPED, null);
            }
            else
            {
                var groupingCriterion = feed.getLatestGroupingCriterion(security);
                tasks.add(new LatestTask(groupingCriterion, feed, status, security));
            }
        }

        return tasks;
    }

    private void fireSnapshot(PriceUpdateRequest request)
    {
        var snapshot = request.getStatusSnapshot();
        Display.getDefault().asyncExec(() -> PriceUpdateProgress.getInstance().notifyProgress(this, snapshot));
    }
}
