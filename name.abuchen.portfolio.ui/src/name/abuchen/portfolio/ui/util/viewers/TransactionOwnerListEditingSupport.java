package name.abuchen.portfolio.ui.util.viewers;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingPolicy;
import name.abuchen.portfolio.ui.Messages;

/**
 * Creates a cell editor with a combo box of accounts or portfolios.
 */
public class TransactionOwnerListEditingSupport extends ColumnEditingSupport
{
    interface TriConsumer<T, U, V>
    {
        void accept(T t, U u, V v);
    }

    public enum EditMode
    {
        OWNER((e, t) -> e.getOwner(t), (e, t, o) -> e.setOwner(t, o)), //
        CROSSOWNER((e, t) -> e.getCrossOwner(t), (e, t, o) -> e.setOwner(e.getCrossTransaction(t), o));

        private final BiFunction<CrossEntry, Transaction, TransactionOwner<?>> getter;
        private final TriConsumer<CrossEntry, Transaction, TransactionOwner<?>> setter;

        private EditMode(BiFunction<CrossEntry, Transaction, TransactionOwner<?>> getter,
                        TriConsumer<CrossEntry, Transaction, TransactionOwner<?>> setter)
        {
            this.getter = getter;
            this.setter = setter;
        }

        @SuppressWarnings("unchecked")
        public TransactionOwner<Transaction> getOwner(CrossEntry crossEntry, Transaction transaction)
        {
            return (TransactionOwner<Transaction>) getter.apply(crossEntry, transaction);
        }

        public void setOwner(CrossEntry crossEntry, Transaction transaction, TransactionOwner<?> owner)
        {
            setter.accept(crossEntry, transaction, owner);
        }
    }

    private final Client client;
    private final EditMode editMode;

    private ComboBoxCellEditor editor;
    private List<TransactionOwner<?>> comboBoxItems;

    public TransactionOwnerListEditingSupport(Client client, EditMode editMode)
    {
        this.client = client;
        this.editMode = editMode;
    }

    private Transaction getTransaction(Object element)
    {
        if (element instanceof Transaction transaction)
            return transaction;
        else if (element instanceof TransactionPair<?> pair)
            return pair.getTransaction();
        else
            return null;
    }

    @Override
    public boolean canEdit(Object element)
    {
        Transaction transaction = getTransaction(element);
        if (LedgerInlineEditingPolicy.isNativeTargetedProjection(transaction))
            return false;

        return LedgerInlineEditingPolicy.canEditOwner(client, transaction);
    }

    @Override
    public final CellEditor createEditor(Composite composite)
    {
        editor = new ComboBoxCellEditor(composite, new String[0], SWT.READ_ONLY);
        return editor;
    }

