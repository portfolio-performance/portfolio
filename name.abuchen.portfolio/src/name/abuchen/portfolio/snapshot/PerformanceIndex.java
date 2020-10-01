package name.abuchen.portfolio.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.function.ToLongBiFunction;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class PerformanceIndex
{
    private final Client client;
    private final CurrencyConverter converter;
    private final Interval reportInterval;

    protected LocalDate[] dates;
    protected long[] totals;
    protected long[] inboundTransferals;
    protected long[] outboundTransferals;
    protected long[] taxes;
    protected long[] dividends;
    protected long[] interest;
    protected long[] interestCharge;
    protected long[] buys;
    protected long[] sells;
    protected double[] accumulated;
    protected double[] delta;

    private Drawdown drawdown;
    private Volatility volatility;
    private ClientPerformanceSnapshot performanceSnapshot;

    /* package */ PerformanceIndex(Client client, CurrencyConverter converter, Interval reportInterval)
    {
        this.client = client;
        this.converter = converter;
        this.reportInterval = reportInterval;
    }

    public static PerformanceIndex forClient(Client client, CurrencyConverter converter, Interval reportInterval,
                    List<Exception> warnings)
    {
        ClientIndex index = new ClientIndex(client, converter, reportInterval);
        index.calculate(warnings);
        return index;
    }

    public static PerformanceIndex forAccount(Client client, CurrencyConverter converter, Account account,
                    Interval reportInterval, List<Exception> warnings)
    {
        Client pseudoClient = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(account)).filter(client);
        return PerformanceIndex.forClient(pseudoClient, converter, reportInterval, warnings);
    }

    public static PerformanceIndex forPortfolio(Client client, CurrencyConverter converter, Portfolio portfolio,
                    Interval reportInterval, List<Exception> warnings)
    {
        Client pseudoClient = new PortfolioClientFilter(portfolio).filter(client);
        return PerformanceIndex.forClient(pseudoClient, converter, reportInterval, warnings);
    }

    public static PerformanceIndex forPortfolioPlusAccount(Client client, CurrencyConverter converter,
                    Portfolio portfolio, Interval reportInterval, List<Exception> warnings)
    {
        Client pseudoClient = new PortfolioClientFilter(portfolio, portfolio.getReferenceAccount()).filter(client);
        return PerformanceIndex.forClient(pseudoClient, converter, reportInterval, warnings);
    }

    public static PerformanceIndex forClassification(Client client, CurrencyConverter converter,
                    Classification classification, Interval reportInterval, List<Exception> warnings)
    {
        Client filteredClient = new ClientClassificationFilter(classification).filter(client);
        return PerformanceIndex.forClient(filteredClient, converter, reportInterval, warnings);
    }

    public static PerformanceIndex forInvestment(Client client, CurrencyConverter converter, Security security,
                    Interval reportInterval, List<Exception> warnings)
    {
        Client filteredClient = new ClientSecurityFilter(security).filter(client);
        return forClient(filteredClient, converter, reportInterval, warnings);
    }

    public static PerformanceIndex forSecurity(PerformanceIndex clientIndex, Security security)
    {
        SecurityIndex index = new SecurityIndex(clientIndex, security);
        index.calculate();
        return index;
    }

    /* package */
    Client getClient()
    {
        return client;
    }

    public Interval getReportInterval()
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
        long[] transferals = new long[inboundTransferals.length];
        for (int ii = 0; ii < transferals.length; ii++)
            transferals[ii] = inboundTransferals[ii] - outboundTransferals[ii];

        return transferals;
    }

    public long[] getInboundTransferals()
    {
        return inboundTransferals;
    }

    public long[] getOutboundTransferals()
    {
        return outboundTransferals;
    }

    public Drawdown getDrawdown()
    {
        if (drawdown == null)
        {
            int startAt = 0;
            for (; startAt < totals.length; startAt++)
                if (totals[startAt] != 0)
                    break;

            if (startAt == totals.length)
                startAt = totals.length - 1;

            drawdown = new Drawdown(accumulated, dates, startAt);
        }

        return drawdown;
    }

    public Volatility getVolatility()
    {
        if (volatility == null)
            volatility = new Volatility(delta, filterReturnsForVolatilityCalculation());

        return volatility;
    }

    /**
     * The volatility calculation must exclude returns
     * <ul>
     * <li>on first day (because on the first day the return is always zero as
     * there is no previous day to compare to)</li>
     * <li>on days where there are no holdings including the first day it was
     * bought (for example if the investment vehicle was bought in the middle of
     * the reporting period)</li>
     * <li>on weekends or public holidays</li>
     * </ul>
     */
    private IntPredicate filterReturnsForVolatilityCalculation()
    {
        TradeCalendar calendar = TradeCalendarManager.getDefaultInstance();
        return index -> index > 0 && totals[index] != 0 && totals[index - 1] != 0 && !calendar.isHoliday(dates[index]);
    }

    /**
     * Returns the ClientPerformanceSnapshot if available. The snapshot is not
     * available for benchmarks and the consumer price indices.
     */
    public Optional<ClientPerformanceSnapshot> getClientPerformanceSnapshot()
    {
        if (performanceSnapshot == null)
            performanceSnapshot = new ClientPerformanceSnapshot(client, converter, reportInterval);

        return Optional.of(performanceSnapshot);
    }

    public double getPerformanceIRR()
    {
        return getClientPerformanceSnapshot().map(ClientPerformanceSnapshot::getPerformanceIRR)
                        .orElseThrow(IllegalArgumentException::new);
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

    public long[] getInterestCharge()
    {
        return interestCharge;
    }

    public long[] getBuys()
    {
        return buys;
    }

    public long[] getSells()
    {
        return sells;
    }

    /**
     * Calculates the absolute invested capital, i.e. starting with the first
     * transaction recorded for the client.
     */
    public long[] calculateAbsoluteInvestedCapital()
    {
        ToLongBiFunction<Money, LocalDateTime> convertIfNecessary = (amount, date) -> {
            if (amount.getCurrencyCode().equals(getCurrencyConverter().getTermCurrency()))
                return amount.getAmount();
            else
                return getCurrencyConverter().convert(date, amount).getAmount();
        };

        long startValue = 0;
        Interval interval = getActualInterval();

        LocalDate intervalStart = interval.getStart();

        for (Account account : getClient().getAccounts())
            startValue += account.getTransactions() //
                            .stream() //
                            .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT
                                            || t.getType() == AccountTransaction.Type.REMOVAL)
                            .filter(t -> !t.getDateTime().toLocalDate().isAfter(intervalStart)) //
                            .mapToLong(t -> {
                                if (t.getType() == AccountTransaction.Type.DEPOSIT)
                                    return convertIfNecessary.applyAsLong(t.getMonetaryAmount(), t.getDateTime());
                                else if (t.getType() == AccountTransaction.Type.REMOVAL)
                                    return -convertIfNecessary.applyAsLong(t.getMonetaryAmount(), t.getDateTime());
                                else
                                    return 0;
                            }).sum();

        for (Portfolio portfolio : getClient().getPortfolios())
            startValue += portfolio.getTransactions() //
                            .stream() //
                            .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                            || t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                            .filter(t -> !t.getDateTime().toLocalDate().isAfter(intervalStart)) //
                            .mapToLong(t -> {
                                if (t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                                    return convertIfNecessary.applyAsLong(t.getMonetaryAmount(), t.getDateTime());
                                else if (t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                                    return -convertIfNecessary.applyAsLong(t.getMonetaryAmount(), t.getDateTime());
                                else
                                    return 0;
                            }).sum();

        return calculateInvestedCapital(startValue);
    }

    /**
     * Calculates the invested capital for the given reporting period.
     */
    public long[] calculateInvestedCapital()
    {
        return calculateInvestedCapital(totals[0]);
    }

    private long[] calculateInvestedCapital(long startValue)
    {
        long[] investedCapital = new long[inboundTransferals.length];

        investedCapital[0] = startValue;
        long current = startValue;
        for (int ii = 1; ii < investedCapital.length; ii++)
            current = investedCapital[ii] = current + inboundTransferals[ii] - outboundTransferals[ii];

        return investedCapital;
    }

    /**
     * Calculates the delta as difference between the current valuation and the
     * invested capital since the first transaction.
     */
    public long[] calculateAbsoluteDelta()
    {
        return calculateDelta(calculateAbsoluteInvestedCapital());
    }

    /**
     * Calculates the delta as difference between the total valuation and the
     * invested capital for the given reporting period.
     */
    public long[] calculateDelta()
    {
        return calculateDelta(calculateInvestedCapital());
    }

    private long[] calculateDelta(long[] investedCapital)
    {
        long[] answer = investedCapital;

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

    private void exportTo(File file, IntPredicate filter) throws IOException
    {
        CSVFormat csvformat = CSVFormat //
                        .newFormat(';').withQuote('"').withRecordSeparator("\r\n").withAllowDuplicateHeaderNames(); //$NON-NLS-1$

        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), csvformat))
        {
            printer.printRecord(Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_InboundTransferals, //
                            Messages.CSVColumn_OutboundTransferals, //
                            Messages.CSVColumn_DeltaInPercent, //
                            Messages.CSVColumn_CumulatedPerformanceInPercent);

            for (int ii = 0; ii < totals.length; ii++)
            {
                if (!filter.test(ii))
                    continue;

                printer.print(dates[ii].toString());
                printer.print(Values.Amount.format(totals[ii]));
                printer.print(Values.Amount.format(inboundTransferals[ii]));
                printer.print(Values.Amount.format(outboundTransferals[ii]));
                printer.print(Values.Percent.format(delta[ii]));
                printer.print(Values.Percent.format(accumulated[ii]));
                printer.println();
            }
        }
    }
}
