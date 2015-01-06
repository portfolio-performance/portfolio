package name.abuchen.portfolio.snapshot;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

/* package */class ClientIndex extends PerformanceIndex
{
    /* package */ClientIndex(Client client, CurrencyConverter converter, ReportingPeriod reportInterval)
    {
        super(client, converter, reportInterval);
    }

    /* package */void calculate(List<Exception> warnings)
    {
        Interval interval = getReportInterval().toInterval();
        int size = Days.daysBetween(interval.getStart(), interval.getEnd()).getDays() + 1;

        dates = new Date[size];
        totals = new long[size];
        delta = new double[size];
        accumulated = new double[size];
        transferals = new long[size];
        taxes = new long[size];

        collectTransferalsAndTaxes(size, interval);

        // first value = reference value
        dates[0] = interval.getStart().toDate();
        delta[0] = 0;
        accumulated[0] = 0;
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), getCurrencyConverter(), dates[0]);
        long valuation = totals[0] = snapshot.getMonetaryAssets().getAmount();

        // calculate series
        int index = 1;
        DateTime date = interval.getStart().plusDays(1);
        while (date.compareTo(interval.getEnd()) <= 0)
        {
            dates[index] = date.toDate();

            snapshot = ClientSnapshot.create(getClient(), getCurrencyConverter(), dates[index]);
            long thisValuation = totals[index] = snapshot.getMonetaryAssets().getAmount();
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

    private void addValue(long[] array, String currencyCode, long value, Interval interval, DateMidnight time)
    {
        if (value == 0)
            return;

        int ii = Days.daysBetween(interval.getStart(), time).getDays();

        if (!currencyCode.equals(getCurrencyConverter().getTermCurrency()))
            value = getCurrencyConverter().convert(time.toDate(), Money.of(currencyCode, value)).getAmount();

        array[ii] += value;
    }

    private void collectTransferalsAndTaxes(int size, Interval interval)
    {
        for (Account account : getClient().getAccounts())
        {
            String currencyCode = account.getCurrencyCode();
            account.getTransactions()
                            .stream()
                            .filter(t -> t.getDate().getTime() >= interval.getStartMillis()
                                            && t.getDate().getTime() <= interval.getEndMillis()) //
                            .forEach(t -> {
                                switch (t.getType())
                                {
                                    case DEPOSIT:
                                        addValue(transferals, currencyCode, t.getAmount(), interval,
                                                        t.getDateMidnight());
                                        break;
                                    case REMOVAL:
                                        addValue(transferals, currencyCode, -t.getAmount(), interval,
                                                        t.getDateMidnight());
                                        break;
                                    case TAXES:
                                        addValue(taxes, currencyCode, t.getAmount(), interval, t.getDateMidnight());
                                        break;
                                    case TAX_REFUND:
                                        addValue(taxes, currencyCode, -t.getAmount(), interval, t.getDateMidnight());
                                        break;
                                    default:
                                        // do nothing
                                        break;
                                }
                            });

        }

        for (Portfolio portfolio : getClient().getPortfolios())
        {
            String currencyCode = portfolio.getReferenceAccount().getCurrencyCode();

            portfolio.getTransactions()
                            .stream()
                            .filter(t -> t.getDate().getTime() >= interval.getStartMillis()
                                            && t.getDate().getTime() <= interval.getEndMillis()) //
                            .forEach(t -> {
                                switch (t.getType())
                                {
                                    case DELIVERY_INBOUND:
                                        addValue(transferals, currencyCode, t.getAmount(), interval,
                                                        t.getDateMidnight());
                                        addValue(taxes, currencyCode, t.getTaxes(), interval, t.getDateMidnight());
                                        break;
                                    case DELIVERY_OUTBOUND:
                                        addValue(transferals, currencyCode, -t.getAmount(), interval,
                                                        t.getDateMidnight());
                                        addValue(taxes, currencyCode, t.getTaxes(), interval, t.getDateMidnight());
                                        break;
                                    default:
                                        addValue(taxes, currencyCode, t.getTaxes(), interval, t.getDateMidnight());
                                        break;
                                }
                            });

        }

    }
}
