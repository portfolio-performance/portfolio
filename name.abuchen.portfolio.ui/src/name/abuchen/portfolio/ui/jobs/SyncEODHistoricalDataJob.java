package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.online.TaxonomySource;
import name.abuchen.portfolio.online.impl.EODHistoricalDataCom;
import name.abuchen.portfolio.online.impl.EODHistoricalDataCom.Fundamentals;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public final class SyncEODHistoricalDataJob extends AbstractClientJob
{
    private String apiKey;

    public SyncEODHistoricalDataJob(Client client, String apiKey)
    {
        super(client, MessageFormat.format(Messages.JobLabelSyncSecuritiesOnline, "https://eodhistoricaldata.com")); //$NON-NLS-1$

        this.apiKey = Objects.requireNonNull(apiKey);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        List<Security> toBeSynced = getClient().getSecurities().stream() //
                        .filter(s -> !Strings.isNullOrEmpty(s.getTickerSymbol())) //
                        .collect(Collectors.toList());

        if (toBeSynced.isEmpty())
            return Status.OK_STATUS;

        monitor.beginTask(getName(), toBeSynced.size());

        boolean isDirty = false;

        List<Taxonomy> countries = getClient().getTaxonomies().stream()
                        .filter(t -> TaxonomySource.EOD_HISTORICAL_DATA_COUNTRY_ALLOCATION.getIdentifier()
                                        .equals(t.getSource()))
                        .collect(Collectors.toList());

        List<Taxonomy> sectors = getClient().getTaxonomies().stream().filter(
                        t -> TaxonomySource.EOD_HISTORICAL_DATA_SECTOR_ALLOCATION.getIdentifier().equals(t.getSource()))
                        .collect(Collectors.toList());

        List<IOException> errors = new ArrayList<>();

        EODHistoricalDataCom eodhistoricaldata = new EODHistoricalDataCom(apiKey);

        for (Security security : toBeSynced)
        {
            monitor.worked(1);

            try
            {
                Optional<EODHistoricalDataCom.Fundamentals> fundamentals = eodhistoricaldata
                                .lookup(security.getTickerSymbol());

                if (fundamentals.isPresent())
                {
                    Fundamentals f = fundamentals.get();
                    boolean hasUpdate = false;

                    for (Taxonomy taxonomy : countries)
                    {
                        hasUpdate = f.updateCountryAllocation(taxonomy, security);
                        isDirty = isDirty || hasUpdate;
                    }

                    for (Taxonomy taxonomy : sectors)
                    {
                        hasUpdate = f.updateSectorAllocation(taxonomy, security);
                        isDirty = isDirty || hasUpdate;
                    }
                }
                else
                {
                    PortfolioPlugin.info(MessageFormat.format(
                                    "No eodhistoricaldata.com information found for ''{0}'' with Ticker {1}", //$NON-NLS-1$
                                    security.getName(), security.getTickerSymbol()));
                }
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
                errors.forEach(e -> status
                                .add(new Status(IStatus.ERROR, SyncEODHistoricalDataJob.class, e.getMessage())));
                ErrorDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, this.getName(),
                                status);
            });
        }

        return Status.OK_STATUS;
    }
}
