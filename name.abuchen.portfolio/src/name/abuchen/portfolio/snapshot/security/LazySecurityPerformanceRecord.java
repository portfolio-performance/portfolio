package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.function.Supplier;

import name.abuchen.portfolio.math.Risk;
import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.security.CostCalculation.CostCalculationResult;
import name.abuchen.portfolio.util.Interval;

public final class LazySecurityPerformanceRecord extends BaseSecurityPerformanceRecord implements Adaptable
{
    public static final class LazyValue<V>
    {
        private V value;
        private final Supplier<V> computeFunction;

        public LazyValue(Supplier<V> computeFunction)
        {
            this.computeFunction = computeFunction;
        }

        public V get()
        {
            if (value == null)
                value = computeFunction.get();
            return value;
        }
    }

    /**
     * internal rate of return of security {@link #calculateIRR()}
     */
    private LazyValue<Double> irr = new LazyValue<>(
                    () -> Calculation.perform(IRRCalculation.class, converter, security, lineItems).getIRR());

    private LazyValue<PerformanceIndex> performanceIndex = new LazyValue<>(
                    () -> PerformanceIndex.forInvestment(client, converter, security, interval, new ArrayList<>()));

    /**
     * True time-weighted rate of return
     */
    private LazyValue<Double> twror = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getFinalAccumulatedPercentage();
    });

    /**
     * Annualized True time-weighted rate of return
     */
    private LazyValue<Double> twrorpa = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getFinalAccumulatedAnnualizedPercentage();
    });

    /**
     * Max Drawdown and Max Drawdown Duration
     */
    private LazyValue<Risk.Drawdown> drawdown = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getDrawdown();
    });

    /**
     * Volatility and semi-volatility
     */
    private LazyValue<Risk.Volatility> volatility = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getVolatility();
    });

    private LazyValue<DeltaCalculation> deltaCalculation = new LazyValue<>(
                    () -> Calculation.perform(DeltaCalculation.class, converter, security, lineItems));

    /**
     * delta = market value + sells + dividends - purchase costs
     */
    private LazyValue<Money> delta = new LazyValue<>(() -> deltaCalculation.get().getDelta());

    /**
     * deltaPercent = delta / purchase costs + buy
     */
    private LazyValue<Double> deltaPercent = new LazyValue<>(() -> deltaCalculation.get().getDeltaPercent());

    /**
     * market value of holdings at end of period
     */
    // in order to minimize rounding error, first sum up individual values
    // and convert only then into the term currency
    private LazyValue<Money> marketValue = new LazyValue<>(() -> this.lineItems.stream() //
                    .filter(data -> data instanceof CalculationLineItem.ValuationAtEnd)
                    .map(CalculationLineItem::getValue) //
                    .collect(MoneyCollectors.sum(security.getCurrencyCode())) //
                    .with(converter.at(interval.getEnd())));

    private LazyValue<CostCalculationResult> costCalculation = new LazyValue<>(
                    () -> Calculation.perform(CostCalculation.class, converter, security, lineItems).getResult());

    /**
     * fifo cost of shares held
     */
    private LazyValue<Money> fifoCost = new LazyValue<>(() -> costCalculation.get().fifoCost());

    /**
     * moving average cost of shares held
     */
    private LazyValue<Money> movingAverageCost = new LazyValue<>(() -> costCalculation.get().movingAverageCost());

    /**
     * market value - fifo cost of shares held
     */
    private LazyValue<Money> capitalGainsOnHoldings = new LazyValue<>(
                    () -> marketValue.get().subtract(costCalculation.get().fifoCost()));

    /**
     * {@link capitalGainsOnHoldings} in percent
     */
    private LazyValue<Double> capitalGainsOnHoldingsPercent = new LazyValue<>(() -> {
        var mv = marketValue.get();
        var cost = costCalculation.get().fifoCost();

        if (mv.getAmount() == 0L && cost.getAmount() == 0L)
            return 0d;
        else
            return ((double) mv.getAmount() / (double) cost.getAmount()) - 1;
    });

    /**
     * market value - moving average cost of shares held
     */
    private LazyValue<Money> capitalGainsOnHoldingsMovingAverage = new LazyValue<>(
                    () -> marketValue.get().subtract(costCalculation.get().movingAverageCost()));

    /**
     * {@link capitalGainsOnHoldingsMovingAverage} in percent
     */
    private LazyValue<Double> capitalGainsOnHoldingsMovingAveragePercent = new LazyValue<>(() -> {
        var mv = marketValue.get();
        var cost = costCalculation.get().movingAverageCost();

        if (mv.getAmount() == 0L && cost.getAmount() == 0L)
            return 0d;
        else
            return ((double) mv.getAmount() / (double) cost.getAmount()) - 1;
    });

    /**
     * cost per shares held
     */
    private LazyValue<Quote> fifoCostPerSharesHeld = new LazyValue<>(() -> {
        var costs = costCalculation.get();
        return Quote.of(costs.netFifoCost().getCurrencyCode(), Math.round(costs.netFifoCost().getAmount()
                        / (double) costs.sharesHeld() * Values.Share.factor() * Values.Quote.factorToMoney()));
    });

    /**
     * cost per shares held
     */
    private LazyValue<Quote> movingAverageCostPerSharesHeld = new LazyValue<>(() -> {
        var costs = costCalculation.get();

        Money netMovingAverageCost = costs.netMovingAverageCost();
        return Quote.of(netMovingAverageCost.getCurrencyCode(), Math.round(netMovingAverageCost.getAmount()
                        / (double) costs.sharesHeld() * Values.Share.factor() * Values.Quote.factorToMoney()));
    });

    private LazyValue<Money> sumOfDividends = new LazyValue<>(
                    () -> Calculation.perform(DividendCalculation.class, converter, security, lineItems).getSum());

    /* package */ LazySecurityPerformanceRecord(Client client, Security security, CurrencyConverter converter,
                    Interval interval)
    {
        super(client, security, converter, interval);
    }

    public LazyValue<Double> getIrr()
    {
        return irr;
    }

    public LazyValue<Double> getTrueTimeWeightedRateOfReturn()
    {
        return twror;
    }

    public LazyValue<Double> getTrueTimeWeightedRateOfReturnAnnualized()
    {
        return twrorpa;
    }

    public LazyValue<Drawdown> getDrawdown()
    {
        return drawdown;
    }

    public LazyValue<Volatility> getVolatility()
    {
        return volatility;
    }

    public LazyValue<Money> getDelta()
    {
        return delta;
    }

    public LazyValue<Double> getDeltaPercent()
    {
        return deltaPercent;
    }

    public LazyValue<Money> getMarketValue()
    {
        return marketValue;
    }

    public LazyValue<Money> getFifoCost()
    {
        return fifoCost;
    }

    public LazyValue<Money> getMovingAverageCost()
    {
        return movingAverageCost;
    }

    public LazyValue<Money> getCapitalGainsOnHoldings()
    {
        return capitalGainsOnHoldings;
    }

    public LazyValue<Double> getCapitalGainsOnHoldingsPercent()
    {
        return capitalGainsOnHoldingsPercent;
    }

    public LazyValue<Money> getCapitalGainsOnHoldingsMovingAverage()
    {
        return capitalGainsOnHoldingsMovingAverage;
    }

    public LazyValue<Double> getCapitalGainsOnHoldingsMovingAveragePercent()
    {
        return capitalGainsOnHoldingsMovingAveragePercent;
    }

    public LazyValue<Quote> getFifoCostPerSharesHeld()
    {
        return fifoCostPerSharesHeld;
    }

    public LazyValue<Quote> getMovingAverageCostPerSharesHeld()
    {
        return movingAverageCostPerSharesHeld;
    }

    public LazyValue<Money> getSumOfDividends()
    {
        return sumOfDividends;
    }

    public LazyValue<Double> getTotalRateOfReturnDiv()
    {
        return new LazyValue<>(() -> {
            var costs = costCalculation.get();
            return costs.sharesHeld() > 0
                            ? (double) sumOfDividends.get().getAmount() / (double) costs.fifoCost().getAmount()
                            : 0;
        });
    }

    public LazyValue<Double> getTotalRateOfReturnDivMovingAverage()
    {
        return new LazyValue<>(() -> {
            var costs = costCalculation.get();
            return costs.sharesHeld() > 0
                            ? (double) sumOfDividends.get().getAmount() / (double) costs.movingAverageCost().getAmount()
                            : 0;
        });
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
}
