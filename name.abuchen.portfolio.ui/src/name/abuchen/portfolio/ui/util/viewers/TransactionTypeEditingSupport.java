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
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingField;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingPolicy;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.actions.ConvertBuySellToDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.ConvertDeliveryToBuySellAction;
import name.abuchen.portfolio.ui.views.actions.ConvertPortfolioCompositeTypeAction;
import name.abuchen.portfolio.ui.views.actions.RevertBuySellAction;
import name.abuchen.portfolio.ui.views.actions.RevertDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.RevertDepositRemovalAction;
import name.abuchen.portfolio.ui.views.actions.RevertFeeTaxAction;
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
                    new Class[] { ConvertPortfolioCompositeTypeAction.class },

                    PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.BUY,
                    new Class[] { RevertBuySellAction.class },

                    PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                    new Class[] { ConvertBuySellToDeliveryAction.class },

                    PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.DELIVERY_INBOUND,
                    new Class[] { ConvertPortfolioCompositeTypeAction.class },

                    PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                    new Class[] { RevertDeliveryAction.class },

                    PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.BUY,
                    new Class[] { ConvertDeliveryToBuySellAction.class },

                    PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.SELL,
                    new Class[] { ConvertPortfolioCompositeTypeAction.class },

                    PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.DELIVERY_INBOUND,
                    new Class[] { RevertDeliveryAction.class },

                    PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.SELL,
                    new Class[] { ConvertDeliveryToBuySellAction.class },

                    PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.BUY,
                    new Class[] { ConvertPortfolioCompositeTypeAction.class },

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
                    new Class[] { RevertInterestAction.class },

                    AccountTransaction.Type.FEES, AccountTransaction.Type.FEES_REFUND,
                    new Class[] { RevertFeeTaxAction.class },

                    AccountTransaction.Type.FEES_REFUND, AccountTransaction.Type.FEES,
                    new Class[] { RevertFeeTaxAction.class },

                    AccountTransaction.Type.TAXES, AccountTransaction.Type.TAX_REFUND,
                    new Class[] { RevertFeeTaxAction.class },

                    AccountTransaction.Type.TAX_REFUND, AccountTransaction.Type.TAXES,
                    new Class[] { RevertFeeTaxAction.class } };

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

        if (t instanceof AccountTransaction at)
            return new TransactionPair<>(lookupOwner(at), at);
        else if (t instanceof PortfolioTransaction pt)
            return new TransactionPair<>(lookupOwner(pt), pt);
        else
            throw new UnsupportedOperationException();
    }

    private Transaction getTransaction(Object element)
    {
        if (element instanceof Transaction tx)
            return tx;
        else if (element instanceof TransactionPair<?>)
            return ((TransactionPair<?>) element).getTransaction();
        else
            throw new UnsupportedOperationException();
    }

    private Enum<?> getTypeValue(Transaction t)
    {
        if (t instanceof AccountTransaction at)
            return at.getType();
        else if (t instanceof PortfolioTransaction pt)
            return pt.getType();
        else
            throw new IllegalArgumentException("unsupported transaction type " + t); //$NON-NLS-1$
    }

    @Override
    public boolean canEdit(Object element)
    {
        Transaction t = getTransaction(element);
        if (LedgerInlineEditingPolicy.isNativeTargetedProjection(t))
            return false;
        if (!LedgerInlineEditingPolicy.isEditable(t, LedgerInlineEditingField.TYPE))
            return false;

        Enum<?> type = getTypeValue(t);

        for (int ii = 0; ii < TRANSITIONS.length; ii += 3)
        {
            if (TRANSITIONS[ii] == type && supportsTransition(t, type, (Enum<?>) TRANSITIONS[ii + 1]))
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
            if (TRANSITIONS[ii] == type && supportsTransition(t, type, (Enum<?>) TRANSITIONS[ii + 1]))
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

        if (!LedgerInlineEditingPolicy.isEditable(t, LedgerInlineEditingField.TYPE))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_UI_001
                            .message(Messages.LedgerTransactionTypeEditingSupportPolicyBlockedTransition));

        if (!supportsTransition(t, oldValue, newValue))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_UI_002
                            .message(Messages.LedgerTransactionTypeEditingSupportUnsupportedTransition));

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

        throw new IllegalArgumentException("transition from " + fromValue + " to " + toValue + " not found"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private boolean supportsTransition(Transaction transaction, Enum<?> fromValue, Enum<?> toValue)
    {
        return LedgerInlineEditingPolicy.supportsTypeTransition(client, getTransactionPair(transaction), fromValue,
                        toValue);
    }

}
