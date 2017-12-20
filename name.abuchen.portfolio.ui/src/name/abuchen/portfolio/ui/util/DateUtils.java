package name.abuchen.portfolio.ui.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

public class DateUtils
{
    public static boolean isDateTimeTransaction(Transaction transaction)
    {
        if (transaction instanceof PortfolioTransaction)
        {
            LocalDateTime dateTime = transaction.getDateTime();
            return !dateTime.toLocalTime().equals(LocalTime.MIDNIGHT);
        }
        return false;
    }
    
    public static String formatTransactionDate(Transaction transaction)
    {
        if (isDateTimeTransaction(transaction))
        {
            return Values.DateTime.format(transaction.getDateTime());
        }
        else
        {
            return Values.Date.format(transaction.getDate());
        }
    }
}
