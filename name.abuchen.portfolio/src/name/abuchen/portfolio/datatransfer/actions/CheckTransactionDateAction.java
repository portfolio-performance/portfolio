package name.abuchen.portfolio.datatransfer.actions;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;

public class CheckTransactionDateAction implements ImportAction
{
    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        return check(transaction);
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        return check(transaction);
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        return check(entry.getAccountTransaction(), entry.getPortfolioTransaction());
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        return check(entry.getSourceTransaction(), entry.getTargetTransaction());
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        return check(entry.getSourceTransaction(), entry.getTargetTransaction());
    }

    private Status check(Transaction... transactions)
    {
        for (Transaction tx : transactions)
        {
            if (tx.getDateTime() == null)
                return new Status(Status.Code.ERROR, Messages.IssueTransactionWithoutDate);
        }
        return Status.OK_STATUS;
    }
}
