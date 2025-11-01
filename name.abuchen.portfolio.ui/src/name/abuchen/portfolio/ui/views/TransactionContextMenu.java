package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
import name.abuchen.portfolio.model.Transaction;
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
import name.abuchen.portfolio.ui.views.actions.ConvertTransferToDepositRemovalAction;
import name.abuchen.portfolio.ui.views.actions.CreateRemovalForDividendAction;

public class TransactionContextMenu
{
    private final AbstractFinanceView owner;

    public TransactionContextMenu(AbstractFinanceView owner)
    {
        this.owner = owner;
    }

    public void menuAboutToShow(IMenuManager manager, boolean fullContextMenu, IStructuredSelection selection)
    {
        if (selection.isEmpty())
        {
            if (fullContextMenu)
                new SecurityContextMenu(owner).menuAboutToShow(manager, null, null);

            return;
        }

        // convert the selection to a list of unwrapped transaction pairs to
        // ensure that any downstream action does not work on the filtered
        // client

        // editing, copying, deletion is only possible, if the transaction has
        // not been created for calculation purposes

        @SuppressWarnings("unchecked")
        var txs = selection.stream().map(o -> ((TransactionPair<Transaction>) o).unwrap()).toList();

        if (txs.size() == 1)
        {
            var tx = txs.getFirst();

            tx.withAccountTransaction().ifPresent(t -> fillContextMenuAccountTx(manager, fullContextMenu, t));
            tx.withPortfolioTransaction().ifPresent(t -> fillContextMenuPortfolioTx(manager, fullContextMenu, t));

            manager.add(new Separator());
        }

        if (fullContextMenu)
        {
            fillContextMenuAccountTxList(manager, txs);
            fillContextMenuPortfolioTxList(manager, txs);
        }

        manager.add(new Separator());

        var deletableTx = txs.stream().filter(tx -> tx.getTransaction().getOriginalTransaction() == null).toList();

        var deleteAction = new SimpleAction(Messages.MenuTransactionDelete, a -> {
            for (var tx : deletableTx)
                tx.deleteTransaction(owner.getClient());
            owner.markDirty();
        });
        deleteAction.setEnabled(!deletableTx.isEmpty());
        manager.add(deleteAction);
    }

    public void handleEditKey(KeyEvent e, IStructuredSelection selection)
    {
        if (e.keyCode == 'e' && e.stateMask == SWT.MOD1)
        {
            if (selection.isEmpty())
                return;

            TransactionPair<?> tx = ((TransactionPair<?>) selection.getFirstElement()).unwrap();

            // do not edit derived transactions
            if (tx.getTransaction().getOriginalTransaction() != null)
                return;

            tx.withAccountTransaction().ifPresent(t -> createEditAccountTransactionAction(t).run());
            tx.withPortfolioTransaction().ifPresent(t -> createEditPortfolioTransactionAction(t).run());
        }
        if (e.keyCode == 'd' && e.stateMask == SWT.MOD1)
        {
            if (selection.isEmpty())
                return;

            TransactionPair<?> tx = ((TransactionPair<?>) selection.getFirstElement()).unwrap();

            // do not edit derived transactions
            if (tx.getTransaction().getOriginalTransaction() != null)
                return;

            tx.withAccountTransaction().ifPresent(t -> createCopyAccountTransactionAction(t).run());
            tx.withPortfolioTransaction().ifPresent(t -> createCopyPortfolioTransactionAction(t).run());
        }
    }

    private void fillContextMenuAccountTxList(IMenuManager manager, List<TransactionPair<Transaction>> selection)
    {
        var accountTxPairs = selection.stream() //
                        .filter(p -> ((TransactionPair<?>) p).getTransaction().getOriginalTransaction() == null)
                        .filter(p -> ((TransactionPair<?>) p).isAccountTransaction())
                        .map(p -> p.withAccountTransaction().get()) //
                        .toList();

        if (accountTxPairs.size() != selection.size())
            return;

        var transfers = accountTxPairs.stream().filter(p -> p.getTransaction().getOriginalTransaction() == null)
                        .filter(p -> p.getTransaction().getType() == AccountTransaction.Type.TRANSFER_IN
                                        || p.getTransaction().getType() == AccountTransaction.Type.TRANSFER_OUT)
                        .map(p -> p.getTransaction()).toList();

        if (transfers.size() == accountTxPairs.size())
        {
            manager.add(new ConvertTransferToDepositRemovalAction(owner.getClient(), transfers));
        }

        var dividends = accountTxPairs.stream().filter(p -> p.getTransaction().getOriginalTransaction() == null)
                        .filter(p -> p.getTransaction().getType() == AccountTransaction.Type.DIVIDENDS).toList();

        if (dividends.size() == accountTxPairs.size())
        {
            manager.add(new CreateRemovalForDividendAction(owner.getClient(), dividends));
        }
    }

