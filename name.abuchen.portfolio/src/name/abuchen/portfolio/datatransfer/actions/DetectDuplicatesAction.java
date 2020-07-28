package name.abuchen.portfolio.datatransfer.actions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

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

public class DetectDuplicatesAction implements ImportAction
{
    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        return check(transaction, account.getTransactions());
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

    public Transaction findSavingsPlanTransaction(AccountTransaction subject, List<AccountTransaction> transactions)
    {
        for (AccountTransaction t : transactions)
        {
            if (t.getType() != AccountTransaction.Type.BUY)
                continue;

            // check for savings plan for potential duplicates and buy or
            // inbound delivery transactions
            if (isSavingsPlanDuplicate(subject, t))
                return t;
        }
        return null;
    }
    
    private Status check(AccountTransaction subject, List<AccountTransaction> transactions)
    {
        for (AccountTransaction t : transactions)
        {
            if (subject.getType() != t.getType())
                continue;

            boolean isMatch = false;
            if (isPotentialDuplicate(subject, t))
            {
                isMatch = true;
            }

            // check for savings plan for potential duplicates and buy or inbound delivery transactions
            if (isMatch || subject.getType() == AccountTransaction.Type.BUY)
            {
                if (isSavingsPlanDuplicate(subject, t)) 
                {
                    return new Status(Status.Code.OK, IMPORT_SAVINGS_PLAN_ITEM); 
                } 
                else if (isMatch)
                {
                    return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
                }
            } 
        }

        return Status.OK_STATUS;
    }

    private Status check(PortfolioTransaction subject, List<PortfolioTransaction> transactions)
    {
        Predicate<PortfolioTransaction> equivalentTypes;

        switch (subject.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                equivalentTypes = t -> t.getType() == PortfolioTransaction.Type.BUY
                                || t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND;
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                equivalentTypes = t -> t.getType() == PortfolioTransaction.Type.SELL
                                || t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
                break;
            default:
                equivalentTypes = t -> subject.getType() == t.getType();
        }

        boolean isMatch = false;
        for (PortfolioTransaction t : transactions)
        {
            if (!equivalentTypes.test(t))
                continue;

            if (isPotentialDuplicate(subject, t))
            {
                isMatch = true;
            }

            // check for savings plan for potential duplicates and buy or inbound delivery transactions
            if (isMatch || subject.getType() == PortfolioTransaction.Type.BUY
                            || subject.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
            {
                if (isSavingsPlanDuplicate(subject, t)) 
                {
                    return new Status(Status.Code.OK, IMPORT_SAVINGS_PLAN_ITEM); 
                } 
                else if (isMatch)
                {
                    return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
                }
            } 
        }

        return Status.OK_STATUS;
    }

    private boolean isPotentialDuplicate(Transaction subject, Transaction other)
    {
        if (!other.getDateTime().equals(subject.getDateTime()))
            return false;

        if (!other.getCurrencyCode().equals(subject.getCurrencyCode()))
            return false;

        if (other.getAmount() != subject.getAmount())
            return false;

        if (other.getShares() != subject.getShares())
            return false;

        if (!Objects.equals(other.getSecurity(), subject.getSecurity())) // NOSONAR
            return false;
        
        return true;
    }

    private boolean isSavingsPlanDuplicate(Transaction subject, Transaction other)
    {
        if (!other.getCurrencyCode().equals(subject.getCurrencyCode()))
            return false;

        if (!Objects.equals(other.getSecurity(), subject.getSecurity())) // NOSONAR
            return false;

        if (other.getAmount() != subject.getAmount())
            return false;

        LocalDateTime date = subject.getDateTime();
        // date can be up to three days after other's date (due to shifted executions on weekends)
        if (date.isBefore(other.getDateTime()) || date.minusDays(3).isAfter(other.getDateTime()))
            return false;
        
        // number of shares might differ due to differing prices per share used by savings plan
        // accept a difference of 10%
        long shares = subject.getShares();
        if (shares * 1.1 < other.getShares() || shares * 0.9 > other.getShares())
            return false;
        
        // use the behavior that savings plans get a specific note and check for it
        if (other.getNote() == null || !other.getNote().startsWith(Messages.InvestmentPlanAutoNoteLabel.substring(0, 8)))
            return false;
        
        return true;

    }
}
