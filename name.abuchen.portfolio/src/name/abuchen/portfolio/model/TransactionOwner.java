package name.abuchen.portfolio.model;

import java.util.List;

/**
 * A transaction owner has transactions.
 * 
 * @see Account
 * @see Portfolio
 */
public interface TransactionOwner<T extends Transaction>
{
    List<T> getTransactions();

    void addTransaction(T transaction);
}
