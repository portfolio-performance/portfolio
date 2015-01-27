package name.abuchen.portfolio.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.DoubleStream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.math.Risk;
import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;
import org.joda.time.DateTime;
import org.joda.time.Duration;

public class PerformanceIndex
{
    private final Client client;
    private final ReportingPeriod reportInterval;

    protected Date[] dates;
    protected long[] totals;
    protected long[] transferals;
    protected long[] taxes;
    protected double[] accumulated;
    protected double[] delta;
    protected Drawdown drawdown;
    protected Volatility volatility;

    /* package */PerformanceIndex(Client client, ReportingPeriod reportInterval)
    {
        this.client = client;
        this.reportInterval = reportInterval;
    }

    public static ClientIndex forClient(Client client, ReportingPeriod reportInterval, List<Exception> warnings)
    {
        ClientIndex index = new ClientIndex(client, reportInterval);
        index.calculate(warnings);
        return index;
    }

    public static PerformanceIndex forAccount(Client client, Account account, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        Classification classification = new Classification(null, null);
        classification.addAssignment(new Assignment(account));
        return forClassification(client, classification, reportInterval, warnings);
    }

    public static PerformanceIndex forPortfolio(Client client, Portfolio portfolio, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        return PortfolioIndex.calculate(client, portfolio, reportInterval, warnings);
    }

    public static PerformanceIndex forPortfolioPlusAccount(Client client, Portfolio portfolio,
                    ReportingPeriod reportInterval, List<Exception> warnings)
    {
        return PortfolioPlusIndex.calculate(client, portfolio, reportInterval, warnings);
    }

    public static PerformanceIndex forClassification(Client client, Classification classification,
                    ReportingPeriod reportInterval, List<Exception> warnings)
    {
        return ClassificationIndex.calculate(client, classification, reportInterval, warnings);
    }

    public static PerformanceIndex forInvestment(Client client, Security security, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        Classification classification = new Classification(null, null);
        classification.addAssignment(new Assignment(security));
        return forClassification(client, classification, reportInterval, warnings);
    }

    public static PerformanceIndex forSecurity(PerformanceIndex clientIndex, Security security, List<Exception> warnings)
    {
        SecurityIndex index = new SecurityIndex(clientIndex.getClient(), clientIndex.getReportInterval());
        index.calculate(clientIndex, security);
        return index;
    }

    public static PerformanceIndex forConsumerPriceIndex(PerformanceIndex clientIndex, List<Exception> warnings)
    {
        CPIIndex index = new CPIIndex(clientIndex.getClient(), clientIndex.getReportInterval());
        index.calculate(clientIndex);
        return index;
    }

    public Client getClient()
    {
        return client;
    }

    public ReportingPeriod getReportInterval()
    {
        return reportInterval;
    }

    public Date[] getDates()
    {
        return dates;
    }

    public double[] getAccumulatedPercentage()
    {
        return accumulated;
    }

    /**
     * Returns the final accumulated performance value for this performance
     * reporting period. It is the last element of the array returned by
     * {@link #getAccumulatedPercentage}.
     */
    public double getFinalAccumulatedPercentage()
    {
        return accumulated != null ? accumulated[accumulated.length - 1] : 0;
    }

    public double[] getDeltaPercentage()
    {
        return delta;
    }

    public long[] getTotals()
    {
        return totals;
    }

    public long[] getTransferals()
    {
        return transferals;
    }

    public double getMaxDrawdown()
    {
        return drawdown.getMagnitude();
    }

    public Duration getMaxDrawdownDuration()
    {
        return drawdown.getDuration();
    }

    public double getVolatility()
    {
        return volatility.getStandardDeviation();
    }

    public double getSemiVolatility()
    {
        return volatility.getSemiDeviation();
    }

    public double getAnnualizedVolatility()
    {
        Map<Integer, List<Date>> buckets = new HashMap<Integer, List<Date>>();
        DateTime current;
        for (Date date : dates)
        {
            current = new DateTime(date);
            if (buckets.get(current.getYear()) == null)
            {
                buckets.put(current.getYear(), new ArrayList<Date>());
            }
            buckets.get(current.getYear()).add(date);
        }
        double[] volas = new double[buckets.size()];
        int j = 0;
        for (Entry<Integer, List<Date>> entry : buckets.entrySet())
        {
            List<Date> tempDates = entry.getValue();
            double[] tempAccumulated = new double[tempDates.size()];
            int i = 0;
            for (Date d : tempDates)
            {
                tempAccumulated[i++] = accumulated[Arrays.asList(dates).indexOf(d)];
            }
            Date[] temp = new Date[tempDates.size()];
            volas[j++] = Risk
                            .annualize(new Volatility(tempAccumulated).getStandardDeviation(), tempDates.toArray(temp));
        }
        return DoubleStream.of(volas).average().getAsDouble();
    }

    public double getAnnualizedSemiVolatility()
    {
        return Risk.annualize(volatility.getSemiDeviation(), dates);
    }

    public long[] getTaxes()
    {
        return taxes;
    }

    public long[] calculateInvestedCapital()
    {
        long[] investedCapital = new long[transferals.length];

        long current = investedCapital[0] = totals[0];
        for (int ii = 1; ii < investedCapital.length; ii++)
            current = investedCapital[ii] = current + transferals[ii];

        return investedCapital;
    }

    public long[] calculateAbsoluteDelta()
    {
        long[] answer = calculateInvestedCapital();

        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = totals[ii] - answer[ii];

        return answer;
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

    public void exportTo(File file) throws IOException
    {
        CSVStrategy strategy = new CSVStrategy(';', '"', CSVStrategy.COMMENTS_DISABLED, CSVStrategy.ESCAPE_DISABLED,
                        false, false, false, false);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(strategy);

            printer.println(new String[] { Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_Transferals, //
                            Messages.CSVColumn_DeltaInPercent, //
                            Messages.CSVColumn_CumulatedPerformanceInPercent });

            for (int ii = 0; ii < totals.length; ii++)
            {
                printer.print(Values.Date.format(dates[ii]));
                printer.print(Values.Amount.format(totals[ii]));
                printer.print(Values.Amount.format(transferals[ii]));
                printer.print(Values.Percent.format(delta[ii]));
                printer.print(Values.Percent.format(accumulated[ii]));
                printer.println();
            }
        }
    }
}
