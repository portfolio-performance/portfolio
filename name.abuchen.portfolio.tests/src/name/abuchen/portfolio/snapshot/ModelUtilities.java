package name.abuchen.portfolio.snapshot;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

import org.joda.time.DateMidnight;
import org.joda.time.DateTimeConstants;

/* package */class ModelUtilities
{
    static void addT(Account account, Calendar calendar, AccountTransaction.Type type, long amount)
    {
        account.addTransaction(new AccountTransaction(calendar.getTime(), null, type, amount));
    }

    static void addT(Account account, int year, int month, int day, AccountTransaction.Type type, long amount)
    {
        account.addTransaction(new AccountTransaction(new GregorianCalendar(year, month, day).getTime(), null, type,
                        amount));
    }

    static void generatePrices(Security security, long startPrice, DateMidnight start, DateMidnight end)
    {
        security.addPrice(new SecurityPrice(start.toDate(), startPrice));

        Random random = new Random();

        DateMidnight date = start;
        long price = startPrice;
        while (date.compareTo(end) < 0)
        {
            date = date.plusDays(1);

            if (date.getDayOfWeek() > DateTimeConstants.SATURDAY)
                continue;

            price = (long) ((double) price * ((random.nextDouble() * 0.2 - 0.1d) + 1));
            security.addPrice(new SecurityPrice(date.toDate(), price));
        }
    }

}
