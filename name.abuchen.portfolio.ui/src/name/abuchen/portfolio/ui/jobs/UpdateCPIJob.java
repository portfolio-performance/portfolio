package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.online.CPIFeed;
import name.abuchen.portfolio.online.DestatisCPIFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

public final class UpdateCPIJob extends AbstractClientJob
{
    public UpdateCPIJob(Client client)
    {
        super(client, Messages.JobLabelUpdateCPI);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdateCPI, 1);

        CPIFeed feed = new DestatisCPIFeed();

        List<IStatus> errors = new ArrayList<IStatus>();

        try
        {
            List<ConsumerPriceIndex> prices = feed.getConsumerPriceIndices();
            boolean isDirty = getClient().setConsumerPriceIndices(prices);

            if (isDirty)
                getClient().markDirty();
        }
        catch (IOException e)
        {
            errors.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }

        if (!errors.isEmpty())
        {
            PortfolioPlugin.log(new MultiStatus(PortfolioPlugin.PLUGIN_ID, -1, errors.toArray(new IStatus[0]),
                            Messages.JobMsgErrorUpdatingIndices, null));
        }

        return Status.OK_STATUS;
    }
}
