package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.TransactionHelper;
import name.abuchen.portfolio.util.TransactionHelper.CounterTransaction;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class UITransactionHelper
{

    private UITransactionHelper()
    {}

    public static boolean deleteCounterTransaction(Shell shell, Client client, Transaction transaction)
    {
        if (!TransactionHelper.hasCounterTransaction(transaction))
            return true;

        CounterTransaction counterTransaction = TransactionHelper.findCounterTransaction(client, transaction);
        if (counterTransaction == null)
        {
            Enum<?> type = transaction instanceof PortfolioTransaction ? ((PortfolioTransaction) transaction).getType()
                            : ((AccountTransaction) transaction).getType();
            boolean doDeleteAnyway = MessageDialog.openConfirm(
                            shell,
                            Messages.DialogTitleCounterTransactionNotFound,
                            MessageFormat.format(Messages.MsgCounterTransactionNotFoundDeleteAnyway, type,
                                            transaction.getDate(), transaction.getAmount() / Values.Amount.divider()));
            return doDeleteAnyway;
        }
        else
        {
            counterTransaction.remove();
            return true;
        }
    }
}
