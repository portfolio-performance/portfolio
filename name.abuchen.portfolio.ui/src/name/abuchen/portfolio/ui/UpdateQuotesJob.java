package name.abuchen.portfolio.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

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
        Map<String, List<Security>> byFeeds = new HashMap<String, List<Security>>();
        for (Security s : securities)
        {
            List<Security> l = byFeeds.get(s.getFeed());
            if (l == null)
                byFeeds.put(s.getFeed(), l = new ArrayList<Security>());
            l.add(s);
        }

        boolean isDirty = false;

        for (Map.Entry<String, List<Security>> entry : byFeeds.entrySet())
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

        for (Security security : securities)
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
