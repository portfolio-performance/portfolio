package name.abuchen.portfolio.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.Messages;
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

public class PerformanceIndex
{
    private final Client client;
    private final ReportingPeriod reportInterval;

    protected Date[] dates;
    protected long[] totals;
    protected long[] transferals;
    protected double[] accumulated;
    protected double[] delta;

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

    public long[] calculateInvestedCapital()
    {
        long[] investedCapital = new long[transferals.length];

        long current = investedCapital[0] = totals[0];
        for (int ii = 1; ii < investedCapital.length; ii++)
            current = investedCapital[ii] = current + transferals[ii];

        return investedCapital;
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

        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
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
        finally
        {
            writer.close();
        }
    }
}
