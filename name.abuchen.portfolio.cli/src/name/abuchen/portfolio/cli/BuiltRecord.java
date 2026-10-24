package name.abuchen.portfolio.cli;

import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;

/**
 * A transaction record that has passed every check and is not in the model
 * yet: its sides, primary first, each with the account it belongs to.
 *
 * @param insert
 *            puts all sides into their accounts
 */
record BuiltRecord(List<Side> sides, Runnable insert)
{
    /** One side of a record, in the cash account or investment account that holds it. */
    record Side(Transaction transaction, TransactionOwner<? extends Transaction> owner)
    {
    }

    Transaction primary()
    {
        return sides.get(0).transaction();
    }

    static BuiltRecord of(AccountTransaction transaction, Account account)
    {
        return new BuiltRecord(List.of(new Side(transaction, account)), () -> account.addTransaction(transaction));
    }

    static BuiltRecord of(PortfolioTransaction transaction, Portfolio portfolio)
    {
        return new BuiltRecord(List.of(new Side(transaction, portfolio)),
                        () -> portfolio.addTransaction(transaction));
    }

    static BuiltRecord of(BuySellEntry entry)
    {
        return new BuiltRecord(List.of(new Side(entry.getPortfolioTransaction(), entry.getPortfolio()),
                        new Side(entry.getAccountTransaction(), entry.getAccount())), entry::insert);
    }

    static BuiltRecord of(PortfolioTransferEntry entry)
    {
        return new BuiltRecord(List.of(new Side(entry.getSourceTransaction(), entry.getSourcePortfolio()),
                        new Side(entry.getTargetTransaction(), entry.getTargetPortfolio())), entry::insert);
    }

    static BuiltRecord of(AccountTransferEntry entry)
    {
        return new BuiltRecord(List.of(new Side(entry.getSourceTransaction(), entry.getSourceAccount()),
                        new Side(entry.getTargetTransaction(), entry.getTargetAccount())), entry::insert);
    }
}
