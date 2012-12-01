package name.abuchen.portfolio.util;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

public class TransactionHelper
{
    public static class CounterTransaction
    {
        private final Object owner;
        private final Transaction transaction;

        private CounterTransaction(Object owner, Transaction transaction)
        {
            this.owner = owner;
            this.transaction = transaction;
        }

        public Object getOwner()
        {
            return owner;
        }

        public Transaction getTransaction()
        {
            return transaction;
        }

        public void remove()
        {
            if (owner instanceof Portfolio)
                ((Portfolio) owner).getTransactions().remove(transaction);
            else if (owner instanceof Account)
                ((Account) owner).getTransactions().remove(transaction);
        }
    }

    private TransactionHelper()
    {}

    public static boolean hasCounterTransaction(Transaction t)
    {
        if (t instanceof AccountTransaction)
            return hasCounterTransaction((AccountTransaction) t);
        else if (t instanceof PortfolioTransaction)
            return hasCounterTransaction((PortfolioTransaction) t);
        else
            throw new UnsupportedOperationException("Unsupport transaction " + t.getClass().getName()); //$NON-NLS-1$
    }

    private static boolean hasCounterTransaction(AccountTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
            case SELL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
                return true;
            default:
                return false;
        }
    }

    private static boolean hasCounterTransaction(PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
            case SELL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
                return true;
            default:
                return false;
        }
    }

    public static List<CounterTransaction> findCounterTransaction(Client client, Transaction t)
    {
        if (t instanceof AccountTransaction)
            return findCounterTransaction(client, (AccountTransaction) t);
        else if (t instanceof PortfolioTransaction)
            return findCounterTransaction(client, (PortfolioTransaction) t);
        else
            throw new UnsupportedOperationException("Unsupport transaction " + t.getClass().getName()); //$NON-NLS-1$
    }

    private static List<CounterTransaction> findCounterTransaction(Client client, AccountTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
                return findCounterTransactionInPortfolio(client, t, PortfolioTransaction.Type.BUY);
            case SELL:
                return findCounterTransactionInPortfolio(client, t, PortfolioTransaction.Type.SELL);
            case TRANSFER_IN:
                return findCounterTransactionInAccounts(client, t, AccountTransaction.Type.TRANSFER_OUT);
            case TRANSFER_OUT:
                return findCounterTransactionInAccounts(client, t, AccountTransaction.Type.TRANSFER_IN);
            default:
                throw new UnsupportedOperationException("Unsupport transaction type " + t.getType()); //$NON-NLS-1$
        }
    }

    private static List<CounterTransaction> findCounterTransaction(Client client, PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
                return findCounterTransactionInAccounts(client, t, AccountTransaction.Type.BUY);
            case SELL:
                return findCounterTransactionInAccounts(client, t, AccountTransaction.Type.SELL);
            case TRANSFER_IN:
                return findCounterTransactionInPortfolio(client, t, PortfolioTransaction.Type.TRANSFER_OUT);
            case TRANSFER_OUT:
                return findCounterTransactionInPortfolio(client, t, PortfolioTransaction.Type.TRANSFER_IN);
            default:
                throw new UnsupportedOperationException("Unsupport transaction type " + t.getType()); //$NON-NLS-1$
        }
    }

    private static List<CounterTransaction> findCounterTransactionInPortfolio(Client client, Transaction t,
                    PortfolioTransaction.Type type)
    {
        List<CounterTransaction> answer = new ArrayList<CounterTransaction>();
        for (Portfolio p : client.getPortfolios())
        {
            for (PortfolioTransaction tp : p.getTransactions())
            {
                if (tp.getType() != type)
                    continue;

                if (!tp.getDate().equals(t.getDate()))
                    continue;

                if (tp.getSecurity() != t.getSecurity())
                    continue;

                if (tp.getAmount() != t.getAmount())
                    continue;

                answer.add(new CounterTransaction(p, tp));
            }
        }
        return answer;
    }

    private static List<CounterTransaction> findCounterTransactionInAccounts(Client client, Transaction t,
                    AccountTransaction.Type type)
    {
        List<CounterTransaction> answer = new ArrayList<CounterTransaction>();
        for (Account a : client.getAccounts())
        {
            if (a.getTransactions().contains(t))
                continue;

            for (AccountTransaction ta : a.getTransactions())
            {
                if (ta.getType() != type)
                    continue;

                if (!ta.getDate().equals(t.getDate()))
                    continue;

                if (ta.getSecurity() != null && ta.getSecurity() != t.getSecurity())
                    continue;

                if (ta.getAmount() != t.getAmount())
                    continue;

                answer.add(new CounterTransaction(a, ta));
            }
        }
        return answer;
    }

}
