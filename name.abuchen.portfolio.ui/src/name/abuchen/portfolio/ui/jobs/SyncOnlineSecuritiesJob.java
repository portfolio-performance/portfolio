package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;


import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public final class SyncOnlineSecuritiesJob extends AbstractClientJob
{
    public SyncOnlineSecuritiesJob(Client client)
    {
        super(client, Messages.JobLabelSyncSecuritiesOnline);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        List<Security> toBeSynced = getClient().getSecurities().stream().filter(s -> s.getOnlineId() != null)
                        .collect(Collectors.toList());

        if (toBeSynced.isEmpty())
            return Status.OK_STATUS;

        monitor.beginTask(Messages.JobLabelSyncSecuritiesOnline, toBeSynced.size());

        boolean isDirty = false;

        for (Security security : toBeSynced)
        {
            SecuritySearchProvider securityUpdater = security.getOnlineProvider();

            monitor.worked(1);
            try
            {
                isDirty = isDirty || securityUpdater.update(security);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
            }
        }

        if (isDirty)
            getClient().markDirty();

        return Status.OK_STATUS;
    }
}
