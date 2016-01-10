package name.abuchen.portfolio.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;

public class PerformanceIndex
{
    private final Client client;
    private final CurrencyConverter converter;
    private final ReportingPeriod reportInterval;

    protected LocalDate[] dates;
    protected long[] totals;
    protected long[] transferals;
    protected long[] taxes;
    protected long[] dividends;
    protected long[] interest;
    protected double[] accumulated;
    protected double[] delta;
    protected Drawdown drawdown;
    protected Volatility volatility;

    /* package */PerformanceIndex(Client client, CurrencyConverter converter, ReportingPeriod reportInterval)
    {
        this.client = client;
        this.converter = converter;
        this.reportInterval = reportInterval;
    }

    public static ClientIndex forClient(Client client, CurrencyConverter converter, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        ClientIndex index = new ClientIndex(client, converter, reportInterval);
        index.calculate(warnings);
        return index;
    }

    public static PerformanceIndex forAccount(Client client, CurrencyConverter converter, Account account,
                    ReportingPeriod reportInterval, List<Exception> warnings)
    {
        Classification classification = new Classification(null, null);
        classification.addAssignment(new Assignment(account));
        return forClassification(client, converter, classification, reportInterval, warnings);
    }

    public static PerformanceIndex forPortfolio(Client client, CurrencyConverter converter, Portfolio portfolio,
                    ReportingPeriod reportInterval, List<Exception> warnings)
    {
        return PortfolioIndex.calculate(client, converter, portfolio, reportInterval, warnings);
    }

    public static PerformanceIndex forPortfolioPlusAccount(Client client, CurrencyConverter converter,
                    Portfolio portfolio, ReportingPeriod reportInterval, List<Exception> warnings)
    {
        return PortfolioPlusIndex.calculate(client, converter, portfolio, reportInterval, warnings);
    }

    public static PerformanceIndex forClassification(Client client, CurrencyConverter converter,
                    Classification classification, ReportingPeriod reportInterval, List<Exception> warnings)
    {
        return ClassificationIndex.calculate(client, converter, classification, reportInterval, warnings);
    }

    public static PerformanceIndex forInvestment(Client client, CurrencyConverter converter, Security security,
                    ReportingPeriod reportInterval, List<Exception> warnings)
    {
        Classification classification = new Classification(null, null);
        classification.addAssignment(new Assignment(security));
        return forClassification(client, converter, classification, reportInterval, warnings);
    }

    public static PerformanceIndex forSecurity(PerformanceIndex clientIndex, Security security, List<Exception> warnings)
    {
        SecurityIndex index = new SecurityIndex(clientIndex.getClient(), clientIndex.getCurrencyConverter(),
                        clientIndex.getReportInterval());
        index.calculate(clientIndex, security);
        return index;
    }

    public static PerformanceIndex forConsumerPriceIndex(PerformanceIndex clientIndex, List<Exception> warnings)
    {
        CPIIndex index = new CPIIndex(clientIndex.getClient(), clientIndex.getCurrencyConverter(),
                        clientIndex.getReportInterval());
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

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    /**
     * Returns the interval for which data exists. Might be different from
     * {@link #getReportInterval()} if the reporting interval extends into the
     * future.
     */
    public Interval getActualInterval()
    {
        return Interval.of(dates[0], dates[dates.length - 1]);
    }

    public LocalDate[] getDates()
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

    public Drawdown getDrawdown()
    {
        if (drawdown == null)
            drawdown = new Drawdown(accumulated, dates);

        return drawdown;
    }

    public Volatility getVolatility()
    {
        if (volatility == null)
            volatility = new Volatility(delta, filterReturnsForVolatilityCalculation());

        return volatility;
    }

    /**
     * The volatility calculation must excludes returns
     * <ul>
     * <li>on first day (because on the first day the return is always zero as
     * there is no previous day to compare to)</li>
     * <li>on days where there are no holdings including the first day it was
     * bought (for example if the investment vehicle was bought in the middle of
     * the reporting period)</li>
     * <li>on weekends or public holidays</li>
     * </ul>
     */
    private Predicate<Integer> filterReturnsForVolatilityCalculation()
    {
        TradeCalendar calendar = new TradeCalendar();
        return index -> index > 0 && totals[index] != 0 && totals[index - 1] != 0 && !calendar.isHoliday(dates[index]);
    }

    public long[] getTaxes()
    {
        return taxes;
    }

    public long[] getDividends()
    {
        return dividends;
    }

    public long[] getInterest()
    {
        return interest;
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

    public Optional<LocalDate> getFirstDataPoint()
    {
        for (int ii = 0; ii < totals.length; ii++)
        {
            if (totals[ii] != 0)
                return Optional.of(dates[ii]);
        }

        return Optional.empty();
    }

    public void exportTo(File file) throws IOException
    {
        exportTo(file, index -> true);
    }

    public void exportVolatilityData(File file) throws IOException
    {
        exportTo(file, filterReturnsForVolatilityCalculation());
    }

    private void exportTo(File file, Predicate<Integer> filter) throws IOException
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
                if (!filter.test(ii))
                    continue;

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
