package name.abuchen.portfolio.ui.jobs;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
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

            // Generate the transactions on the UI thread. The views iterate
            // the (unsynchronized) transaction lists of accounts and
            // portfolios on the UI thread. Adding transactions from this
            // background job results in a ConcurrentModificationException.
            Display.getDefault().syncExec(() -> {
                var isModified = false;

                for (InvestmentPlan plan : getClient().getPlans())
                {
                    if (!plan.isAutoGenerate())
                        continue;

                    var sizeBefore = plan.getTransactions().size();

                    try
                    {
                        List<TransactionPair<?>> transactions = plan.generateTransactions(converter);
                        if (!transactions.isEmpty())
                            tx.put(plan, transactions);
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);

                        // generateTransactions does not return the
                        // transactions created before the exception
                        var all = plan.getTransactions(getClient());
                        if (all.size() > sizeBefore)
                            tx.put(plan, new ArrayList<>(all.subList(sizeBefore, all.size())));
                    }

                    // generateTransactions keeps the transactions created
                    // before an IOException is thrown, therefore compare the
                    // number of transactions instead of the returned list
                    if (plan.getTransactions().size() != sizeBefore)
                        isModified = true;
                }

                // mark the client dirty once after all plans have been
                // executed so that the file is flagged as modified and all
                // views are recalculated
                if (isModified)
                    getClient().markDirty();
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
