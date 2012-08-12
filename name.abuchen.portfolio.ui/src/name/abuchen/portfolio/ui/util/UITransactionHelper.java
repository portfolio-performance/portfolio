package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TransactionHelper;
import name.abuchen.portfolio.util.TransactionHelper.CounterTransaction;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

public class UITransactionHelper
{

    private static final class CounterTransactionLabelProvider extends LabelProvider
    {
        @Override
        public Image getImage(Object element)
        {
            CounterTransaction t = (CounterTransaction) element;
            if (t.getOwner() instanceof Account)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            else if (t.getOwner() instanceof Portfolio)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
            else
                return null;
        }

        @Override
        public String getText(Object element)
        {
            CounterTransaction t = (CounterTransaction) element;
            return t.getOwner().toString();
        }
    }

    private UITransactionHelper()
    {}

    public static boolean deleteCounterTransaction(Shell shell, Client client, Transaction transaction)
    {
        if (!TransactionHelper.hasCounterTransaction(transaction))
            return true;

        Enum<?> type = transaction instanceof PortfolioTransaction ? ((PortfolioTransaction) transaction).getType()
                        : ((AccountTransaction) transaction).getType();

        List<CounterTransaction> counterTransactions = TransactionHelper.findCounterTransaction(client, transaction);
        if (counterTransactions.isEmpty())
        {
            boolean doDeleteAnyway = MessageDialog.openConfirm(
                            shell,
                            Messages.DialogTitleCounterTransactionNotFound,
                            MessageFormat.format(Messages.MsgCounterTransactionNotFoundDeleteAnyway, type,
                                            transaction.getDate(), transaction.getAmount() / Values.Amount.divider()));
            return doDeleteAnyway;
        }
        else if (counterTransactions.size() > 1)
        {
            ListDialog dialog = new ListDialog(shell);
            dialog.setTitle(Messages.DialogTitleMultipleCounterTransactions);
            dialog.setMessage(MessageFormat.format(Messages.MsgPickOneOfMultipleCounterTransactions, type,
                            transaction.getDate(), transaction.getAmount() / Values.Amount.divider()));
            dialog.setContentProvider(ArrayContentProvider.getInstance());
            dialog.setLabelProvider(new CounterTransactionLabelProvider());
            dialog.setInput(counterTransactions);

            if (dialog.open() == ListDialog.CANCEL)
                return false;

            for (Object o : dialog.getResult())
                ((CounterTransaction) o).remove();

            return true;
        }
        else
        {
            counterTransactions.get(0).remove();
            return true;
        }
    }
}
