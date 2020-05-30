package name.abuchen.portfolio.datatransfer.actions;

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

        for (PortfolioTransaction t : transactions)
        {
            if (!equivalentTypes.test(t))
                continue;

            if (isPotentialDuplicate(subject, t))
                return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
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
}
