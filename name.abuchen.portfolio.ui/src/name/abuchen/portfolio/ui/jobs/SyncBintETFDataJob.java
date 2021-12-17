package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Strings;
import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.online.TaxonomySource;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.BintETFData;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public final class SyncBintETFDataJob extends AbstractClientJob
{
    private String apiKey;
    
    public SyncBintETFDataJob(Client client, String apiKey)
    {
        super(client, MessageFormat.format(Messages.JobLabelSyncSecuritiesOnline, "BINT.ee")); //$NON-NLS-1$
        
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        List<Security> toBeSynced = getClient().getSecurities().stream()
                        .filter(s -> !Strings.isNullOrEmpty(s.getIsin())).collect(Collectors.toList());

        if (toBeSynced.isEmpty())
            return Status.OK_STATUS;

        monitor.beginTask(getName(), toBeSynced.size());

        boolean isDirty = false;
        
        List<Taxonomy> countries = getClient().getTaxonomies().stream()
                        .filter(t -> TaxonomySource.BINT_EE_COUNTRY_ALLOCATION.getIdentifier()
                                        .equals(t.getSource()))
                        .collect(Collectors.toList());

        List<Taxonomy> sectors = getClient().getTaxonomies().stream().filter(
                        t -> TaxonomySource.BINT_EE_SECTOR_ALLOCATION.getIdentifier().equals(t.getSource()))
                        .collect(Collectors.toList());


        List<IOException> errors = new ArrayList<>();

        BintETFData etfdata = new BintETFData();
        
        etfdata.setApiKey(apiKey);

        for (Security security : toBeSynced)
        {
            monitor.worked(1);

            try
            {
                List<ResultItem> items = etfdata.search(security.getIsin());

                if (!items.isEmpty())
                {
                    boolean hasUpdate = BintETFData.updateWith(security, getClient().getSettings(), items.get(0));
                    isDirty = isDirty || hasUpdate;

                    for (Taxonomy taxonomy : countries)
                    {
                        hasUpdate = BintETFData.updateCountryAllocation(security, taxonomy, items.get(0));
                        isDirty = isDirty || hasUpdate;
                    }

                    for (Taxonomy taxonomy : sectors)
                    {
                        hasUpdate = BintETFData.updateSectorAllocation(security, taxonomy, items.get(0));
                        isDirty = isDirty || hasUpdate;
                    }
                }
                else
                {
                    PortfolioPlugin.info(
                                    MessageFormat.format("No information found for ''{0}'' with ISIN {1}", //$NON-NLS-1$
                                                    security.getName(), security.getIsin()));
                }
            }
            catch (WebAccessException e)
            {
                errors.add(e);

                // if the quota is exceeded, no use in continuing with the
                // remaining requests

                if (e.getHttpErrorCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                    break;
            }
            catch (IOException e)
            {
                errors.add(e);
            }
        }

        if (isDirty)
            getClient().markDirty();

        if (!errors.isEmpty())
        {
            Display.getDefault().asyncExec(() -> {
                MultiStatus status = new MultiStatus(PortfolioPlugin.PLUGIN_ID, IStatus.ERROR, this.getName(), null);
                errors.forEach(e -> status.add(new Status(IStatus.ERROR, SyncBintETFDataJob.class, e.getMessage())));
                ErrorDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, this.getName(),
                                status);
            });
        }

        return Status.OK_STATUS;
    }
}