    private void fillContextMenuPortfolioTxList(IMenuManager manager, List<TransactionPair<Transaction>> selection)
    {
        Collection<TransactionPair<PortfolioTransaction>> txCollection = new ArrayList<>(selection.size());
        Iterator<?> it = selection.iterator();
        while (it.hasNext())
        {
            TransactionPair<?> foo = (TransactionPair<?>) it.next();
            if (foo.getTransaction().getOriginalTransaction() == null)
                foo.withPortfolioTransaction().ifPresent(txCollection::add);
        }

        if (txCollection.size() != selection.size())
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
        }

        if (allDelivery)
        {
            manager.add(new ConvertDeliveryToBuySellAction(owner.getClient(), txCollection));
        }
    }

    private void fillContextMenuAccountTx(IMenuManager manager, boolean fullContextMenu,
                    TransactionPair<AccountTransaction> tx)
    {
        Action action = createEditAccountTransactionAction(tx);
        action.setAccelerator(SWT.MOD1 | 'E');
        action.setEnabled(tx.getTransaction().getOriginalTransaction() == null);
        manager.add(action);

        Action duplicateAction = createCopyAccountTransactionAction(tx);
        duplicateAction.setAccelerator(SWT.MOD1 | 'D');
        duplicateAction.setEnabled(tx.getTransaction().getOriginalTransaction() == null);
        manager.add(duplicateAction);

        if (fullContextMenu)
        {
            manager.add(new Separator());
            new AccountContextMenu(owner).menuAboutToShow(manager, (Account) tx.getOwner(),
                            tx.getTransaction().getSecurity());
        }
    }

    private void fillContextMenuPortfolioTx(IMenuManager manager, boolean fullContextMenu,
                    TransactionPair<PortfolioTransaction> tx)
    {
        PortfolioTransaction ptx = tx.getTransaction();

        Action editAction = createEditPortfolioTransactionAction(tx);
        editAction.setAccelerator(SWT.MOD1 | 'E');
        editAction.setEnabled(ptx.getOriginalTransaction() == null);
        manager.add(editAction);

        Action duplicateAction = createCopyPortfolioTransactionAction(tx);
        duplicateAction.setAccelerator(SWT.MOD1 | 'D');
        duplicateAction.setEnabled(ptx.getOriginalTransaction() == null);
        manager.add(duplicateAction);

        manager.add(new Separator());

        if (fullContextMenu)
            new SecurityContextMenu(owner).withoutAccelerator() //
                            .menuAboutToShow(manager, ptx.getSecurity(), (Portfolio) tx.getOwner());
        else
            manager.add(new BookmarkMenu(owner.getPart(), ptx.getSecurity()));
    }

    private Action createEditAccountTransactionAction(TransactionPair<AccountTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry entry)
        {
            return new OpenDialogAction(owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof AccountTransferEntry entry)
        {
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

    private Action createCopyAccountTransactionAction(TransactionPair<AccountTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry entry)
        {
            return new OpenDialogAction(owner, Messages.MenuDuplicateTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.presetBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof AccountTransferEntry entry)
        {
            return new OpenDialogAction(owner, Messages.MenuDuplicateTransaction) //
                            .type(AccountTransferDialog.class, d -> d.presetEntry(entry));
        }
        else
        {
            return new OpenDialogAction(owner, Messages.MenuDuplicateTransaction) //
                            .type(AccountTransactionDialog.class,
                                            d -> d.presetTransaction((Account) tx.getOwner(), tx.getTransaction())) //
                            .parameters(tx.getTransaction().getType());
        }
    }

    private Action createEditPortfolioTransactionAction(TransactionPair<PortfolioTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry entry)
        {
            return new OpenDialogAction(owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof PortfolioTransferEntry entry)
        {
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

    private Action createCopyPortfolioTransactionAction(TransactionPair<PortfolioTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry entry)
        {
            return new OpenDialogAction(owner, Messages.MenuDuplicateTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.presetBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof PortfolioTransferEntry entry)
        {
            return new OpenDialogAction(owner, Messages.MenuDuplicateTransaction) //
                            .type(SecurityTransferDialog.class, d -> d.presetEntry(entry));
        }
        else
        {
            return new OpenDialogAction(owner, Messages.MenuDuplicateTransaction) //
                            .type(SecurityTransactionDialog.class, d -> d.presetDeliveryTransaction(tx)) //
                            .parameters(tx.getTransaction().getType());
        }
    }
}
