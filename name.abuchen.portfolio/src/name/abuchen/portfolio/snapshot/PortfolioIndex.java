package name.abuchen.portfolio.snapshot;

import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

public class PortfolioIndex extends PerformanceIndex
{
    public static PortfolioIndex forPeriod(Client client, Portfolio portfolio, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        PortfolioIndex index = new PortfolioIndex(client, portfolio, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private Portfolio portfolio;

    private PortfolioIndex(Client client, Portfolio portfolio, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.portfolio = portfolio;
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
        totals[0] = PortfolioSnapshot.create(portfolio, dates[0]).getValue();

        // calculate series
        int index = 1;
        DateTime date = interval.getStart().plusDays(1);
        while (date.compareTo(interval.getEnd()) <= 0)
        {
            dates[index] = date.toDate();
            totals[index] = PortfolioSnapshot.create(portfolio, dates[index]).getValue();

            date = date.plusDays(1);
            index++;
        }
    }

}
