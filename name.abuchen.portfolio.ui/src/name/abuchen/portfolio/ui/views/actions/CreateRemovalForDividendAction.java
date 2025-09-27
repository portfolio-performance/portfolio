package name.abuchen.portfolio.ui.views.actions;

import java.util.Collection;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;

public class CreateRemovalForDividendAction extends Action
{
    private final Client client;
    private final Collection<TransactionPair<AccountTransaction>> transactionList;

    public CreateRemovalForDividendAction(Client client,
                    Collection<TransactionPair<AccountTransaction>> transactionList)
    {
        this.client = client;
        this.transactionList = transactionList;

        setText(Messages.MenuCreateRemovalForDividendAction);
    }

    @Override
    public void run()
    {
        for (var pair : transactionList)
        {
            var transaction = pair.getTransaction();

            AccountTransaction tx = new AccountTransaction();
            tx.setType(AccountTransaction.Type.REMOVAL);
            tx.setCurrencyCode(transaction.getCurrencyCode());
            tx.setAmount(transaction.getAmount());
            tx.setDateTime(transaction.getDateTime());

            pair.getOwner().addTransaction(tx);
        }

        client.markDirty();
    }
}
