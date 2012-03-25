package name.abuchen.portfolio.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.YahooFinanceQuoteFeed;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class UpdateQuotesJob extends Job
{
    private final Client client;
    private final boolean includeHistoricQuotes;
    private final long repeatPeriod;

    public UpdateQuotesJob(Client client)
    {
        this(client, true, -1);
    }

    public UpdateQuotesJob(Client client, boolean includeHistoricQuotes, long repeatPeriod)
    {
        super(Messages.JobLabelUpdateQuotes);
        this.client = client;
        this.includeHistoricQuotes = includeHistoricQuotes;
        this.repeatPeriod = repeatPeriod;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdating, client.getSecurities().size());
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed();

        List<IStatus> errors = new ArrayList<IStatus>();

        try
        {
            feed.updateLatestQuote(client.getSecurities());
        }
        catch (IOException e)
        {
            errors.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_IN, e.getMessage(), e));
        }

        if (includeHistoricQuotes)
        {
            for (Security security : client.getSecurities())
            {
                try
                {
                    feed.updateHistoricalQuotes(security);
                }
                catch (IOException e)
                {
                    errors.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_IN, security.getName() + ": " //$NON-NLS-1$
                                    + e.getMessage(), e));
                }

                monitor.worked(1);
            }
        }

        notifyFinished();

        if (!errors.isEmpty())
        {
            PortfolioPlugin.log(new MultiStatus(PortfolioPlugin.PLUGIN_IN, -1, errors.toArray(new IStatus[0]),
                            Messages.JobMsgErrorUpdatingQuotes, null));
        }

        if (repeatPeriod > 0)
            schedule(repeatPeriod);

        return Status.OK_STATUS;
    }

    protected void notifyFinished()
    {}
}
