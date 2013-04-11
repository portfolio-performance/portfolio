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
import name.abuchen.portfolio.model.Security.AssetClass;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

public class ClientIndex extends PerformanceIndex
{
    
    
    public static ClientIndex forPeriod(Client client, ReportingPeriod reportInterval, List<Exception> warnings)
    {
        ClientIndex index = new ClientIndex(client, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private long[][] assetClasses;

    private ClientIndex(Client client, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
    }

    public DateTime getFirstDataPoint()
    {
        for (int ii = 0; ii < totals.length; ii++)
        {
            if (totals[ii] != 0)
                return new DateTime(dates[ii]);
        }

        return null;
    }
    
   

    public long[] byAssetClass(AssetClass assetClass)
    {
        return assetClasses[assetClass.ordinal()];
    }

    private void calculate(List<Exception> warnings)
    {
        Interval interval = getReportInterval().toInterval();
        int size = Days.daysBetween(interval.getStart(), interval.getEnd()).getDays() + 1;

        dates = new Date[size];
        totals = new long[size];
        assetClasses = new long[AssetClass.values().length][size];
        delta = new double[size];
        accumulated = new double[size];

        transferals = collectTransferals(size, interval);
        invested = collectInvested(size, interval);

        // first value = reference value
        dates[0] = interval.getStart().toDate();
        delta[0] = 0;
        accumulated[0] = 0;
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), dates[0]);
        long valuation = totals[0] = snapshot.getAssets();
        fillAssetClasses(snapshot, 0);

        // calculate series
        int index = 1;
        DateTime date = interval.getStart().plusDays(1);
        while (date.compareTo(interval.getEnd()) <= 0)
        {
            dates[index] = date.toDate();

            snapshot = ClientSnapshot.create(getClient(), dates[index]);
            long thisValuation = totals[index] = snapshot.getAssets();
            long thisDelta = thisValuation - transferals[index] - valuation;

            fillAssetClasses(snapshot, index);

            if (valuation == 0)
            {
                delta[index] = 0;

                if (thisDelta != 0d)
                {
                    delta[index] = (double) thisDelta / (double) Math.abs(transferals[index]);
                    warnings.add(new RuntimeException(MessageFormat.format(Messages.MsgDeltaWithoutAssets, thisDelta,
                                    date.toDate())));
                    if (transferals[index] != 0)
                        delta[index] = (double) thisDelta / (double) transferals[index];
                    else
                        warnings.add(new RuntimeException(MessageFormat.format(Messages.MsgDeltaWithoutAssets,
                                        thisDelta, date.toDate())));
                }
            }
            else
            {
                delta[index] = (double) thisDelta / (double) Math.abs(valuation);
            }

            accumulated[index] = ((accumulated[index - 1] + 1) * ((accumulated[index - 1] + 1) < 0 ? (delta[index] - 1)
                                             : (delta[index] + 1))) - 1;

            date = date.plusDays(1);
            valuation = thisValuation;
            index++;
        }
    }
    
    private double[] collectInvested(int size, Interval interval) {
        double[] invested = new double[size];
        for (Portfolio p : getClient().getPortfolios()) {
            for (PortfolioTransaction t : p.getTransactions()){
                if (interval.contains(t.getDate().getTime())){
                    long delta = 0;
                    switch (t.getType())
                    {
                        case BUY:
                            delta = t.getAmount();
                            break;
                        case SELL:
                            delta = -t.getAmount();
                            break;
                        default:
                            // do nothing
                    }
                    if (delta != 0){
                        int ii = Days.daysBetween(interval.getStart(), new DateTime(t.getDate().getTime())).getDays();
                        for (int i=ii;i<invested.length;i++) {
                            invested[i] += delta;
                        }
                    }
                }
            }
        }
        return invested;
    }

    private void fillAssetClasses(ClientSnapshot snapshot, int index)
    {
        GroupByAssetClass byAssetClass = snapshot.groupByAssetClass();
        for (int ii = 0; ii < assetClasses.length; ii++)
        {
            AssetCategory c = byAssetClass.byClass(AssetClass.values()[ii]);
            assetClasses[ii][index] = c != null ? c.getValuation() : 0;
        }
    }

    private long[] collectTransferals(int size, Interval interval)
    {
        long[] transferals = new long[size];

        for (Account a : getClient().getAccounts())
        {
            for (AccountTransaction t : a.getTransactions())
            {
                if (t.getDate().getTime() >= interval.getStartMillis()
                                && t.getDate().getTime() <= interval.getEndMillis())
                {
                    long transferal = 0;
                    switch (t.getType())
                    {
                        case DEPOSIT:
                            transferal = t.getAmount();
                            break;
                        case REMOVAL:
                            transferal = -t.getAmount();
                            break;
                        default:
                            // do nothing
                    }

                    if (transferal != 0)
                    {
                        int ii = Days.daysBetween(interval.getStart(), new DateTime(t.getDate().getTime())).getDays();
                        transferals[ii] += transferal;
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
                    }

                    if (transferal != 0)
                    {
                        int ii = Days.daysBetween(interval.getStart(), new DateTime(t.getDate().getTime())).getDays();
                        transferals[ii] += transferal;
                    }

                }
            }
        }

        return transferals;
    }
}
