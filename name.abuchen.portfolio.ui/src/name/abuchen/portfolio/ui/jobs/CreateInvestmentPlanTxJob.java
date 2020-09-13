package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public final class CreateInvestmentPlanTxJob extends AbstractClientJob
{
    private Job startAfterOtherJob;

    private ExchangeRateProviderFactory factory;

    public CreateInvestmentPlanTxJob(Client client, ExchangeRateProviderFactory factory)
    {
        super(client, Messages.InvestmentPlanAutoCreationJob);
        this.factory = factory;
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

            Map<InvestmentPlan, List<TransactionPair<?>>> tx = new HashMap<>();

            CurrencyConverterImpl converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
            getClient().getPlans().stream().filter(InvestmentPlan::isAutoGenerate).forEach(plan -> {
                try
                {
                    List<TransactionPair<?>> transactions = plan.generateTransactions(converter);
                    if (!transactions.isEmpty())
                        tx.put(plan, transactions);
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);
                }
            });

            if (!tx.isEmpty())
            {
                Display.getDefault().asyncExec(() -> {

                    String message;

                    if (tx.size() == 1)
                    {
                        Entry<InvestmentPlan, List<TransactionPair<?>>> entry = tx.entrySet().iterator().next();
                        message = MessageFormat.format(Messages.InvestmentPlanTxCreated, entry.getKey().getName(),
                                        entry.getValue().size());
                    }
                    else
                    {
                        int count = tx.values().stream().mapToInt(List::size).sum();

                        StringBuilder builder = new StringBuilder();
                        builder.append(MessageFormat.format(Messages.InvestmentPlanTxForMultiplePlansCreated, count));

                        for (Entry<InvestmentPlan, List<TransactionPair<?>>> entry : tx.entrySet())
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
