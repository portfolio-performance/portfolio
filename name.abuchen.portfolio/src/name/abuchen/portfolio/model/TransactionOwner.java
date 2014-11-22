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

    /**
     * Adds a new transaction to the transaction owner ({@link Portfolio} or
     * {@link Account}).
     * 
     * @param transaction
     */
    void addTransaction(T transaction);

    /**
     * Deletes the transaction from the transaction owner ({@link Portfolio} or
     * {@link Account}). Deleting a transactions also removes a possible cross
     * entry and removing the transaction from an {@link InvestmentPlan}.
     */
    void deleteTransaction(T transaction, Client client);

    /**
     * Deletes the transaction from the transaction owner, and only from the
     * transaction owner (hence shallow).
     */
    void shallowDeleteTransaction(T transaction, Client client);
}
