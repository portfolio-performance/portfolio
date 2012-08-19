package name.abuchen.portfolio.ui;

import java.io.IOException;
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
import org.eclipse.core.runtime.jobs.Job;

public class UpdateQuotesJob extends Job
{
    private final List<Security> securities;
    private final boolean includeHistoricQuotes;
    private final long repeatPeriod;

    public UpdateQuotesJob(Client client)
    {
        this(client, true, -1);
    }

    public UpdateQuotesJob(List<Security> securities, boolean includeHistoricQuotes, long repeatPeriod)
    {
        super(Messages.JobLabelUpdateQuotes);
        this.securities = new ArrayList<Security>(securities);
        this.includeHistoricQuotes = includeHistoricQuotes;
        this.repeatPeriod = repeatPeriod;
    }

    public UpdateQuotesJob(Client client, boolean includeHistoricQuotes, long repeatPeriod)
    {
        this(client.getSecurities(), includeHistoricQuotes, repeatPeriod);
    }

    public UpdateQuotesJob(Security security)
    {
        this(Arrays.asList(new Security[] { security }), true, -1);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdating, securities.size());

        List<IStatus> errors = new ArrayList<IStatus>();

        doUpdateLatestQuotes(monitor, errors);

        if (includeHistoricQuotes)
            doUpdateHistoricalQuotes(monitor, errors);

        notifyFinished();

        if (!errors.isEmpty())
        {
            PortfolioPlugin.log(new MultiStatus(PortfolioPlugin.PLUGIN_ID, -1, errors.toArray(new IStatus[0]),
                            Messages.JobMsgErrorUpdatingQuotes, null));
        }

        if (repeatPeriod > 0)
            schedule(repeatPeriod);

        return Status.OK_STATUS;
    }

    private void doUpdateLatestQuotes(IProgressMonitor monitor, List<IStatus> errors)
    {
        Map<String, List<Security>> byFeeds = new HashMap<String, List<Security>>();
        for (Security s : securities)
        {
            List<Security> l = byFeeds.get(s.getFeed());
            if (l == null)
                byFeeds.put(s.getFeed(), l = new ArrayList<Security>());
            l.add(s);
        }

        for (Map.Entry<String, List<Security>> entry : byFeeds.entrySet())
        {
            try
            {
                QuoteFeed feed = Factory.getQuoteFeedProvider(entry.getKey());
                if (feed != null)
                {
                    ArrayList<Exception> exceptions = new ArrayList<Exception>();
                    feed.updateLatestQuotes(entry.getValue(), exceptions);
                    
                    for (Exception e : exceptions)
                        errors.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
                }
            }
            catch (IOException e)
            {
                errors.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        }
    }

    private void doUpdateHistoricalQuotes(IProgressMonitor monitor, List<IStatus> errors)
    {
        for (Security security : securities)
        {
            try
            {
                QuoteFeed feed = Factory.getQuoteFeedProvider(security.getFeed());
                if (feed != null)
                    feed.updateHistoricalQuotes(security);
            }
            catch (IOException e)
            {
                errors.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, security.getName() + ": " //$NON-NLS-1$
                                + e.getMessage(), e));
            }

            monitor.worked(1);
        }
    }

    protected void notifyFinished()
    {}
}
