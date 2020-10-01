package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.BookmarkMenu;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.actions.ConvertBuySellToDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.ConvertDeliveryToBuySellAction;

public class TransactionContextMenu
{
    private final AbstractFinanceView owner;

    public TransactionContextMenu(AbstractFinanceView owner)
    {
        this.owner = owner;
    }

    public void menuAboutToShow(IMenuManager manager, boolean fullContextMenu, IStructuredSelection selection)
    {
        if (selection.isEmpty() && fullContextMenu)
        {
            new SecurityContextMenu(owner).menuAboutToShow(manager, null, null);
        }

        if (selection.size() == 1)
        {
            TransactionPair<?> tx = (TransactionPair<?>) selection.getFirstElement();

            tx.withAccountTransaction().ifPresent(t -> fillContextMenuAccountTx(manager, fullContextMenu, t));
            tx.withPortfolioTransaction().ifPresent(t -> fillContextMenuPortfolioTx(manager, fullContextMenu, t));

            manager.add(new Separator());
        }

        if (!selection.isEmpty())
        {
            if (fullContextMenu)
                fillContextMenuPortfolioTxList(manager, selection);

            manager.add(new SimpleAction(Messages.MenuTransactionDelete, a -> {
                for (Object tx : selection.toArray())
                    ((TransactionPair<?>) tx).deleteTransaction(owner.getClient());

                owner.markDirty();
            }));
        }
    }

    public void handleEditKey(KeyEvent e, IStructuredSelection selection)
    {
        if (e.keyCode == 'e' && e.stateMask == SWT.MOD1)
        {
            if (selection.isEmpty())
                return;

            TransactionPair<?> tx = (TransactionPair<?>) selection.getFirstElement();
            tx.withAccountTransaction().ifPresent(t -> createEditAccountTransactionAction(t).run());
            tx.withPortfolioTransaction().ifPresent(t -> createEditPortfolioTransactionAction(t).run());
        }
    }

    private void fillContextMenuPortfolioTxList(IMenuManager manager, IStructuredSelection selection)
    {
        Collection<TransactionPair<PortfolioTransaction>> txCollection = new ArrayList<>(selection.size());
        Iterator<?> it = selection.iterator();
        while (it.hasNext())
        {
            TransactionPair<?> foo = (TransactionPair<?>) it.next();
            foo.withPortfolioTransaction().ifPresent(txCollection::add);
        }

        if (txCollection.isEmpty())
            return;

        boolean allBuyOrSellType = true;
        boolean allDelivery = true;

        for (TransactionPair<PortfolioTransaction> tx : txCollection)
        {
            PortfolioTransaction ptx = tx.getTransaction();

            allBuyOrSellType &= ptx.getType() == PortfolioTransaction.Type.BUY
                            || ptx.getType() == PortfolioTransaction.Type.SELL;

            allDelivery &= ptx.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                            || ptx.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
        }

        if (allBuyOrSellType)
        {
            manager.add(new ConvertBuySellToDeliveryAction(owner.getClient(), txCollection));
            manager.add(new Separator());
        }

        if (allDelivery)
        {
            manager.add(new ConvertDeliveryToBuySellAction(owner.getClient(), txCollection));
            manager.add(new Separator());
        }
    }

    private void fillContextMenuAccountTx(IMenuManager manager, boolean fullContextMenu, TransactionPair<AccountTransaction> tx)
    {
        Action action = createEditAccountTransactionAction(tx);
        action.setAccelerator(SWT.MOD1 | 'E');
        manager.add(action);

        if (fullContextMenu)
        {
            manager.add(new Separator());
            new AccountContextMenu(owner).menuAboutToShow(manager, (Account) tx.getOwner(),
                            tx.getTransaction().getSecurity());
        }
    }

    private void fillContextMenuPortfolioTx(IMenuManager manager, boolean fullContextMenu, TransactionPair<PortfolioTransaction> tx)
    {
        PortfolioTransaction ptx = tx.getTransaction();

        Action editAction = createEditPortfolioTransactionAction(tx);
        editAction.setAccelerator(SWT.MOD1 | 'E');
        manager.add(editAction);
        manager.add(new Separator());

        if (fullContextMenu)
            new SecurityContextMenu(owner).menuAboutToShow(manager, ptx.getSecurity(), (Portfolio) tx.getOwner());
        else
            manager.add(new BookmarkMenu(owner.getPart(), ptx.getSecurity()));
    }

    private Action createEditAccountTransactionAction(TransactionPair<AccountTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof AccountTransferEntry)
        {
            AccountTransferEntry entry = (AccountTransferEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(owner, Messages.MenuEditTransaction) //
                            .type(AccountTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(owner, Messages.MenuEditTransaction) //
                            .type(AccountTransactionDialog.class,
                                            d -> d.setTransaction((Account) tx.getOwner(), tx.getTransaction())) //
                            .parameters(tx.getTransaction().getType());
        }
    }

    private Action createEditPortfolioTransactionAction(TransactionPair<PortfolioTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof PortfolioTransferEntry)
        {
            PortfolioTransferEntry entry = (PortfolioTransferEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(owner, Messages.MenuEditTransaction) //
                            .type(SecurityTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(owner, Messages.MenuEditTransaction) //
                            .type(SecurityTransactionDialog.class, d -> d.setDeliveryTransaction(tx)) //
                            .parameters(tx.getTransaction().getType());
        }
    }
}
