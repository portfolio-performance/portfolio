package name.abuchen.portfolio.snapshot;

import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

public class AccountIndex extends PerformanceIndex
{
    public static AccountIndex forPeriod(Client client, Account account, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        AccountIndex index = new AccountIndex(client, account, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private Account account;

    private AccountIndex(Client client, Account account, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.account = account;
    }

    private void calculate(List<Exception> warnings)
    {
        Interval interval = getReportInterval().toInterval();
        int size = Days.daysBetween(interval.getStart(), interval.getEnd()).getDays() + 1;

        dates = new Date[size];
        totals = new long[size];
        delta = new double[size];
        accumulated = new double[size];
        transferals = new long[size];

        // first value = reference value
        dates[0] = interval.getStart().toDate();
        totals[0] = AccountSnapshot.create(account, dates[0]).getFunds();

        // calculate series
        int index = 1;
        DateTime date = interval.getStart().plusDays(1);
        while (date.compareTo(interval.getEnd()) <= 0)
        {
            dates[index] = date.toDate();
            totals[index] = AccountSnapshot.create(account, dates[index]).getFunds();

            date = date.plusDays(1);
            index++;
        }
    }

}
