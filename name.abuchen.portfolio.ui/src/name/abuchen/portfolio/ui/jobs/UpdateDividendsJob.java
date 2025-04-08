package name.abuchen.portfolio.ui.jobs;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.online.DividendFeed;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.DivvyDiaryDividendFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public final class UpdateDividendsJob extends AbstractClientJob
{

    private final List<Security> securities;

    public UpdateDividendsJob(Client client)
    {
        this(client, client.getSecurities());
    }

    public UpdateDividendsJob(Client client, Predicate<Security> filter)
    {
        this(client, client.getSecurities().stream().filter(filter).toList());
    }

    public UpdateDividendsJob(Client client, Security security)
    {
        this(client, Arrays.asList(security));
    }

    public UpdateDividendsJob(Client client, List<Security> securities)
    {
        super(client, Messages.JobLabelUpdatingDividendEvents);

        this.securities = new ArrayList<>(securities);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobLabelUpdatingDividendEvents, IProgressMonitor.UNKNOWN);

        DividendFeed feed = Factory.getDividendFeed(DivvyDiaryDividendFeed.class);

        boolean isDirty = false;

        for (Security security : securities)
        {
            try
            {
                List<DividendEvent> dividends = feed.getDividendPayments(security);

                if (!dividends.isEmpty())
                {
                    List<DividendEvent> current = security.getEvents().stream()
                                    .filter(event -> event.getType() == SecurityEvent.Type.DIVIDEND_PAYMENT)
                                    .map(DividendEvent.class::cast) //
                                    .collect(toMutableList());

                    for (DividendEvent dividendEvent : dividends)
                    {
                        if (current.contains(dividendEvent))
                        {
                            current.remove(dividendEvent);
                        }
                        else
                        {
                            security.addEvent(dividendEvent);
                            isDirty = true;
                        }
                    }

                    security.removeEventIf(current::contains);

                    isDirty = isDirty || !current.isEmpty();
                }
            }
            catch (WebAccessException e)
            {
                PortfolioPlugin.log(security.getName() + ": " + e.getMessage()); //$NON-NLS-1$
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
