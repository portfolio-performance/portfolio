package name.abuchen.portfolio.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Values;

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
