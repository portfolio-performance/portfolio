package name.abuchen.portfolio.ui.util.viewers;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;

/**
 * Creates a cell editor with a combo box of Accounts. Options must be a list of non-null
 * values. By default, the user must choose one option. Override the method
 * {@link #canBeNull} in order to add an additional empty element
 */
public class TransactionOwnerListEditingSupport extends ListEditingSupport
{
    private Class<?> subjectType;
    private String attributeName;
    private Client client;

    public TransactionOwnerListEditingSupport(Client client, Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName, new ArrayList<Object>());
        this.subjectType = subjectType;
        this.attributeName = attributeName;
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    private Transaction getTransaction(Object element)
    {
        Transaction t;
        if (element instanceof Transaction)
            t = (Transaction) element;
        else if (element instanceof TransactionPair)
            t = (Transaction) ((TransactionPair<Transaction>) element).getTransaction();
        else
            throw new UnsupportedOperationException();
        return t;
    }

    @Override
    public boolean canEdit(Object element)
    {
        Transaction t = getTransaction(element);

        if (t.getCrossEntry() == null)
            return false;

        boolean canEdit = (adapt(t.getCrossEntry()) != null); 
        if (canEdit)
        {
            TransactionOwner<? extends Transaction> owner;
            if (attributeName.equals("primaryTransactionOwner")) //$NON-NLS-1$
                owner = t.getCrossEntry().getOwner(t);
            else if (attributeName.equals("secondaryTransactionOwner")) //$NON-NLS-1$
                owner = t.getCrossEntry().getCrossOwner(t);
            else
                throw new IllegalArgumentException();

            final TransactionOwner<? extends Transaction> skip;
            if (t.getCrossEntry().getOwner(t).getClass().equals(t.getCrossEntry().getCrossOwner(t).getClass()))
                skip  = t.getCrossEntry().getOwner(t);
            else
                skip  = null;

            List<?> options; 
            if (owner instanceof Account)
            {
                Account account = (Account) owner;
                options = client.getAccounts().stream().filter(a -> {return (skip != null?!skip.equals(a):true);}).filter(a -> account.getCurrencyCode().equals(a.getCurrencyCode())).collect(Collectors.toList());
            }
            else if (owner instanceof Portfolio)
                options = client.getPortfolios().stream().filter(p -> {return (skip != null?!skip.equals(p):true);}).collect(Collectors.toList());
            else
                throw new IllegalArgumentException();

            if (options.size() > 1)
                setComboBoxItems(new ArrayList<Object>(options));
            else
                return false;
        }
        return canEdit;
    }

    private String switchAttributeName(String attributeName)
    {
        if (attributeName.equals("primaryTransactionOwner")) //$NON-NLS-1$
            return "secondaryTransactionOwner"; //$NON-NLS-1$
        else if (attributeName.equals("secondaryTransactionOwner")) //$NON-NLS-1$
            return "primaryTransactionOwner"; //$NON-NLS-1$
        else
            throw new IllegalArgumentException();
    }
    
    private PropertyDescriptor getDescriptor(Transaction transaction)
    {
        if (transaction.getCrossEntry() instanceof BuySellEntry) 
        {
            if (transaction instanceof AccountTransaction)
                return descriptorFor(subjectType, switchAttributeName(attributeName));
            else if (transaction instanceof PortfolioTransaction)
                return descriptor();
            else
                throw new IllegalArgumentException();
        }
        else if (transaction.getCrossEntry() instanceof AccountTransferEntry)
        {
            if (((AccountTransaction) transaction).getType().equals(AccountTransaction.Type.TRANSFER_OUT))
                return descriptorFor(subjectType, switchAttributeName(attributeName));
            else if (((AccountTransaction) transaction).getType().equals(AccountTransaction.Type.TRANSFER_IN))
                return descriptor();
            else
                throw new IllegalArgumentException();
        }
        else if (transaction.getCrossEntry() instanceof PortfolioTransferEntry)
        {
            if (((PortfolioTransaction) transaction).getType().equals(PortfolioTransaction.Type.TRANSFER_OUT))
                return descriptorFor(subjectType, switchAttributeName(attributeName));
            else if (((PortfolioTransaction) transaction).getType().equals(PortfolioTransaction.Type.TRANSFER_IN))
                return descriptor();
            else
                throw new IllegalArgumentException();
        }
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        Transaction t = getTransaction(element);

        Object subject = adapt(t.getCrossEntry());
        PropertyDescriptor descriptor = getDescriptor(t);

        Object property = descriptor.getReadMethod().invoke(subject);

        List<Object> comboBoxItems = getComboBoxItems();
        for (int ii = 0; ii < comboBoxItems.size(); ii++)
        {
            Object item = comboBoxItems.get(ii);
            if (item != null && item.equals(property))
                return ii;
            else if (item == null && property == null)
                return ii;
        }

        return 0;
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        Transaction transaction = getTransaction(element);
        Object subject = adapt(transaction.getCrossEntry());
        TransactionOwner<Transaction> owner;
        TransactionOwner<Transaction> crossOwner;
        if (transaction.getCrossEntry().getOwner(transaction) instanceof TransactionOwner && transaction.getCrossEntry().getCrossOwner(transaction) instanceof TransactionOwner)
        {
            @SuppressWarnings("unchecked")
            TransactionOwner<Transaction> tmpOwner      = (TransactionOwner<Transaction>) transaction.getCrossEntry().getOwner(transaction);
            @SuppressWarnings("unchecked")
            TransactionOwner<Transaction> tmpCrossOwner = (TransactionOwner<Transaction>) transaction.getCrossEntry().getCrossOwner(transaction);
            owner      = tmpOwner;
            crossOwner = tmpCrossOwner;
        }
        else
            throw new IllegalArgumentException();
        
        int index = (Integer) value;
        if (index < 0)
            return;

        List<Object> comboBoxItems = getComboBoxItems();

        PropertyDescriptor descriptor = getDescriptor(transaction);

        Object newValue = comboBoxItems.get(index);
        Object oldValue = descriptor.getReadMethod().invoke(subject);

        if ((newValue != null && !newValue.equals(oldValue)) || (newValue == null && oldValue != null))
        {
            descriptor.getWriteMethod().invoke(subject, newValue);
            notify(transaction, newValue, oldValue);
            Transaction crossTransaction             = (Transaction) transaction.getCrossEntry().getCrossTransaction(transaction);
            owner.deleteTransaction(transaction, client);
            crossOwner.deleteTransaction(crossTransaction, client);
            transaction.getCrossEntry().insert();
        }
    }
}
