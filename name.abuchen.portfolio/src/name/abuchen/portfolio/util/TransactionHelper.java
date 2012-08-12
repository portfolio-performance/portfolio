package name.abuchen.portfolio.util;

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
                return true;
            default:
                return false;
        }
    }

    public static CounterTransaction findCounterTransaction(Client client, Transaction t)
    {
        if (t instanceof AccountTransaction)
            return findCounterTransaction(client, (AccountTransaction) t);
        else if (t instanceof PortfolioTransaction)
            return findCounterTransaction(client, (PortfolioTransaction) t);
        else
            throw new UnsupportedOperationException("Unsupport transaction " + t.getClass().getName()); //$NON-NLS-1$
    }

    private static CounterTransaction findCounterTransaction(Client client, AccountTransaction t)
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
                return null;
        }
    }

    private static CounterTransaction findCounterTransaction(Client client, PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
                return findCounterTransactionInAccounts(client, t, AccountTransaction.Type.BUY);
            case SELL:
                return findCounterTransactionInAccounts(client, t, AccountTransaction.Type.SELL);
            default:
                return null;
        }
    }

    private static CounterTransaction findCounterTransactionInPortfolio(Client client, AccountTransaction t,
                    PortfolioTransaction.Type type)
    {
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

                return new CounterTransaction(p, tp);
            }
        }
        return null;
    }

    private static CounterTransaction findCounterTransactionInAccounts(Client client, Transaction t,
                    AccountTransaction.Type type)
    {
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

                return new CounterTransaction(a, ta);
            }
        }
        return null;
    }

}
