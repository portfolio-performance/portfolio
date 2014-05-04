package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

public final class SecurityPerformanceRecord implements Adaptable
{
    public enum Periodicity
    {
        UNKNOWN, NONE, INDEFINITE, ANNUAL, SEMIANNUAL, QUARTERLY, IRREGULAR;

        private static final ResourceBundle RESOURCES = ResourceBundle
                        .getBundle("name.abuchen.portfolio.snapshot.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("dividends." + name()); //$NON-NLS-1$
        }
    }

    private final Security security;
    private List<Transaction> transactions = new ArrayList<Transaction>();

    /**
     * internal rate of return of security {@link #calculateIRR()}
     */
    private double irr;

    /**
     * True time-weighted rate of return
     * {@link #calculatePerformance(ReportingPeriod)}
     */
    private double twror;

    /**
     * delta = market value + sells + dividends - purchase costs
     * {@link #calculateDelta()}
     */
    private long delta;

    /**
     * market value of holdings at end of period
     * {@link #addTransaction(Transaction)}
     */
    private long marketValue;

    /**
     * fifo cost of shares held {@link #calculateFifoCosts()}
     */
    private long fifoCost;

    /**
     * shares held {@link #calculateFifoCosts()}
     */
    private long sharesHeld;

    /**
     * cost per shares held {@link #calculateFifoCosts()}
     */
    private long fifoCostPerSharesHeld;

    /**
     * sum of all dividend payments {@link #calculateDividends()}
     */
    private long sumOfDividends;

    /**
     * number of dividend events during reporting period
     * {@link #calculateDividends()}
     */
    private int dividendEventCount;

    /**
     * last dividend payment in reporting period {@link #calculateDividends()}
     */
    private Date lastDividendPayment;

    /**
     * periodicity of dividend payments {@link #calculateDividends()}
     */
    private Periodicity periodicity = Periodicity.UNKNOWN;

    /* package */SecurityPerformanceRecord(Security security)
    {
        this.security = security;
    }

    public Security getSecurity()
    {
        return security;
    }

    public String getSecurityName()
    {
        return getSecurity().getName();
    }

    public String getNote()
    {
        return getSecurity().getNote();
    }

    public double getIrr()
    {
        return irr;
    }

    public double getTrueTimeWeightedRateOfReturn()
    {
        return twror;
    }

    public long getDelta()
    {
        return delta;
    }

    public long getMarketValue()
    {
        return marketValue;
    }

    public long getFifoCost()
    {
        return fifoCost;
    }

    public long getSharesHeld()
    {
        return sharesHeld;
    }

    public long getFifoCostPerSharesHeld()
    {
        return fifoCostPerSharesHeld;
    }

    public long getSumOfDividends()
    {
        return sumOfDividends;
    }

    public int getDividendEventCount()
    {
        return dividendEventCount;
    }

    public Date getLastDividendPayment()
    {
        return lastDividendPayment;
    }

    public Periodicity getPeriodicity()
    {
        return periodicity;
    }

    public int getPeriodicitySort()
    {
        return periodicity.ordinal();
    }

    public double getTotalRateOfReturnDiv()
    {
        return sharesHeld > 0 ? (double) sumOfDividends / (double) fifoCost : 0;
    }

    public List<Transaction> getTransactions()
    {
        return transactions;
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Security.class)
            return type.cast(security);
        else if (type == Attributable.class)
            return type.cast(security);
        else if (type == Named.class)
            return type.cast(security);
        else
            return null;
    }

    /* package */void addTransaction(Transaction t)
    {
        transactions.add(t);

        if (t instanceof DividendFinalTransaction)
            marketValue = t.getAmount();
    }

    /* package */void calculate(Client client, ReportingPeriod period)
    {
        Collections.sort(transactions, new TransactionComparator());

        if (!transactions.isEmpty())
        {
            calculateIRR();
            calculatePerformance(client, period);
            calculateDelta();
            calculateFifoCosts();
            calculateDividends();
        }
    }

    private void calculateIRR()
    {
        this.irr = Calculation.perform(IRRCalculation.class, transactions).getIRR();
    }

    private void calculatePerformance(Client client, ReportingPeriod period)
    {
        PerformanceIndex index = PerformanceIndex.forInvestment(client, security, period, new ArrayList<Exception>());
        double[] performance = index.getAccumulatedPercentage();
        this.twror = performance.length > 0 ? performance[performance.length - 1] : 0d;
    }

    private void calculateDelta()
    {
        this.delta = Calculation.perform(DeltaCalculation.class, transactions).getDelta();
    }

    private void calculateFifoCosts()
    {
        CostCalculation cost = Calculation.perform(CostCalculation.class, transactions);
        this.fifoCost = cost.getFifoCost();
        this.sharesHeld = cost.getSharesHeld();
        this.fifoCostPerSharesHeld = Math.round(fifoCost * Values.Share.factor() / (double) sharesHeld);
    }

    private void calculateDividends()
    {
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, transactions);
        this.sumOfDividends = dividends.getSum();
        this.dividendEventCount = dividends.getNumOfEvents();
        this.lastDividendPayment = dividends.getLastDividendPayment();
        this.periodicity = dividends.getPeriodicity();
    }
}
