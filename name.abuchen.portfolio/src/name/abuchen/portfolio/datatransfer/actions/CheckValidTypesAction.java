package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public class CheckValidTypesAction implements ImportAction
{
    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        switch (transaction.getType())
        {
            case BUY:
            case SELL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckInvalidTransactionType,
                                transaction.getType().toString()));
            case DEPOSIT:
            case DIVIDENDS:
            case INTEREST:
            case INTEREST_CHARGE:
            case TAX_REFUND:
            case TAXES:
            case REMOVAL:
            case FEES:
            case FEES_REFUND:
                return checkUnits(transaction);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Status checkUnits(AccountTransaction transaction)
    {
        // same rules as the transaction dialog: only dividends and interest
        // can carry taxes, only dividends can carry fees

        var type = transaction.getType();

        if (type != AccountTransaction.Type.DIVIDENDS && type != AccountTransaction.Type.INTEREST)
        {
            var taxes = transaction.getUnits().filter(u -> u.getType() == Unit.Type.TAX).map(Unit::getAmount)
                            .collect(MoneyCollectors.sum(transaction.getCurrencyCode()));
            if (!taxes.isZero())
                return new Status(Status.Code.ERROR, MessageFormat.format(
                                Messages.MsgCheckTransactionTypeCannotHaveTaxes, type, Values.Money.format(taxes)));
        }

        if (type != AccountTransaction.Type.DIVIDENDS)
        {
            var fees = transaction.getUnits().filter(u -> u.getType() == Unit.Type.FEE).map(Unit::getAmount)
                            .collect(MoneyCollectors.sum(transaction.getCurrencyCode()));
            if (!fees.isZero())
                return new Status(Status.Code.ERROR, MessageFormat.format(
                                Messages.MsgCheckTransactionTypeCannotHaveFees, type, Values.Money.format(fees)));
        }

        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        switch (transaction.getType())
        {
            case BUY:
            case SELL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckInvalidTransactionType,
                                transaction.getType().toString()));
            case DELIVERY_INBOUND:
            case DELIVERY_OUTBOUND:
                return Status.OK_STATUS;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        switch (entry.getPortfolioTransaction().getType())
        {
            case BUY:
            case SELL:
                return Status.OK_STATUS;
            case TRANSFER_IN:
            case TRANSFER_OUT:
            case DELIVERY_INBOUND:
            case DELIVERY_OUTBOUND:
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckInvalidTransactionType,
                                entry.getPortfolioTransaction().getType().toString()));
            default:
                throw new UnsupportedOperationException();
        }
    }
}
