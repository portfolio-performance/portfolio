package name.abuchen.portfolio.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.online.CPIFeed;
import name.abuchen.portfolio.online.DestatisCPIFeed;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class UpdateCPIJob extends Job
{
    private Client client;

    public UpdateCPIJob(Client client)
    {
        super(Messages.JobLabelUpdateCPI);
        this.client = client;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        CPIFeed feed = new DestatisCPIFeed();

        List<IStatus> errors = new ArrayList<IStatus>();

        try
        {
            List<ConsumerPriceIndex> prices = feed.getConsumerPriceIndeces();
            client.setConsumerPriceIndeces(prices);
        }
        catch (IOException e)
        {
            errors.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }

        notifyFinished();

        if (!errors.isEmpty())
        {
            PortfolioPlugin.log(new MultiStatus(PortfolioPlugin.PLUGIN_ID, -1, errors.toArray(new IStatus[0]),
                            Messages.JobMsgErrorUpdatingIndeces, null));
        }

        return Status.OK_STATUS;
    }

    protected void notifyFinished()
    {}
}
