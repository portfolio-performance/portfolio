package name.abuchen.portfolio.ui.views;

import java.util.EnumSet;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Peer;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;

public class PeerContextMenu
{
    private AbstractFinanceView owner;

    public PeerContextMenu(AbstractFinanceView owner)
    {
        this.owner = owner;
    }

    public void menuAboutToShow(IMenuManager manager, final Peer peer, final Account account)
    {
        if (peer == null)
            return;

        for (final AccountTransaction.Type type : EnumSet.of( //
                        AccountTransaction.Type.DEPOSIT, //
                        AccountTransaction.Type.REMOVAL, //
                        AccountTransaction.Type.TAXES, //
                        AccountTransaction.Type.TAX_REFUND, //
                        AccountTransaction.Type.FEES, //
                        AccountTransaction.Type.FEES_REFUND, //
                        AccountTransaction.Type.INTEREST, //
                        AccountTransaction.Type.INTEREST_CHARGE))
        {
            new OpenDialogAction(owner, type.toString() + "...") //$NON-NLS-1$
                            .type(AccountTransactionDialog.class) //
                            .parameters(type) //
                            .with(account) //
                            .addTo(manager);
        }

        if (peer.isAccount())
        {
            manager.add(new Separator());

            new OpenDialogAction(owner, Messages.AccountMenuTransfer) //
                            .type(AccountTransferDialog.class) //
                            .with(account) //
                            .addTo(manager);
            
        }
        manager.add(new Separator());

    }
}
