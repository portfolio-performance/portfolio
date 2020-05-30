package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

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
                return Status.OK_STATUS;
            default:
                throw new UnsupportedOperationException();
        }
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
}
