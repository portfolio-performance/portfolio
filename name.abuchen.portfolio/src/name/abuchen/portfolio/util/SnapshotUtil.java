package name.abuchen.portfolio.util;

import java.time.LocalDateTime;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Transaction;

public class SnapshotUtil
{
    private SnapshotUtil()
    {
        /* This utility class should not be instantiated */
    }

    public static LocalDateTime getPerformanceDateTime(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction
                        && accountTransaction.getType() == AccountTransaction.Type.DIVIDENDS
                        && accountTransaction.getExDate() != null)
            return accountTransaction.getExDate();

        return transaction.getDateTime();
    }

}
