package name.abuchen.portfolio.snapshot;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

/* package */class ClientIndex extends PerformanceIndex
{
    /* package */ClientIndex(Client client, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
    }

    /* package */void calculate(List<Exception> warnings)
    {
        Interval interval = getReportInterval().toInterval();
        int size = Days.daysBetween(interval.getStart(), interval.getEnd()).getDays() + 1;

        dates = new Date[size];
        totals = new long[size];
        delta = new double[size];
        accumulated = new double[size];

        collectTransferalsAndTaxes(size, interval);

        // first value = reference value
        dates[0] = interval.getStart().toDate();
        delta[0] = 0;
        accumulated[0] = 0;
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), dates[0]);
        long valuation = totals[0] = snapshot.getAssets();

        // calculate series
        int index = 1;
        DateTime date = interval.getStart().plusDays(1);
        while (date.compareTo(interval.getEnd()) <= 0)
        {
            dates[index] = date.toDate();

            snapshot = ClientSnapshot.create(getClient(), dates[index]);
            long thisValuation = totals[index] = snapshot.getAssets();
            long thisDelta = thisValuation - transferals[index] - valuation;

            if (valuation == 0)
            {
                delta[index] = 0;

                if (thisDelta != 0d)
                {
                    if (transferals[index] != 0)
                        delta[index] = (double) thisDelta / (double) transferals[index];
                    else
                        warnings.add(new RuntimeException(MessageFormat.format(Messages.MsgDeltaWithoutAssets,
                                        thisDelta, date.toDate())));
                }
            }
            else
            {
                delta[index] = (double) thisDelta / (double) valuation;
            }

            accumulated[index] = ((accumulated[index - 1] + 1) * (delta[index] + 1)) - 1;

            date = date.plusDays(1);
            valuation = thisValuation;
            index++;
        }
    }

    private void collectTransferalsAndTaxes(int size, Interval interval)
    {
        this.transferals = new long[size];
        this.taxes = new long[size];

        for (Account a : getClient().getAccounts())
        {
            for (AccountTransaction t : a.getTransactions())
            {
                if (t.getDate().getTime() >= interval.getStartMillis()
                                && t.getDate().getTime() <= interval.getEndMillis())
                {
                    long transferal = 0;
                    long tax = 0;
                    switch (t.getType())
                    {
                        case DEPOSIT:
                            transferal = t.getAmount();
                            break;
                        case REMOVAL:
                            transferal = -t.getAmount();
                            break;
                        case TAXES:
                            tax = t.getAmount();
                            break;
                        default:
                            // do nothing
                            break;
                    }

                    if (transferal != 0)
                    {
                        int ii = Days.daysBetween(interval.getStart(), new DateTime(t.getDate().getTime())).getDays();
                        transferals[ii] += transferal;
                    }

                    if (tax != 0)
                    {
                        int ii = Days.daysBetween(interval.getStart(), new DateTime(t.getDate().getTime())).getDays();
                        taxes[ii] += tax;
                    }
                }
            }
        }

        for (Portfolio p : getClient().getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                if (t.getDate().getTime() >= interval.getStartMillis()
                                && t.getDate().getTime() <= interval.getEndMillis())
                {
                    long transferal = 0;

                    switch (t.getType())
                    {
                        case DELIVERY_INBOUND:
                            transferal = t.getAmount();
                            break;
                        case DELIVERY_OUTBOUND:
                            transferal = -t.getAmount();
                            break;
                        default:
                            // do nothing
                            break;
                    }

                    if (transferal != 0)
                    {
                        int ii = Days.daysBetween(interval.getStart(), new DateTime(t.getDate().getTime())).getDays();
                        transferals[ii] += transferal;
                    }

                }
            }
        }
    }
}
