package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
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

        PortfolioReportNet portfolioReport = new PortfolioReportNet();

        for (Security security : toBeSynced)
        {
            monitor.worked(1);

            try
            {
                Optional<ResultItem> item = portfolioReport.getUpdatedValues(security.getOnlineId());

                if (item.isPresent())
                {
                    boolean hasUpdate = PortfolioReportNet.updateWith(security, item.get());
                    isDirty = isDirty || hasUpdate;
                }
                else
                {
                    PortfolioPlugin.info(MessageFormat.format("No data found for ''{0}'' with OnlineId {1}", //$NON-NLS-1$
                                    security.getName(), security.getOnlineId()));
                }
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
