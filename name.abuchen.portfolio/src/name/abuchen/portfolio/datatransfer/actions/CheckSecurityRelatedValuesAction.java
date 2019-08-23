package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.Set;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.money.Values;

public class CheckSecurityRelatedValuesAction implements ImportAction
{
    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        boolean hasSecurity = transaction.getSecurity() != null;

        Set<Type> typesWithOptionalSecurity = EnumSet.of(Type.DIVIDENDS, Type.TAXES, Type.TAX_REFUND, Type.FEES,
                        Type.FEES_REFUND);

        if (hasSecurity && !typesWithOptionalSecurity.contains(transaction.getType()))
            return new Status(Status.Code.ERROR,
                            MessageFormat.format(Messages.MsgCheckTransactionTypeCannotHaveASecurity,
                                            transaction.getType(), transaction.getSecurity().getName()));

        if (!hasSecurity && transaction.getType() == Type.DIVIDENDS)
            return new Status(Status.Code.ERROR, Messages.MsgCheckDividendsMustHaveASecurity);

        if (transaction.getShares() != 0
                        && (!hasSecurity || !typesWithOptionalSecurity.contains(transaction.getType())))
            return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckTransactionTypeCannotHaveShares,
                            transaction.getType(), Values.Share.format(transaction.getShares())));

        return Status.OK_STATUS;
    }
}
