package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendPayment;
import name.abuchen.portfolio.online.DividendFeed;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.DivvyDiaryDividendFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public final class UpdateDividendsJob extends AbstractClientJob
{

    private final List<Security> securities;

    public UpdateDividendsJob(Client client)
    {
        this(client, client.getSecurities());
    }

    public UpdateDividendsJob(Client client, Security security)
    {
        this(client, Arrays.asList(security));
    }

    public UpdateDividendsJob(Client client, List<Security> securities)
    {
        super(client, "Reading dividend payments");

        this.securities = new ArrayList<>(securities);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdating, IProgressMonitor.UNKNOWN);

        DividendFeed feed = Factory.getDividendFeed(DivvyDiaryDividendFeed.class);

        boolean isDirty = false;

        for (Security security : securities)
        {
            try
            {
                List<DividendPayment> dividends = feed.getDividendPayments(security);

                if (!dividends.isEmpty())
                {
                    security.removeEventIf(event -> event.getType() == SecurityEvent.Type.DIVIDEND_PAYMENT);
                    dividends.forEach(dividend -> security.addEvent(dividend));
                    isDirty = true;
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
