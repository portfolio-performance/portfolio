package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerNativeComponentInspectorModel;
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
import name.abuchen.portfolio.ui.views.actions.LedgerNativeComponentInspectorAction;

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
            new SecurityContextMenu(owner).menuAboutToShow(manager, List.of(), null);
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
            {
                fillContextMenuAccountTxList(manager, selection);
                fillContextMenuPortfolioTxList(manager, selection);
            }

            manager.add(new Separator());

            if (!containsLedgerNativeTargetedProjection(selection))
            {
                manager.add(new SimpleAction(Messages.MenuTransactionDelete, a -> {
                    deleteTransactions(owner.getClient(), selection.toArray());

                    owner.markDirty();
                }));
            }
        }
    }

    static void deleteTransactions(Client client, Object[] selectedTransactions)
    {
        Set<String> deletedLedgerEntryUUIDs = new HashSet<>();
        Set<String> deletedTransactionUUIDs = new HashSet<>();

        for (Object item : selectedTransactions)
        {
            TransactionPair<?> pair = (TransactionPair<?>) item;
            var transaction = pair.getTransaction();
            var ledgerEntryUUID = pair.getLedgerEntryUUID();

            if (ledgerEntryUUID.isPresent())
            {
                if (!deletedLedgerEntryUUIDs.add(ledgerEntryUUID.get()))
                    continue;

                pair.deleteTransaction(client);
                continue;
            }

            if (!deletedTransactionUUIDs.add(transaction.getUUID()))
                continue;

            CrossEntry crossEntry = transaction.getCrossEntry();
            if (crossEntry != null)
            {
                var crossTransaction = crossEntry.getCrossTransaction(transaction);

                if (crossTransaction != null)
                    deletedTransactionUUIDs.add(crossTransaction.getUUID());
            }

            pair.deleteTransaction(client);
        }
    }

    public void handleEditKey(KeyEvent e, IStructuredSelection selection)
    {
        if (e.keyCode == 'e' && e.stateMask == SWT.MOD1)
        {
            if (selection.isEmpty())
                return;

            TransactionPair<?> tx = (TransactionPair<?>) selection.getFirstElement();
            if (isLedgerNativeTargetedProjection(tx))
                return;

            tx.withAccountTransaction().ifPresent(t -> createEditAccountTransactionAction(t).run());
            tx.withPortfolioTransaction().ifPresent(t -> createEditPortfolioTransactionAction(t).run());
        }
        if (e.keyCode == 'd' && e.stateMask == SWT.MOD1)
        {
            if (selection.isEmpty())
                return;

            TransactionPair<?> tx = (TransactionPair<?>) selection.getFirstElement();
            if (isLedgerNativeTargetedProjection(tx))
                return;

            tx.withAccountTransaction().ifPresent(t -> createCopyAccountTransactionAction(t).run());
            tx.withPortfolioTransaction().ifPresent(t -> createCopyPortfolioTransactionAction(t).run());
        }
    }

    private void fillContextMenuAccountTxList(IMenuManager manager, IStructuredSelection selection)
    {
        @SuppressWarnings("unchecked")
        var accountTxPairs = selection.stream() //
                        .filter(p -> ((TransactionPair<?>) p).isAccountTransaction())
                        .map(p -> ((TransactionPair<AccountTransaction>) p)) //
                        .toList();

        if (accountTxPairs.size() != selection.size())
            return;

        if (accountTxPairs.stream().anyMatch(TransactionContextMenu::isLedgerNativeTargetedProjection))
            return;

        var transfers = accountTxPairs.stream()
                        .filter(p -> p.getTransaction().getType() == AccountTransaction.Type.TRANSFER_IN
                                        || p.getTransaction().getType() == AccountTransaction.Type.TRANSFER_OUT)
                        .map(p -> p.getTransaction()).toList();

        if (transfers.size() == accountTxPairs.size())
        {
            manager.add(new ConvertTransferToDepositRemovalAction(owner.getClient(), transfers));
        }

        var dividends = accountTxPairs.stream()
                        .filter(p -> p.getTransaction().getType() == AccountTransaction.Type.DIVIDENDS).toList();

        if (dividends.size() == accountTxPairs.size())
        {
            manager.add(new CreateRemovalForDividendAction(owner.getClient(), dividends));
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

        if (txCollection.size() != selection.size())
            return;

        if (txCollection.stream().anyMatch(TransactionContextMenu::isLedgerNativeTargetedProjection))
            return;

        if (supportsBuySellToDeliveryAction(txCollection))
        {
            manager.add(new ConvertBuySellToDeliveryAction(owner.getClient(), txCollection));
        }

        if (supportsDeliveryToBuySellAction(txCollection))
        {
            manager.add(new ConvertDeliveryToBuySellAction(owner.getClient(), txCollection));
        }
    }

    static boolean supportsBuySellToDeliveryAction(Collection<TransactionPair<PortfolioTransaction>> txCollection)
    {
        return !txCollection.isEmpty() && txCollection.stream().noneMatch(TransactionContextMenu::isLedgerNativeTargetedProjection)
                        && txCollection.stream().allMatch(tx -> {
            var type = tx.getTransaction().getType();
            return type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL;
        });
    }

    static boolean supportsDeliveryToBuySellAction(Collection<TransactionPair<PortfolioTransaction>> txCollection)
    {
        return !txCollection.isEmpty() && txCollection.stream().noneMatch(TransactionContextMenu::isLedgerNativeTargetedProjection)
                        && txCollection.stream().allMatch(tx -> {
            var type = tx.getTransaction().getType();
            return type == PortfolioTransaction.Type.DELIVERY_INBOUND
                            || type == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
        });
    }

    private void fillContextMenuAccountTx(IMenuManager manager, boolean fullContextMenu,
                    TransactionPair<AccountTransaction> tx)
    {
        LedgerNativeComponentInspectorAction.create(owner, tx.getTransaction()).ifPresent(manager::add);

        if (isLedgerNativeTargetedProjection(tx))
            return;

        Action action = createEditAccountTransactionAction(tx);
        action.setAccelerator(SWT.MOD1 | 'E');
        manager.add(action);

        Action duplicateAction = createCopyAccountTransactionAction(tx);
        duplicateAction.setAccelerator(SWT.MOD1 | 'D');
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

        LedgerNativeComponentInspectorAction.create(owner, tx.getTransaction()).ifPresent(manager::add);

        if (isLedgerNativeTargetedProjection(tx))
            return;

        Action editAction = createEditPortfolioTransactionAction(tx);
        editAction.setAccelerator(SWT.MOD1 | 'E');
        manager.add(editAction);

        Action duplicateAction = createCopyPortfolioTransactionAction(tx);
        duplicateAction.setAccelerator(SWT.MOD1 | 'D');
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

    static boolean containsLedgerNativeTargetedProjection(IStructuredSelection selection)
    {
        return selection.stream() //
                        .filter(TransactionPair.class::isInstance) //
                        .map(TransactionPair.class::cast) //
                        .anyMatch(TransactionContextMenu::isLedgerNativeTargetedProjection);
    }

    static boolean isLedgerNativeTargetedProjection(TransactionPair<?> tx)
    {
        return LedgerNativeComponentInspectorModel.isLedgerNativeTargetedProjection(tx.getTransaction());
    }

}
