package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.views.actions.ConvertBuySellToDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.ConvertDeliveryToBuySellAction;
import name.abuchen.portfolio.ui.views.actions.RevertBuySellAction;
import name.abuchen.portfolio.ui.views.actions.RevertDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.RevertDepositRemovalAction;
import name.abuchen.portfolio.ui.views.actions.RevertInterestAction;
import name.abuchen.portfolio.ui.views.actions.RevertTransferAction;

/**
 * Creates a cell editor with a combo box of transactions types.
 */
public class TransactionTypeEditingSupport extends ColumnEditingSupport
{
    private static final Object[] TRANSITIONS = new Object[] { //
                    PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.SELL,
                    new Class[] { RevertBuySellAction.class },

                    PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.DELIVERY_INBOUND,
                    new Class[] { ConvertBuySellToDeliveryAction.class },

                    PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                    new Class[] { RevertBuySellAction.class, ConvertBuySellToDeliveryAction.class },

                    PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.BUY,
                    new Class[] { RevertBuySellAction.class },

                    PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                    new Class[] { ConvertBuySellToDeliveryAction.class },

                    PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.DELIVERY_INBOUND,
                    new Class[] { RevertBuySellAction.class, ConvertBuySellToDeliveryAction.class },

                    PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                    new Class[] { RevertDeliveryAction.class },

                    PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.BUY,
                    new Class[] { ConvertDeliveryToBuySellAction.class },

                    PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.SELL,
                    new Class[] { RevertDeliveryAction.class, ConvertDeliveryToBuySellAction.class },

                    PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.DELIVERY_INBOUND,
                    new Class[] { RevertDeliveryAction.class },

                    PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.SELL,
                    new Class[] { ConvertDeliveryToBuySellAction.class },

                    PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.BUY,
                    new Class[] { RevertDeliveryAction.class, ConvertDeliveryToBuySellAction.class },

                    AccountTransaction.Type.SELL, AccountTransaction.Type.BUY,
                    new Class[] { RevertBuySellAction.class },

                    AccountTransaction.Type.BUY, AccountTransaction.Type.SELL,
                    new Class[] { RevertBuySellAction.class },

                    AccountTransaction.Type.TRANSFER_IN, AccountTransaction.Type.TRANSFER_OUT,
                    new Class[] { RevertTransferAction.class },

                    AccountTransaction.Type.TRANSFER_OUT, AccountTransaction.Type.TRANSFER_IN,
                    new Class[] { RevertTransferAction.class },

                    AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL,
                    new Class[] { RevertDepositRemovalAction.class },

                    AccountTransaction.Type.REMOVAL, AccountTransaction.Type.DEPOSIT,
                    new Class[] { RevertDepositRemovalAction.class },

                    AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE,
                    new Class[] { RevertInterestAction.class },

                    AccountTransaction.Type.INTEREST_CHARGE, AccountTransaction.Type.INTEREST,
                    new Class[] { RevertInterestAction.class } };

    private final Client client;

    private ComboBoxCellEditor editor;
    private List<Object> comboBoxItems;

    public TransactionTypeEditingSupport(Client client)
    {
        this.client = client;
    }

    /**
     * Returns the owner of the transaction.
     */
    private Account lookupOwner(AccountTransaction t)
    {
        return client.getAccounts().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Returns the owner of the transaction.
     */
    private Portfolio lookupOwner(PortfolioTransaction t)
    {
        return client.getPortfolios().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    private TransactionPair<?> getTransactionPair(Object element)
    {
        if (element instanceof TransactionPair<?>)
            return (TransactionPair<?>) element;

        Transaction t = getTransaction(element);

        if (t instanceof AccountTransaction)
            return new TransactionPair<>(lookupOwner((AccountTransaction) t), (AccountTransaction) t);
        else if (t instanceof PortfolioTransaction)
            return new TransactionPair<>(lookupOwner((PortfolioTransaction) t), (PortfolioTransaction) t);
        else
            throw new UnsupportedOperationException();
    }

    private Transaction getTransaction(Object element)
    {
        if (element instanceof Transaction)
            return (Transaction) element;
        else if (element instanceof TransactionPair<?>)
            return ((TransactionPair<?>) element).getTransaction();
        else
            throw new UnsupportedOperationException();
    }

    private Enum<?> getTypeValue(Transaction t)
    {
        if (t instanceof AccountTransaction)
            return ((AccountTransaction) t).getType();
        else if (t instanceof PortfolioTransaction)
            return ((PortfolioTransaction) t).getType();
        else
            throw new IllegalArgumentException();
    }

    @Override
    public boolean canEdit(Object element)
    {
        Transaction t = getTransaction(element);
        Enum<?> type = getTypeValue(t);

        for (int ii = 0; ii < TRANSITIONS.length; ii += 3)
        {
            if (TRANSITIONS[ii] == type)
                return true;
        }

        return false;
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
        Transaction t = getTransaction(element);
        Enum<?> type = getTypeValue(t);

        comboBoxItems = new ArrayList<>();
        comboBoxItems.add(type);

        for (int ii = 0; ii < TRANSITIONS.length; ii += 3)
        {
            if (TRANSITIONS[ii] == type)
                comboBoxItems.add(TRANSITIONS[ii + 1]);
        }

        Collections.sort(comboBoxItems, (r, l) -> r.toString().compareTo(l.toString()));

        editor.setItems(comboBoxItems.stream().map(Object::toString).toArray(String[]::new));
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        Transaction t = getTransaction(element);
        Enum<?> type = getTypeValue(t);
        return comboBoxItems.indexOf(type);
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        int index = (Integer) value;
        if (index < 0 || index >= comboBoxItems.size())
            return;

        Transaction t = getTransaction(element);

        Enum<?> oldValue = getTypeValue(t);
        Enum<?> newValue = (Enum<?>) comboBoxItems.get(index);

        if (newValue.equals(oldValue))
            return;

        TransactionPair<?> pair = getTransactionPair(element);

        Class<?>[] transition = getTransition(oldValue, newValue);

        for (Class<?> action : transition)
            ((Action) action.getDeclaredConstructor(Client.class, TransactionPair.class) //
                            .newInstance(client, pair)).run();

        notify(element, newValue, oldValue);
    }

    private Class<?>[] getTransition(Enum<?> fromValue, Enum<?> toValue)
    {
        for (int ii = 0; ii < TRANSITIONS.length; ii += 3)
        {
            if (TRANSITIONS[ii] == fromValue && TRANSITIONS[ii + 1] == toValue)
                return (Class<?>[]) TRANSITIONS[ii + 2];
        }

        throw new IllegalArgumentException();
    }
}
