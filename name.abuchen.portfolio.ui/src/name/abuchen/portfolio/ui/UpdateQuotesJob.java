package name.abuchen.portfolio.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;

public final class UpdateQuotesJob extends AbstractClientJob
{
    private final List<Security> securities;
    private final boolean includeHistoricQuotes;
    private final long repeatPeriod;

    public UpdateQuotesJob(Client client)
    {
        this(client, true, -1);
    }

    public UpdateQuotesJob(Client client, List<Security> securities, boolean includeHistoricQuotes, long repeatPeriod)
    {
        super(client, Messages.JobLabelUpdateQuotes);
        this.securities = new ArrayList<Security>(securities);
        this.includeHistoricQuotes = includeHistoricQuotes;
        this.repeatPeriod = repeatPeriod;
    }

    public UpdateQuotesJob(Client client, boolean includeHistoricQuotes, long repeatPeriod)
    {
        this(client, client.getSecurities(), includeHistoricQuotes, repeatPeriod);
    }

    public UpdateQuotesJob(Client client, Security security)
    {
        this(client, Arrays.asList(new Security[] { security }), true, -1);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdating, securities.size());

        List<IStatus> errors = new ArrayList<IStatus>();

        // update latest quotes
        boolean isDirty = doUpdateLatestQuotes(monitor, errors);

        // update historical quotes
        if (includeHistoricQuotes)
        {
            boolean isHistoricalDirty = doUpdateHistoricalQuotes(monitor, errors);
            isDirty = isDirty || isHistoricalDirty;
        }

        if (isDirty)
        {
            getClient().markDirty();
        }

        if (!errors.isEmpty())
        {
            PortfolioPlugin.log(new MultiStatus(PortfolioPlugin.PLUGIN_ID, -1, errors.toArray(new IStatus[0]),
                            Messages.JobMsgErrorUpdatingQuotes, null));
        }

        if (repeatPeriod > 0)
        {
            schedule(repeatPeriod);
        }

        return Status.OK_STATUS;
    }

    private boolean doUpdateLatestQuotes(IProgressMonitor monitor, List<IStatus> errors)
    {
        Map<String, List<Security>> feed2securities = new HashMap<>();
        for (Security s : securities)
        {
            // if configured, use feed for latest quotes
            // otherwise use the default feed used by historical quotes as well
            String feedId = s.getLatestFeed();
            if (feedId == null)
                feedId = s.getFeed();

            feed2securities.computeIfAbsent(feedId, key -> new ArrayList<Security>()).add(s);
        }

        boolean isDirty = false;

        for (Map.Entry<String, List<Security>> entry : feed2securities.entrySet())
        {
            if (monitor.isCanceled())
                return isDirty;

            QuoteFeed feed = Factory.getQuoteFeedProvider(entry.getKey());
            if (feed != null)
            {
                ArrayList<Exception> exceptions = new ArrayList<Exception>();
                boolean isUpdated = feed.updateLatestQuotes(entry.getValue(), exceptions);
                isDirty = isDirty || isUpdated;

                if (!exceptions.isEmpty())
                    addToErrors(feed.getName(), exceptions, errors);
            }
        }

        return isDirty;
    }

    private boolean doUpdateHistoricalQuotes(IProgressMonitor monitor, List<IStatus> errors)
    {
        boolean isDirty = false;

        // randomize list in case LRU cache size of HTMLTableQuote feed is too
        // small; otherwise entries would be evited in order
        List<Security> list = new ArrayList<>(securities);
        Collections.shuffle(list);

        for (Security security : list)
        {
            if (monitor.isCanceled())
                return isDirty;

            monitor.subTask(MessageFormat.format(Messages.JobMsgUpdatingQuotesFor, security.getName()));

            QuoteFeed feed = Factory.getQuoteFeedProvider(security.getFeed());
            if (feed != null)
            {
                ArrayList<Exception> exceptions = new ArrayList<Exception>();
                boolean isUpdated = feed.updateHistoricalQuotes(security, exceptions);

                isDirty = isDirty || isUpdated;

                if (!exceptions.isEmpty())
                    addToErrors(security.getName(), exceptions, errors);
            }

            monitor.worked(1);
        }

        return isDirty;
    }

    private void addToErrors(String label, List<Exception> exceptions, List<IStatus> errors)
    {
        MultiStatus status = new MultiStatus(PortfolioPlugin.PLUGIN_ID, IStatus.ERROR, label, null);
        for (Exception exception : exceptions)
            status.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, exception.getMessage(), exception));
        errors.add(status);
    }
}
