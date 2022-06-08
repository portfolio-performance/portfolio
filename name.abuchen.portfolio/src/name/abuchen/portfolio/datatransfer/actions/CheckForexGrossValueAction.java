package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;
import java.util.Optional;

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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class CheckForexGrossValueAction implements ImportAction
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
        return check(entry.getPortfolioTransaction(), entry.getAccountTransaction());
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
        for (Transaction t : transactions)
        {
            Status status = check(t);
            if (status.getCode() != Status.Code.OK)
                return status;
        }
        return Status.OK_STATUS;
    }

    private Status check(Transaction transaction)
    {
        Optional<Unit> grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE);

        if (grossValueUnit.isEmpty())
            return Status.OK_STATUS;

        Money unitValue = grossValueUnit.get().getAmount();
        Money calculatedValue = transaction.getGrossValue();

        if (!unitValue.equals(calculatedValue))
            return new Status(Status.Code.ERROR,
                            MessageFormat.format(Messages.MsgCheckConfiguredAndCalculatedGrossValueDoNotMatch,
                                            Values.Money.format(unitValue), Values.Money.format(calculatedValue)));

        return Status.OK_STATUS;

    }

}
