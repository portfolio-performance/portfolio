package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.math.Risk;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailProvider;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;
import name.abuchen.portfolio.util.Interval;

public final class SecurityPerformanceRecord implements Adaptable, TrailProvider, SecurityPerformanceIndicator.Costs
{
    public enum Periodicity
    {
        UNKNOWN, NONE, INDEFINITE, ANNUAL, SEMIANNUAL, QUARTERLY, MONTHLY, IRREGULAR;

        private static final ResourceBundle RESOURCES = ResourceBundle
                        .getBundle("name.abuchen.portfolio.snapshot.labels"); //$NON-NLS-1$

        @Override
        public String toString()
        {
            return RESOURCES.getString("dividends." + name()); //$NON-NLS-1$
        }
    }

    public interface Trails // NOSONAR
    {
        String FIFO_COST = "fifoCost"; //$NON-NLS-1$
        String REALIZED_CAPITAL_GAINS = "realizedCapitalGains"; //$NON-NLS-1$
        String REALIZED_CAPITAL_GAINS_FOREX = "realizedCapitalGainsForex"; //$NON-NLS-1$
        String UNREALIZED_CAPITAL_GAINS = "unrealizedCapitalGains"; //$NON-NLS-1$
        String UNREALIZED_CAPITAL_GAINS_FOREX = "unrealizedCapitalGainsForex"; //$NON-NLS-1$
    }

    /* package */ static final class Builder
    {
        private final Security security;
        private final List<CalculationLineItem> lineItems = new ArrayList<>();

        public Builder(Security security)
        {
            this.security = security;
        }

        public void addLineItem(CalculationLineItem item)
        {
            this.lineItems.add(item);
        }

        public boolean isEmpty()
        {
            return this.lineItems.isEmpty();
        }

        @SafeVarargs
        public final SecurityPerformanceRecord build(Client client, CurrencyConverter converter, Interval interval,
                        Class<? extends SecurityPerformanceIndicator>... indicators)
        {
            SecurityPerformanceRecord record = new SecurityPerformanceRecord(security, lineItems);
            record.calculate(client, converter, interval, indicators);
            return record;
        }
    }

