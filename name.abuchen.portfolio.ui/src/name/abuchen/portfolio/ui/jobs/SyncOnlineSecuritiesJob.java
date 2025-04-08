package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public final class SyncOnlineSecuritiesJob extends AbstractClientJob
{
    public SyncOnlineSecuritiesJob(Client client)
    {
        super(client, Messages.JobLabelSyncSecuritiesOnline);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        List<Security> toBeSynced = getClient().getSecurities().stream().filter(s -> s.getOnlineId() != null).toList();

        if (toBeSynced.isEmpty())
            return Status.OK_STATUS;

        monitor.beginTask(MessageFormat.format(Messages.JobLabelSyncSecuritiesOnline, "https://portfolio-report.net"), //$NON-NLS-1$
                        toBeSynced.size());

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
            catch (WebAccessException e)
            {
                // check if the onlineId has become permanently unavailable and,
                // if necessary, remove the online id

                if (e.getHttpErrorCode() == HttpStatus.SC_NOT_FOUND)
                {
                    var xError = e.getHeader("X-Error"); //$NON-NLS-1$

                    if (!xError.isEmpty() && xError.get(0).equals("Security not found")) //$NON-NLS-1$
                    {
                        security.setOnlineId(null);

                        if (PortfolioReportQuoteFeed.ID.equals(security.getFeed()))
                            security.setFeed(QuoteFeed.MANUAL);

                        if (PortfolioReportQuoteFeed.ID.equals(security.getLatestFeed()))
                            security.setFeed(null);

                        isDirty = true;

                        PortfolioPlugin.log("Unlinking " + security.getName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    else
                    {
                        PortfolioPlugin.log(security.getName() + ": " + e.getMessage()); //$NON-NLS-1$
                    }
                }
                else
                {
                    PortfolioPlugin.log(security.getName() + ": " + e.getMessage()); //$NON-NLS-1$
                }
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(security.getName(), e);
            }
        }

        if (isDirty)
            getClient().markDirty();

        return Status.OK_STATUS;
    }
}
