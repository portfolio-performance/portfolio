package name.abuchen.portfolio.snapshot;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Interval;

/* package */class ClientIndex extends PerformanceIndex
{
    /* package */ClientIndex(Client client, CurrencyConverter converter, ReportingPeriod reportInterval)
    {
        super(client, converter, reportInterval);
    }

    /* package */void calculate(List<Exception> warnings)
    {
        Interval interval = getReportInterval().toInterval();

        // the actual interval should not extend into the future
        if (interval.getEnd().isAfter(LocalDate.now()))
        {
            LocalDate start = interval.getStart();
            LocalDate end = LocalDate.now();

            if (start.isAfter(end))
                start = end;

            interval = Interval.of(start, end);
        }

        // reported via forum: if the user selects as 'since' date something in
        // the future, then #getDays will return something negative. Ensure the
        // 'size' is at least 1 which will create an empty ClientIndex
        int size = Math.max(1, (int) interval.getDays() + 1);

        dates = new LocalDate[size];
        totals = new long[size];
        delta = new double[size];
        accumulated = new double[size];
        transferals = new long[size];
        taxes = new long[size];
        dividends = new long[size];
        interest = new long[size];

        collectTransferalsAndTaxes(size, interval);

        // first value = reference value
        dates[0] = interval.getStart();
        delta[0] = 0;
        accumulated[0] = 0;
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), getCurrencyConverter(), dates[0]);
        long valuation = totals[0] = snapshot.getMonetaryAssets().getAmount();

        // calculate series
        int index = 1;
        LocalDate date = interval.getStart().plusDays(1);
        while (date.compareTo(interval.getEnd()) <= 0)
        {
            dates[index] = date;

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
                                        thisDelta, date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))));
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

    private void addValue(long[] array, String currencyCode, long value, Interval interval, LocalDate time)
    {
        if (value == 0)
            return;

        int ii = Dates.daysBetween(interval.getStart(), time);

        if (!currencyCode.equals(getCurrencyConverter().getTermCurrency()))
            value = getCurrencyConverter().convert(time, Money.of(currencyCode, value)).getAmount();

        array[ii] += value;
    }

    private void collectTransferalsAndTaxes(int size, Interval interval)
    {
        for (Account account : getClient().getAccounts())
        {
            account.getTransactions()
                            .stream()
                            .filter(t -> !t.getDate().isBefore(interval.getStart())
                                            && !t.getDate().isAfter(interval.getEnd()))
                            .forEach(t -> {
                                switch (t.getType())
                                {
                                    case DEPOSIT:
                                        addValue(transferals, t.getCurrencyCode(), t.getAmount(), interval, t.getDate());
                                        break;
                                    case REMOVAL:
                                        addValue(transferals, t.getCurrencyCode(), -t.getAmount(), interval,
                                                        t.getDate());
                                        break;
                                    case TAXES:
                                        addValue(taxes, t.getCurrencyCode(), t.getAmount(), interval, t.getDate());
                                        break;
                                    case TAX_REFUND:
                                        addValue(taxes, t.getCurrencyCode(), -t.getAmount(), interval, t.getDate());
                                        break;
                                    case DIVIDENDS:
                                        addValue(dividends, t.getCurrencyCode(), t.getAmount(), interval, t.getDate());
                                        break;
                                    case INTEREST:
                                        addValue(interest, t.getCurrencyCode(), t.getAmount(), interval, t.getDate());
                                        break;
                                    default:
                                        // do nothing
                                        break;
                                }
                            });

        }

        for (Portfolio portfolio : getClient().getPortfolios())
        {
            portfolio.getTransactions()
                            .stream()
                            .filter(t -> !t.getDate().isBefore(interval.getStart())
                                            && !t.getDate().isAfter(interval.getEnd()))
                            .forEach(t -> {
                                // collect taxes
                                addValue(taxes, t.getCurrencyCode(), t.getUnitSum(Unit.Type.TAX).getAmount(), //
                                                interval, t.getDate());

                                // collect transferals
                                switch (t.getType())
                                {
                                    case DELIVERY_INBOUND:
                                        addValue(transferals, t.getCurrencyCode(), t.getAmount(), interval, t.getDate());
                                        break;
                                    case DELIVERY_OUTBOUND:
                                        addValue(transferals, t.getCurrencyCode(), -t.getAmount(), interval,
                                                        t.getDate());
                                        break;
                                    default:
                                        break;
                                }
                            });

        }
    }
}
