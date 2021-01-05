package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.common.base.Strings;
import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.ETFDataCom;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public final class SyncETFDataJob extends AbstractClientJob
{
    public SyncETFDataJob(Client client)
    {
        super(client, Messages.JobLabelSyncSecuritiesOnline);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        List<Security> toBeSynced = getClient().getSecurities().stream()
                        .filter(s -> !Strings.isNullOrEmpty(s.getIsin())).collect(Collectors.toList());

        if (toBeSynced.isEmpty())
            return Status.OK_STATUS;

        monitor.beginTask(MessageFormat.format(Messages.JobLabelSyncSecuritiesOnline, "https://etf-data.com"), //$NON-NLS-1$
                        toBeSynced.size());

        boolean isDirty = false;

        ETFDataCom etfdata = new ETFDataCom();

        for (Security security : toBeSynced)
        {
            monitor.worked(1);

            try
            {
                List<ResultItem> items = etfdata.search(security.getIsin());

                if (!items.isEmpty())
                {
                    boolean hasUpdate = ETFDataCom.updateWith(security, getClient().getSettings(), items.get(0));
                    isDirty = isDirty || hasUpdate;
                }
                else
                {
                    PortfolioPlugin.info(
                                    MessageFormat.format("No etf-data.com information found for ''{0}'' with ISIN {1}", //$NON-NLS-1$
                                                    security.getName(), security.getIsin()));
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
