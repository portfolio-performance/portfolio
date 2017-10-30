package name.abuchen.portfolio.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

final class CreateInvestmentPlanTxJob extends AbstractClientJob
{
    private Job startAfterOtherJob;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    public CreateInvestmentPlanTxJob(Client client)
    {
        super(client, Messages.InvestmentPlanAutoCreationJob);
    }

    public void startAfter(Job otherJob)
    {
        this.startAfterOtherJob = otherJob;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        try
        {
            if (startAfterOtherJob != null)
                startAfterOtherJob.join();

            Map<InvestmentPlan, List<PortfolioTransaction>> tx = new HashMap<>();

            CurrencyConverterImpl converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
            getClient().getPlans().stream().forEach(plan -> {
                List<PortfolioTransaction> transactions = plan.generateTransactions(converter);
                if (!transactions.isEmpty())
                    tx.put(plan, transactions);
            });

            if (!tx.isEmpty())
            {
                Display.getDefault().asyncExec(() -> {

                    String message;

                    if (tx.size() == 1)
                    {
                        Entry<InvestmentPlan, List<PortfolioTransaction>> entry = tx.entrySet().iterator().next();
                        message = MessageFormat.format(Messages.InvestmentPlanTxCreated, entry.getKey().getName(),
                                        entry.getValue().size());
                    }
                    else
                    {
                        int count = tx.values().stream().mapToInt(List::size).sum();

                        StringBuilder builder = new StringBuilder();
                        builder.append(MessageFormat.format(Messages.InvestmentPlanTxForMultiplePlansCreated, count));

                        for (Entry<InvestmentPlan, List<PortfolioTransaction>> entry : tx.entrySet())
                            builder.append(MessageFormat.format("\n{0}: {1}", entry.getKey().getName(), //$NON-NLS-1$
                                            entry.getValue().size()));

                        message = builder.toString();
                    }

                    // FIXME Oxygen supports custom button labels
                    MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.LabelInfo, message);
                });
            }

        }
        catch (InterruptedException ignore) // NOSONAR
        {
            // ignore
        }
        return Status.OK_STATUS;
    }
}
