package name.abuchen.portfolio.datatransfer.actions;

import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

public class DetectDuplicatesAction implements ImportAction
{

    @Override
    public Status process(Security security)
    {
        return Status.OK_STATUS;
    }

    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        if (account != null) 
        {
            return check(transaction, account.getTransactions()); 
        }
        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        return check(transaction, portfolio.getTransactions());
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        Status status = check(entry.getAccountTransaction(), account.getTransactions());
        if (status.getCode() != Status.Code.OK)
            return status;
        return check(entry.getPortfolioTransaction(), portfolio.getTransactions());
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        return check(entry.getSourceTransaction(), source.getTransactions());
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        return check(entry.getTargetTransaction(), source.getTransactions());
    }

    private Status check(AccountTransaction subject, List<AccountTransaction> transactions)
    {
        for (AccountTransaction t : transactions)
        {
            if (subject.getType() != t.getType())
                continue;

            if (isPotentialDuplicate(subject, t))
                return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
        }

        return Status.OK_STATUS;
    }

    private Status check(PortfolioTransaction subject, List<PortfolioTransaction> transactions)
    {
        for (PortfolioTransaction t : transactions)
        {
            if (subject.getType() != t.getType())
                continue;

            if (isPotentialDuplicate(subject, t))
                return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
        }

        return Status.OK_STATUS;
    }

    private boolean isPotentialDuplicate(Transaction subject, Transaction other)
    {
        if (!other.getDate().equals(subject.getDate()))
            return false;

        if (!other.getCurrencyCode().equals(subject.getCurrencyCode()))
            return false;

        if (other.getAmount() != subject.getAmount())
            return false;

        if (other.getShares() != subject.getShares())
            return false;

        if (!Objects.equals(other.getSecurity(), subject.getSecurity()))
            return false;

        return true;
    }
}