    @Override
    public final void prepareEditor(Object element)
    {
        // fill drop down with accounts (only with the identical currency) or
        // portfolios.

        Transaction transaction = getTransaction(element);
        if (transaction == null)
            return;
        CrossEntry crossEntry = transaction.getCrossEntry();
        if (crossEntry == null)
            return;

        // Make sure that transfers do not happen from the one and the same
        // account or portfolio.
        final TransactionOwner<?> skipTransfer;
        if (crossEntry.getOwner(transaction).getClass().equals(crossEntry.getCrossOwner(transaction).getClass()))
        {
            switch (editMode)
            {
                case OWNER:
                    skipTransfer = crossEntry.getCrossOwner(transaction);
                    break;
                case CROSSOWNER:
                    skipTransfer = crossEntry.getOwner(transaction);
                    break;
                default:
                    throw new IllegalArgumentException("unsupported edit mode " + editMode); //$NON-NLS-1$
            }
        }
        else
        {
            skipTransfer = null;
        }

        TransactionOwner<?> ownerToEdit = editMode.getOwner(crossEntry, transaction);

        if (ownerToEdit instanceof Account account)
        {
            String ownerCurrencyCode = account.getCurrencyCode();
            comboBoxItems = client.getAccounts().stream().filter(a -> !a.equals(skipTransfer))
                            .filter(a -> a.getCurrencyCode().equals(ownerCurrencyCode)).sorted(new Account.ByName())
                            .collect(Collectors.toList());
        }
        else if (ownerToEdit instanceof Portfolio)
        {
            comboBoxItems = client.getPortfolios().stream().filter(p -> !p.equals(skipTransfer))
                            .sorted(new Portfolio.ByName())
                            .collect(Collectors.toList());
        }
        else
        {
            throw new IllegalArgumentException("unsupported type " + ownerToEdit); //$NON-NLS-1$
        }

        String[] names = new String[comboBoxItems.size()];
        int index = 0;

        for (Object item : comboBoxItems)
            names[index++] = item == null ? "" : item.toString(); //$NON-NLS-1$

        editor.setItems(names);
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        Transaction transaction = getTransaction(element);
        if (transaction == null)
            throw new IllegalArgumentException("no transaction found for " + element); //$NON-NLS-1$
        CrossEntry crossEntry = transaction.getCrossEntry();
        if (crossEntry == null)
            throw new IllegalArgumentException("no cross entry found for transaction " + transaction); //$NON-NLS-1$

        TransactionOwner<?> owner = editMode.getOwner(crossEntry, transaction);
        return comboBoxItems.indexOf(owner);
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        int index = (Integer) value;
        if (index < 0 || index >= comboBoxItems.size())
            return;

        Transaction transaction = getTransaction(element);
        if (transaction == null)
            throw new IllegalArgumentException("no transaction found for " + element); //$NON-NLS-1$
        CrossEntry crossEntry = transaction.getCrossEntry();
        if (crossEntry == null)
            throw new IllegalArgumentException("no cross entry found for transaction " + transaction); //$NON-NLS-1$

        TransactionOwner<?> newValue = comboBoxItems.get(index);
        TransactionOwner<?> oldValue = editMode.getOwner(crossEntry, transaction);

        if (newValue.equals(oldValue))
            return;

        validateOwnerChange(crossEntry, transaction, newValue);

        if (LedgerInlineEditingPolicy.isLedgerBacked(client, transaction))
        {
            if (!LedgerInlineEditingPolicy.updateOwner(client, transaction, oldValue, newValue))
                throw unsupportedLedgerOwnerEdit(crossEntry);
            notify(element, newValue, oldValue);
            return;
        }

        // since we are editing the transaction owner, we must first remove the
        // transaction and then re-insert it

        @SuppressWarnings("unchecked")
        TransactionOwner<Transaction> transactionOwner = (TransactionOwner<Transaction>) crossEntry.getOwner(transaction);
        transactionOwner.deleteTransaction(transaction, client);

        editMode.setOwner(crossEntry, transaction, newValue);
        crossEntry.insert();

        notify(element, newValue, oldValue);
    }

    private void validateOwnerChange(CrossEntry crossEntry, Transaction transaction, TransactionOwner<?> newValue)
    {
        TransactionOwner<?> ownerToEdit = editMode.getOwner(crossEntry, transaction);

        if (!ownerToEdit.getClass().isInstance(newValue))
            throw new IllegalArgumentException("unsupported owner type " + newValue); //$NON-NLS-1$

        if (ownerToEdit instanceof Account account && newValue instanceof Account newAccount
                        && !account.getCurrencyCode().equals(newAccount.getCurrencyCode()))
            throw new IllegalArgumentException("account owner changes require identical currencies"); //$NON-NLS-1$

        if (crossEntry.getOwner(transaction).getClass().equals(crossEntry.getCrossOwner(transaction).getClass()))
        {
            TransactionOwner<?> otherOwner = editMode == EditMode.OWNER ? crossEntry.getCrossOwner(transaction)
                            : crossEntry.getOwner(transaction);

            if (newValue.equals(otherOwner))
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_UI_012
                                .message(Messages.LedgerTransactionOwnerListEditingSupportDistinctOwnersRequired));
        }
    }

    private UnsupportedOperationException unsupportedLedgerOwnerEdit(CrossEntry crossEntry)
    {
        return new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_UI_013
                        .message(MessageFormat.format(
                                        Messages.LedgerTransactionOwnerListEditingSupportUnsupportedOwnerEdit,
                                        crossEntry)));
    }
}