    private final Security security;
    private final List<CalculationLineItem> lineItems;

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
     * Max Drawdown and Max Drawdown Duration
     * {@link #calculatePerformance(ReportingPeriod)}
     */
    private Risk.Drawdown drawdown;

    /**
     * Volatility and semi-volatility
     * {@link #calculatePerformance(ReportingPeriod)}
     */
    private Risk.Volatility volatility;

    /**
     * delta = market value + sells + dividends - purchase costs
     * {@link #calculateDelta()}
     */
    private Money delta;

    /**
     * deltaPercent = delta / purchase costs + buy {@link #calculateDelta()}
     */
    private double deltaPercent;

    /**
     * market value of holdings at end of period
     * {@link #addTransaction(Transaction)}
     */
    private Money marketValue;

    /**
     * Latest quote
     */
    private SecurityPrice quote;

    /**
     * fifo cost of shares held {@link #calculateFifoAndMovingAverageCosts()}
     */
    private Money fifoCost;
    private TrailRecord fifoCostTrail;

    /**
     * moving average cost of shares held
     * {@link #calculateFifoAndMovingAverageCosts()}
     */
    private Money movingAverageCost;

    /**
     * fees paid
     */
    private Money fees;

    /**
     * taxes paid
     */
    private Money taxes;

    /**
     * shares held {@link #calculateFifoAndMovingAverageCosts()}
     */
    private long sharesHeld;

    /**
     * cost per shares held {@link #calculateFifoAndMovingAverageCosts()}
     */
    private Quote fifoCostPerSharesHeld;

    /**
     * cost per shares held {@link #calculateFifoAndMovingAverageCosts()}
     */
    private Quote movingAverageCostPerSharesHeld;

    /**
     * sum of all dividend payments {@link #calculateDividends()}
     */
    private Money sumOfDividends;

    /**
     * number of dividend events during reporting period
     * {@link #calculateDividends()}
     */
    private int dividendEventCount;

    /**
     * last dividend payment in reporting period {@link #calculateDividends()}
     */
    private LocalDate lastDividendPayment;

    /**
     * periodicity of dividend payments {@link #calculateDividends()}
     */
    private Periodicity periodicity = Periodicity.UNKNOWN;

    /**
     * rate of return per year {@link #calculateDividends()}
     */
    private double rateOfReturnPerYear;

    /**
     * market value - fifo cost of shares held
     * {@link #calculateFifoAndMovingAverageCosts()}
     */
    private Money capitalGainsOnHoldings;

    /**
     * {@link capitalGainsOnHoldings} in percent
     */
    private double capitalGainsOnHoldingsPercent;

    /**
     * market value - moving average cost of shares held
     * {@link #calculateFifoAndMovingAverageCosts()}
     */
    private Money capitalGainsOnHoldingsMovingAverage;

    /**
     * {@link capitalGainsOnHoldingsMovingAverage} in percent
     */
    private double capitalGainsOnHoldingsMovingAveragePercent;

    private CapitalGainsRecord realizedCapitalGains;
    private CapitalGainsRecord unrealizedCapitalGains;

    private SecurityPerformanceRecord(Security security, List<CalculationLineItem> lineItems)
    {
        this.security = security;
        this.lineItems = lineItems;
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

    public double getMaxDrawdown()
    {
        return drawdown.getMaxDrawdown();
    }

    public long getMaxDrawdownDuration()
    {
        return drawdown.getMaxDrawdownDuration().getDays();
    }

    public double getVolatility()
    {
        return volatility.getStandardDeviation();
    }

    public double getSemiVolatility()
    {
        return volatility.getSemiDeviation();
    }

    public Money getDelta()
    {
        return delta;
    }

    public double getDeltaPercent()
    {
        return deltaPercent;
    }

    public Money getMarketValue()
    {
        return marketValue;
    }

    public Quote getQuote()
    {
        return Quote.of(security.getCurrencyCode(), quote.getValue());
    }

    public SecurityPrice getLatestSecurityPrice()
    {
        return quote;
    }

    @Override
    public Money getFifoCost()
    {
        return fifoCost;
    }

    @Override
    public Money getMovingAverageCost()
    {
        return movingAverageCost;
    }

    public Money getCapitalGainsOnHoldings()
    {
        return capitalGainsOnHoldings;
    }

    public double getCapitalGainsOnHoldingsPercent()
    {
        return capitalGainsOnHoldingsPercent;
    }

    public Money getCapitalGainsOnHoldingsMovingAverage()
    {
        return capitalGainsOnHoldingsMovingAverage;
    }

    public double getCapitalGainsOnHoldingsMovingAveragePercent()
    {
        return capitalGainsOnHoldingsMovingAveragePercent;
    }

    public Money getFees()
    {
        return fees;
    }

    public Money getTaxes()
    {
        return taxes;
    }

    public long getSharesHeld()
    {
        return sharesHeld;
    }

    @Override
    public Quote getFifoCostPerSharesHeld()
    {
        return fifoCostPerSharesHeld;
    }

    @Override
    public Quote getMovingAverageCostPerSharesHeld()
    {
        return movingAverageCostPerSharesHeld;
    }

    public Money getSumOfDividends()
    {
        return sumOfDividends;
    }

    public int getDividendEventCount()
    {
        return dividendEventCount;
    }

    public LocalDate getLastDividendPayment()
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

    /**
     * Gets the rate of return of dividends per year as a percentage of
     * invested.
     * 
     * @return rate of return per year on success, else 0
     */
    public double getRateOfReturnPerYear()
    {
        return this.rateOfReturnPerYear;
    }

    public double getTotalRateOfReturnDiv()
    {
        return sharesHeld > 0 ? (double) sumOfDividends.getAmount() / (double) fifoCost.getAmount() : 0;
    }

    public double getTotalRateOfReturnDivMovingAverage()
    {
        return sharesHeld > 0 ? (double) sumOfDividends.getAmount() / (double) movingAverageCost.getAmount() : 0;
    }

    public CapitalGainsRecord getRealizedCapitalGains()
    {
        return realizedCapitalGains;
    }

    public CapitalGainsRecord getUnrealizedCapitalGains()
    {
        return unrealizedCapitalGains;
    }

    public List<CalculationLineItem> getLineItems()
    {
        return lineItems;
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
        else if (type == InvestmentVehicle.class)
            return type.cast(security);
        else if (type == Annotated.class)
            return type.cast(security);
        else
            return null;
    }

    @Override
    public Optional<Trail> explain(String key)
    {
        switch (key)
        {
            case Trails.FIFO_COST:
                return Trail.of(getSecurityName(), fifoCostTrail);
            case Trails.REALIZED_CAPITAL_GAINS:
                return Trail.of(getSecurityName(), getRealizedCapitalGains().getCapitalGainsTrail());
            case Trails.REALIZED_CAPITAL_GAINS_FOREX:
                return Trail.of(getSecurityName(), getRealizedCapitalGains().getForexCapitalGainsTrail());
            case Trails.UNREALIZED_CAPITAL_GAINS:
                return Trail.of(getSecurityName(), getUnrealizedCapitalGains().getCapitalGainsTrail());
            case Trails.UNREALIZED_CAPITAL_GAINS_FOREX:
                return Trail.of(getSecurityName(), getUnrealizedCapitalGains().getForexCapitalGainsTrail());
            default:
                return Optional.empty();
        }
    }

    @SafeVarargs
    /* package */ final void calculate(Client client, CurrencyConverter converter, Interval interval,
                    Class<? extends SecurityPerformanceIndicator>... indicators)
    {
        Collections.sort(lineItems, new CalculationLineItemComparator());

        Set<Class<? extends SecurityPerformanceIndicator>> flags = new HashSet<>();
        Arrays.stream(indicators).forEach(flags::add);

        if (!lineItems.isEmpty())
        {
            if (flags.isEmpty() || flags.contains(SecurityPerformanceIndicator.Costs.class))
            {
                calculateSharesHeld(converter);
                calculateMarketValue(converter, interval);
            }

            if (flags.isEmpty())
            {
                calculateIRR(converter);
                calculateTTWROR(client, converter, interval);
                calculateDelta(converter);
            }

            if (flags.isEmpty() || flags.contains(SecurityPerformanceIndicator.Costs.class))
            {
                calculateFifoAndMovingAverageCosts(converter);
            }

            if (flags.isEmpty())
            {
                calculateDividends(converter);
                calculatePeriodicity(client, converter);
            }

            if (flags.isEmpty() || flags.contains(SecurityPerformanceIndicator.CapitalGains.class))
            {
                calculateCapitalGains(converter);
            }
        }
    }

    private void calculateSharesHeld(CurrencyConverter converter)
    {
        this.sharesHeld = Calculation.perform(SharesHeldCalculation.class, converter, security, lineItems)
                        .getSharesHeld();
    }

    private void calculateMarketValue(CurrencyConverter converter, Interval interval)
    {
        // in order to minimize rounding error, first sum up individual values
        // and convert only then into the term currency

        this.marketValue = this.lineItems.stream() //
                        .filter(data -> data instanceof CalculationLineItem.ValuationAtEnd)
                        .map(CalculationLineItem::getValue) //
                        .collect(MoneyCollectors.sum(security.getCurrencyCode())) //
                        .with(converter.at(interval.getEnd()));

        this.quote = security.getSecurityPrice(interval.getEnd());
    }

    private void calculateIRR(CurrencyConverter converter)
    {
        this.irr = Calculation.perform(IRRCalculation.class, converter, security, lineItems).getIRR();
    }

    private void calculateTTWROR(Client client, CurrencyConverter converter, Interval interval)
    {
        PerformanceIndex index = PerformanceIndex.forInvestment(client, converter, security, interval,
                        new ArrayList<Exception>());
        this.twror = index.getFinalAccumulatedPercentage();
        this.drawdown = index.getDrawdown();
        this.volatility = index.getVolatility();
    }

    private void calculateDelta(CurrencyConverter converter)
    {
        DeltaCalculation calculation = Calculation.perform(DeltaCalculation.class, converter, security, lineItems);
        this.delta = calculation.getDelta();
        this.deltaPercent = calculation.getDeltaPercent();
    }

    private void calculateFifoAndMovingAverageCosts(CurrencyConverter converter)
    {
        CostCalculation cost = Calculation.perform(CostCalculation.class, converter, security, lineItems);
        this.fifoCost = cost.getFifoCost();
        this.fifoCostTrail = cost.getFifoCostTrail();
        this.movingAverageCost = cost.getMovingAverageCost();

        Money netFifoCost = cost.getNetFifoCost();

        this.fifoCostPerSharesHeld = Quote.of(netFifoCost.getCurrencyCode(), Math.round(netFifoCost.getAmount()
                        / (double) sharesHeld * Values.Share.factor() * Values.Quote.factorToMoney()));
        Money netMovingAverageCost = cost.getNetMovingAverageCost();
        this.movingAverageCostPerSharesHeld = Quote.of(netMovingAverageCost.getCurrencyCode(),
                        Math.round(netMovingAverageCost.getAmount() / (double) sharesHeld * Values.Share.factor()
                                        * Values.Quote.factorToMoney()));

        this.fees = cost.getFees();
        this.taxes = cost.getTaxes();

        this.capitalGainsOnHoldings = marketValue.subtract(fifoCost);
        this.capitalGainsOnHoldingsMovingAverage = marketValue.subtract(movingAverageCost);

        // avoid NaN for securities with no holdings
        if (marketValue.getAmount() == 0L && fifoCost.getAmount() == 0L)
            this.capitalGainsOnHoldingsPercent = 0d;
        else
            this.capitalGainsOnHoldingsPercent = ((double) marketValue.getAmount() / (double) fifoCost.getAmount()) - 1;
        if (marketValue.getAmount() == 0L && movingAverageCost.getAmount() == 0L)
            this.capitalGainsOnHoldingsMovingAveragePercent = 0d;
        else
            this.capitalGainsOnHoldingsMovingAveragePercent = ((double) marketValue.getAmount()
                            / (double) movingAverageCost.getAmount()) - 1;
    }

    private void calculateDividends(CurrencyConverter converter)
    {
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security, lineItems);
        this.sumOfDividends = dividends.getSum();
        this.dividendEventCount = dividends.getNumOfEvents();
        this.lastDividendPayment = dividends.getLastDividendPayment();
        this.rateOfReturnPerYear = dividends.getRateOfReturnPerYear();
    }

    private void calculatePeriodicity(Client client, CurrencyConverter converter)
    {
        // periodicity is calculated by looking at all dividend transactions, so
        // collect all of them instead of using just a fraction in the current
        // filter
        List<CalculationLineItem> allDividendPayments = security.getTransactions(client).stream()
                        .filter(t -> t.getTransaction() instanceof AccountTransaction) //
                        .filter(t -> {
                            AccountTransaction.Type type = ((AccountTransaction) t.getTransaction()).getType();
                            return type == Type.DIVIDENDS;
                        }) //
                        .map(CalculationLineItem::of) //
                        .collect(Collectors.toList());

        DividendCalculation allDividends = Calculation.perform(DividendCalculation.class, converter, security,
                        allDividendPayments);
        this.periodicity = allDividends.getPeriodicity();
    }

    private void calculateCapitalGains(CurrencyConverter converter)
    {
        CapitalGainsCalculation calculation = Calculation.perform(CapitalGainsCalculation.class, converter, security,
                        lineItems);
        this.realizedCapitalGains = calculation.getRealizedCapitalGains();
        this.unrealizedCapitalGains = calculation.getUnrealizedCapitalGains();
    }
}
