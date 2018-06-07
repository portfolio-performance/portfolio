package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.Comparator;

public class DedicatedTransaction
{
    public static final class ByDateAmountAccountTypeAndHashCode implements Comparator<DedicatedTransaction>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(DedicatedTransaction d1, DedicatedTransaction d2)
        {
            AccountTransaction t1 = d1.getTransaction();
            AccountTransaction t2 = d2.getTransaction();

            int compare = t1.getDate().compareTo(t2.getDate());
            if (compare != 0)
                return compare;

            compare = Long.compare(t1.getAmount(), t2.getAmount());
            if (compare != 0)
                return compare;

            compare = d1.getAccount().getUUID().compareTo(d2.getAccount().getUUID());
            if (compare != 0)
                return compare;

            compare = t1.getType().compareTo(t2.getType());
            if (compare != 0)
                return compare;

            return Integer.compare(t1.hashCode(), t2.hashCode());
        }
    }

    private Account account;
    private AccountTransaction transaction;

    public DedicatedTransaction()
    {
        // needed for xstream de-serialization
    }

    public DedicatedTransaction(Account account, AccountTransaction transaction)
    {
        this.account = account;
        this.transaction = transaction;
    }

    public Account getAccount()
    {
        return this.account;
    }

    public AccountTransaction getTransaction()
    {
        return this.transaction;
    }
}
