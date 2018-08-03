package name.abuchen.portfolio.ui.util.viewers;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.views.actions.ConvertBuySellToDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.ConvertDeliveryToBuySellAction;
import name.abuchen.portfolio.ui.views.actions.RevertBuySellAction;
import name.abuchen.portfolio.ui.views.actions.RevertDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.RevertDepositRemovalAction;
import name.abuchen.portfolio.ui.views.actions.RevertInterestAction;
import name.abuchen.portfolio.ui.views.actions.RevertTransferAction;

import org.eclipse.jface.action.Action;

/**
 * Creates a cell editor with a combo box of Accounts. Options must be a list of non-null
 * values. By default, the user must choose one option. Override the method
 * {@link #canBeNull} in order to add an additional empty element
 */
public class TypeListEditingSupport extends ListEditingSupport
{
    private Class<?> subjectType;
    private String attributeName;
    private Client client;

    public TypeListEditingSupport(Client client, Class<?> subjectType, String attributeName, List<?> options)
    {
        super(subjectType, attributeName, options);
        this.subjectType = subjectType;
        this.attributeName = attributeName;
        this.client = client;
    }

//    manager.add(new ConvertBuySellToDeliveryAction(owner.getClient(),
//                    new TransactionPair<>(lookupOwner(firstTransaction), firstTransaction)));
    /**
     * Returns the owner of the transaction. Because an investment plan can be
     * updated, older transactions do not necessarily belong to the account that
     * is currently configured for by the plan.
     */
    private Account lookupOwner(AccountTransaction t)
    {
        return client.getAccounts().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Returns the owner of the transaction. Because an investment plan can be
     * updated, older transactions do not necessarily belong to the portfolio
     * that is currently configured for the plan.
     */
    private Portfolio lookupOwner(PortfolioTransaction t)
    {
        return client.getPortfolios().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    private Transaction getTransaction(Object element)
    {
        Transaction t;
        if (element instanceof Transaction)
            t = (Transaction) element;
        else if (element instanceof TransactionPair<?>)
        {
                // we have made sure property is of class TransactionPair<?> so the cast is type safe
                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = (TransactionPair<Transaction>) element;
                t = (Transaction) pair.getTransaction();
        }
        else
            throw new UnsupportedOperationException();
        return t;
    }

    @Override
    public boolean canEdit(Object element)
    {
        System.err.println(">>>> TypeListEditingSupport::canEdit   class: " + element.getClass().toString()); //$NON-NLS-1$
        System.err.println("                                     element: " + element.toString()); //$NON-NLS-1$
        if (element instanceof PortfolioTransaction)
        {
            PortfolioTransaction t = (PortfolioTransaction) element;
            if (PortfolioTransaction.Type.BUY.equals(t.getType()) 
                            || PortfolioTransaction.Type.SELL.equals(t.getType())
                            || PortfolioTransaction.Type.DELIVERY_INBOUND.equals(t.getType())
                            || PortfolioTransaction.Type.DELIVERY_OUTBOUND.equals(t.getType())
                            )
            {
                List<PortfolioTransaction.Type> list = new ArrayList<PortfolioTransaction.Type>(Arrays.asList(
                                                                        PortfolioTransaction.Type.BUY, 
                                                                        PortfolioTransaction.Type.SELL, 
                                                                        PortfolioTransaction.Type.DELIVERY_INBOUND, 
                                                                        PortfolioTransaction.Type.DELIVERY_OUTBOUND
                                                                        ));
                setComboBoxItems(new ArrayList<Object>(list));                
                return true;
            }
            else if (PortfolioTransaction.Type.TRANSFER_IN.equals(t.getType()) 
                            || PortfolioTransaction.Type.TRANSFER_OUT.equals(t.getType())
                            )
            {
                List<PortfolioTransaction.Type> list = new ArrayList<PortfolioTransaction.Type>(Arrays.asList( 
                                PortfolioTransaction.Type.TRANSFER_IN, 
                                PortfolioTransaction.Type.TRANSFER_OUT
                                ));
                setComboBoxItems(new ArrayList<Object>(list));                
                return true;
            }
            else
                return false;
        }
        else if (element instanceof AccountTransaction)
        {
            AccountTransaction t = (AccountTransaction) element;
            if (AccountTransaction.Type.BUY.equals(t.getType()) || AccountTransaction.Type.SELL.equals(t.getType()))
            {
                List<AccountTransaction.Type> list = new ArrayList<AccountTransaction.Type>(Arrays.asList(
                                                                        AccountTransaction.Type.BUY, 
                                                                        AccountTransaction.Type.SELL
                                                                        ));
                setComboBoxItems(new ArrayList<Object>(list));                
                return true;
            }
            else if (AccountTransaction.Type.TRANSFER_IN.equals(t.getType()) || AccountTransaction.Type.TRANSFER_OUT.equals(t.getType()))
            {
                List<AccountTransaction.Type> list = new ArrayList<AccountTransaction.Type>(Arrays.asList(
                                                                        AccountTransaction.Type.TRANSFER_IN,
                                                                        AccountTransaction.Type.TRANSFER_OUT
                                                                        ));
                setComboBoxItems(new ArrayList<Object>(list));                
                return true;
            } 
            else if (AccountTransaction.Type.DEPOSIT.equals(t.getType()) || AccountTransaction.Type.REMOVAL.equals(t.getType()))
            {
                List<AccountTransaction.Type> list = new ArrayList<AccountTransaction.Type>(Arrays.asList(
                                                                        AccountTransaction.Type.DEPOSIT,
                                                                        AccountTransaction.Type.REMOVAL
                                                                        ));
                setComboBoxItems(new ArrayList<Object>(list));                
                return true;
            } 
            else if (AccountTransaction.Type.INTEREST.equals(t.getType()) || AccountTransaction.Type.INTEREST_CHARGE.equals(t.getType()))
            {
                List<AccountTransaction.Type> list = new ArrayList<AccountTransaction.Type>(Arrays.asList(
                                                                        AccountTransaction.Type.INTEREST,
                                                                        AccountTransaction.Type.INTEREST_CHARGE
                                                                        ));
                setComboBoxItems(new ArrayList<Object>(list));                
                return true;
            } 
            else
                return false;
        }
        else
            return false;
    }

    @SuppressWarnings("nls")
    private String switchAttributeName(String attributeName)
    {
        if (attributeName.equals("transactionOwner"))
            return "otherTransactionOwner";
        else if (attributeName.equals("otherTransactionOwner"))
            return "transactionOwner";
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

        System.err.println(">>>> TypeListEditingSupport::getvalue   class: " + element.getClass().toString());
        System.err.println("                                         element: " + element.toString());
//        Transaction t = getTransaction(element);
        Object subject = adapt(element);
        System.err.println("                                   attributeName: " + attributeName);
        System.err.println("                                      descriptor: " + descriptor().toString());
        System.err.println("                                      readMethod: " + descriptor().getReadMethod().toString());
        System.err.println("                                     writeMethod: " + descriptor().getWriteMethod().toString());
        if (subject != null)
            System.err.println("                                         subject: " + subject.toString());
        else
            return 0;
        
        Object property = descriptor().getReadMethod().invoke(subject);

        List<Object> comboBoxItems = getComboBoxItems();
        System.err.println("<<<< TypeListEditingSupport::getvalue");        
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
        System.err.println(">>>> TypeListEditingSupport::setvalue      class: " + element.getClass().toString());
        System.err.println("                                         element: " + element.toString());
        Object subject = adapt(element);
        System.err.println("     TypeListEditingSupport::setvalue subject: " + subject.toString());
        System.err.println("                                   attributeName: " + attributeName);
//        System.err.println("                              PRE  owner        : " + transaction.getCrossEntry().getOwner(transaction).toString());
//        System.err.println("                              PRE  crossowner   : " + transaction.getCrossEntry().getCrossOwner(transaction).toString());
//        TransactionOwner<Transaction> owner      = (TransactionOwner<Transaction>) transaction.getCrossEntry().getOwner(transaction);
//        TransactionOwner<Transaction> crossOwner = (TransactionOwner<Transaction>) transaction.getCrossEntry().getCrossOwner(transaction);

        int index = (Integer) value;
        if (index < 0)
            return;

        List<Object> comboBoxItems = getComboBoxItems();

        System.err.println("                                      descriptor: " + descriptor().toString());
        System.err.println("                                      readMethod: " + descriptor().getReadMethod().toString());
        System.err.println("                                     writeMethod: " + descriptor().getWriteMethod().toString());

        Object newValue = comboBoxItems.get(index);
        Object oldValue = descriptor().getReadMethod().invoke(subject);
        System.err.println("     TypeListEditingSupport::setValue  oldValue    : " + oldValue.toString());
        System.err.println("     TypeListEditingSupport::setValue  newValue    : " + newValue.toString());

        List<Action> actions = new ArrayList<Action>();
        
        if ((newValue != null && !newValue.equals(oldValue)) || (newValue == null && oldValue != null))
        {
            if (element instanceof PortfolioTransaction)
            {
                PortfolioTransaction transaction = (PortfolioTransaction) getTransaction(element);
                System.err.println("               portfolio             transaction: " + transaction.toString());
                System.err.println("               portfolio             transaction: " + transaction.toString());
                if (transaction.getCrossEntry() != null)
                {
                    System.err.println("               portfolio             cross-entry: " + transaction.getCrossEntry().toString());
                    System.err.println("               portfolio       cross-transaction: " + transaction.getCrossEntry().getCrossTransaction(transaction).toString());
                }
                else
                    System.err.println("               portfolio             cross-entry: <NULL>");
                TransactionPair<PortfolioTransaction> pair = new TransactionPair<PortfolioTransaction>(lookupOwner(transaction), transaction);
                System.err.println("     TypeListEditingSupport::setValue   POST   pair-trans  : " + pair.getTransaction().toString());
                System.err.println("     TypeListEditingSupport::setValue   POST   pair-owner  : " + pair.getOwner().toString());
                if (oldValue.equals(PortfolioTransaction.Type.DELIVERY_INBOUND) || oldValue.equals(PortfolioTransaction.Type.DELIVERY_OUTBOUND))
                {
                    if (newValue.equals(PortfolioTransaction.Type.DELIVERY_INBOUND) || newValue.equals(PortfolioTransaction.Type.DELIVERY_OUTBOUND))
                    {
                        actions.add(new RevertDeliveryAction(client, pair));                        
                    }
                    else if (newValue.equals(PortfolioTransaction.Type.SELL) || newValue.equals(PortfolioTransaction.Type.BUY))
                    {
                        if ((oldValue.equals(PortfolioTransaction.Type.DELIVERY_INBOUND) && newValue.equals(PortfolioTransaction.Type.SELL)) ||
                                        (oldValue.equals(PortfolioTransaction.Type.DELIVERY_OUTBOUND) && newValue.equals(PortfolioTransaction.Type.BUY)))
                            actions.add(new RevertDeliveryAction(client, pair));                        
                        actions.add(new ConvertDeliveryToBuySellAction(client, pair));   
                    }                    
                    else
                        throw new IllegalArgumentException();
                }
                else if (oldValue.equals(PortfolioTransaction.Type.TRANSFER_IN) || oldValue.equals(PortfolioTransaction.Type.TRANSFER_OUT))
                {
                    if (newValue.equals(PortfolioTransaction.Type.TRANSFER_IN) || newValue.equals(PortfolioTransaction.Type.TRANSFER_OUT))
                    {
                        actions.add(new RevertTransferAction(client, pair));                        
                    }                    
                    else
                        throw new IllegalArgumentException();
                }
                else if (oldValue.equals(PortfolioTransaction.Type.SELL) || oldValue.equals(PortfolioTransaction.Type.BUY))
                {
                    if (newValue.equals(PortfolioTransaction.Type.DELIVERY_INBOUND) || newValue.equals(PortfolioTransaction.Type.DELIVERY_OUTBOUND))
                    {
                        if ((oldValue.equals(PortfolioTransaction.Type.SELL) && newValue.equals(PortfolioTransaction.Type.DELIVERY_INBOUND)) ||
                                        (oldValue.equals(PortfolioTransaction.Type.BUY) && newValue.equals(PortfolioTransaction.Type.DELIVERY_OUTBOUND)))
                            actions.add(new RevertBuySellAction(client, pair));                        
                        actions.add(new ConvertBuySellToDeliveryAction(client, pair));   
                    }
                    else if (newValue.equals(PortfolioTransaction.Type.SELL) || newValue.equals(PortfolioTransaction.Type.BUY))
                    {
                        actions.add(new RevertBuySellAction(client, pair));                        
                    }                    
                    else
                        throw new IllegalArgumentException();
                }
                else
                    throw new IllegalArgumentException();
            }
            else if (element instanceof AccountTransaction)
            {
                AccountTransaction transaction = (AccountTransaction) getTransaction(element);
                System.err.println("             account                 transaction: " + transaction.toString());
                System.err.println("             account                 transaction: " + transaction.toString());
                if (transaction.getCrossEntry() != null)
                {
                    System.err.println("               portfolio             cross-entry: " + transaction.getCrossEntry().toString());
                    System.err.println("             account           cross-transaction: " + transaction.getCrossEntry().getCrossTransaction(transaction).toString());
                }
                else
                {
                    System.err.println("               portfolio             cross-entry: <NULL>");
                }
                TransactionPair<AccountTransaction> pair = new TransactionPair<AccountTransaction>(lookupOwner(transaction), transaction);
                System.err.println("     TypeListEditingSupport::setValue   POST   pair-trans  : " + pair.getTransaction().toString());
                System.err.println("     TypeListEditingSupport::setValue   POST   pair-owner  : " + pair.getOwner().toString());
                if (oldValue.equals(AccountTransaction.Type.SELL) || oldValue.equals(AccountTransaction.Type.BUY))
                {
                    if (newValue.equals(AccountTransaction.Type.SELL) || newValue.equals(AccountTransaction.Type.BUY))
                    {
                        actions.add(new RevertBuySellAction(client, pair));                        
                    }                    
                    else
                        throw new IllegalArgumentException();
                }
                else if (oldValue.equals(AccountTransaction.Type.TRANSFER_IN) || oldValue.equals(AccountTransaction.Type.TRANSFER_OUT))
                {
                    if (newValue.equals(AccountTransaction.Type.TRANSFER_IN) || newValue.equals(AccountTransaction.Type.TRANSFER_OUT))
                    {
                        actions.add(new RevertTransferAction(client, pair));                        
                    }                    
                    else
                        throw new IllegalArgumentException();
                }
                else if (oldValue.equals(AccountTransaction.Type.DEPOSIT) || oldValue.equals(AccountTransaction.Type.REMOVAL))
                {
                    if (newValue.equals(AccountTransaction.Type.DEPOSIT) || newValue.equals(AccountTransaction.Type.REMOVAL))
                    {
                        actions.add(new RevertDepositRemovalAction(client, pair));                        
                    }                    
                    else
                        throw new IllegalArgumentException();
                }
                else if (oldValue.equals(AccountTransaction.Type.INTEREST) || oldValue.equals(AccountTransaction.Type.INTEREST_CHARGE))
                {
                    if (newValue.equals(AccountTransaction.Type.INTEREST) || newValue.equals(AccountTransaction.Type.INTEREST_CHARGE))
                    {
                        actions.add(new RevertInterestAction(client, pair));                        
                    }                    
                    else
                        throw new IllegalArgumentException();
                }
                else
                    throw new IllegalArgumentException();
            }
            if (!actions.isEmpty())
            {
                for (Action action : actions)
                {
                    System.err.println("     TypeListEditingSupport::setValue     actions: " + action.toString());
                    action.run();
                }
            }
            
//            descriptor.getWriteMethod().invoke(subject, newValue);
//            notify(transaction, newValue, oldValue);
//            Transaction crossTransaction             = (Transaction) transaction.getCrossEntry().getCrossTransaction(transaction);
//            owner.deleteTransaction(transaction, client);
//            System.err.println("     TypeListEditingSupport::setValue     cross-transaction: " + crossTransaction.toString());
//            crossOwner.deleteTransaction(crossTransaction, client);
//            transaction.getCrossEntry().insert();
//            System.err.println("<<<< TypeListEditingSupport::setValue     cross-transaction: " + crossTransaction.toString());
        }
        System.err.println("<<<< TypeListEditingSupport::setValue");        
    }
}
