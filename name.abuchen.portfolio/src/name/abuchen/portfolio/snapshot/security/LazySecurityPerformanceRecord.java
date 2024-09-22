package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
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
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.security.CostCalculation.CostCalculationResult;
import name.abuchen.portfolio.snapshot.security.DividendCalculation.DividendCalculationResult;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailProvider;
import name.abuchen.portfolio.util.Interval;

public final class LazySecurityPerformanceRecord extends BaseSecurityPerformanceRecord
                implements Adaptable, TrailProvider
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
    private final LazyValue<Double> irr = new LazyValue<>(
                    () -> Calculation.perform(IRRCalculation.class, converter, security, lineItems).getIRR());

    private final LazyValue<PerformanceIndex> performanceIndex = new LazyValue<>(
                    () -> PerformanceIndex.forInvestment(client, converter, security, interval, new ArrayList<>()));

    /**
     * True time-weighted rate of return
     */
    private final LazyValue<Double> twror = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getFinalAccumulatedPercentage();
    });

    /**
     * Annualized True time-weighted rate of return
     */
    private final LazyValue<Double> twrorpa = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getFinalAccumulatedAnnualizedPercentage();
    });

    /**
     * Max Drawdown and Max Drawdown Duration
     */
    private final LazyValue<Risk.Drawdown> drawdown = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getDrawdown();
    });

    /**
     * Volatility and semi-volatility
     */
    private final LazyValue<Risk.Volatility> volatility = new LazyValue<>(() -> {
        var index = performanceIndex.get();
        return index.getVolatility();
    });

    private final LazyValue<DeltaCalculation> deltaCalculation = new LazyValue<>(
                    () -> Calculation.perform(DeltaCalculation.class, converter, security, lineItems));

    /**
     * delta = market value + sells + dividends - purchase costs
     */
    private final LazyValue<Money> delta = new LazyValue<>(() -> deltaCalculation.get().getDelta());

    /**
     * deltaPercent = delta / purchase costs + buy
     */
    private final LazyValue<Double> deltaPercent = new LazyValue<>(() -> deltaCalculation.get().getDeltaPercent());

    /**
     * market value of holdings at end of period
     */
    // in order to minimize rounding error, first sum up individual values
    // and convert only then into the term currency
    private final LazyValue<Money> marketValue = new LazyValue<>(() -> this.lineItems.stream() //
                    .filter(data -> data instanceof CalculationLineItem.ValuationAtEnd)
                    .map(CalculationLineItem::getValue) //
                    .collect(MoneyCollectors.sum(security.getCurrencyCode())) //
                    .with(converter.at(interval.getEnd())));

    /**
     * Latest quote
     */
    private final LazyValue<SecurityPrice> quote = new LazyValue<>(() -> security.getSecurityPrice(interval.getEnd()));

    private final LazyValue<CostCalculationResult> costCalculation = new LazyValue<>(
                    () -> Calculation.perform(CostCalculation.class, converter, security, lineItems).getResult());

    /**
     * fifo cost of shares held
     */
    private final LazyValue<Money> fifoCost = new LazyValue<>(() -> costCalculation.get().fifoCost());

    /**
     * moving average cost of shares held
     */
    private final LazyValue<Money> movingAverageCost = new LazyValue<>(() -> costCalculation.get().movingAverageCost());

    /**
     * market value - fifo cost of shares held
     */
    private final LazyValue<Money> capitalGainsOnHoldings = new LazyValue<>(
                    () -> marketValue.get().subtract(costCalculation.get().fifoCost()));

    /**
     * {@link capitalGainsOnHoldings} in percent
     */
    private final LazyValue<Double> capitalGainsOnHoldingsPercent = new LazyValue<>(() -> {
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
    private final LazyValue<Money> capitalGainsOnHoldingsMovingAverage = new LazyValue<>(
                    () -> marketValue.get().subtract(costCalculation.get().movingAverageCost()));

    /**
     * {@link capitalGainsOnHoldingsMovingAverage} in percent
     */
    private final LazyValue<Double> capitalGainsOnHoldingsMovingAveragePercent = new LazyValue<>(() -> {
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
    private final LazyValue<Quote> fifoCostPerSharesHeld = new LazyValue<>(() -> {
        var costs = costCalculation.get();
        return Quote.of(costs.netFifoCost().getCurrencyCode(), Math.round(costs.netFifoCost().getAmount()
                        / (double) costs.sharesHeld() * Values.Share.factor() * Values.Quote.factorToMoney()));
    });

    /**
     * cost per shares held
     */
    private final LazyValue<Quote> movingAverageCostPerSharesHeld = new LazyValue<>(() -> {
        var costs = costCalculation.get();

        Money netMovingAverageCost = costs.netMovingAverageCost();
        return Quote.of(netMovingAverageCost.getCurrencyCode(), Math.round(netMovingAverageCost.getAmount()
                        / (double) costs.sharesHeld() * Values.Share.factor() * Values.Quote.factorToMoney()));
    });

    private final LazyValue<DividendCalculationResult> dividendCalculation = new LazyValue<>(() -> {
        // ensure cost calculation is done (and has calculated
        // moving averages)
        costCalculation.get();
        return Calculation.perform(DividendCalculation.class, converter, security, lineItems).getResult();
    });

    private final LazyValue<CapitalGainsCalculation> capitalGains = new LazyValue<>(
                    () -> Calculation.perform(CapitalGainsCalculation.class, converter, security, lineItems));

    private final LazyValue<CapitalGainsRecord> realizedCapitalGains = new LazyValue<>(
                    () -> capitalGains.get().getRealizedCapitalGains());
    private final LazyValue<CapitalGainsRecord> unrealizedCapitalGains = new LazyValue<>(
                    () -> capitalGains.get().getUnrealizedCapitalGains());

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

    public LazyValue<Quote> getQuote()
    {
        return new LazyValue<>(() -> Quote.of(security.getCurrencyCode(), quote.get().getValue()));
    }

    public LazyValue<SecurityPrice> getLatestSecurityPrice()
    {
        return quote;
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

    public LazyValue<Money> getFees()
    {
        return new LazyValue<>(() -> costCalculation.get().fees());
    }

    public LazyValue<Money> getTaxes()
    {
        return new LazyValue<>(() -> costCalculation.get().taxes());
    }

    public LazyValue<Long> getSharesHeld()
    {
        return new LazyValue<>(() -> costCalculation.get().sharesHeld());
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
        return new LazyValue<>(() -> dividendCalculation.get().sum());
    }

    public LazyValue<Integer> getDividendEventCount()
    {
        return new LazyValue<>(() -> dividendCalculation.get().numOfEvents());
    }

    public LazyValue<LocalDate> getLastDividendPayment()
    {
        return new LazyValue<>(() -> dividendCalculation.get().lastDividendPayment());
    }

    public LazyValue<Periodicity> getPeriodicity()
    {
        return new LazyValue<>(() -> dividendCalculation.get().periodicity());
    }

    /**
     * Gets the rate of return of dividends per year as a percentage of
     * invested.
     * 
     * @return rate of return per year on success, else 0
     */
    public LazyValue<Double> getRateOfReturnPerYear()
    {
        return new LazyValue<>(() -> dividendCalculation.get().rateOfReturnPerYear());
    }

    public LazyValue<Double> getTotalRateOfReturnDiv()
    {
        return new LazyValue<>(() -> {
            var costs = costCalculation.get();
            return costs.sharesHeld() > 0
                            ? (double) dividendCalculation.get().sum().getAmount()
                                            / (double) costs.fifoCost().getAmount()
                            : 0;
        });
    }

    public LazyValue<Double> getTotalRateOfReturnDivMovingAverage()
    {
        return new LazyValue<>(() -> {
            var costs = costCalculation.get();
            return costs.sharesHeld() > 0
                            ? (double) dividendCalculation.get().sum().getAmount()
                                            / (double) costs.movingAverageCost().getAmount()
                            : 0;
        });
    }

    public LazyValue<CapitalGainsRecord> getRealizedCapitalGains()
    {
        return realizedCapitalGains;
    }

    public LazyValue<CapitalGainsRecord> getUnrealizedCapitalGains()
    {
        return unrealizedCapitalGains;
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
                return Trail.of(getSecurityName(), costCalculation.get().fifoCostTrail());
            case Trails.REALIZED_CAPITAL_GAINS:
                return Trail.of(getSecurityName(), getRealizedCapitalGains().get().getCapitalGainsTrail());
            case Trails.REALIZED_CAPITAL_GAINS_FOREX:
                return Trail.of(getSecurityName(), getRealizedCapitalGains().get().getForexCapitalGainsTrail());
            case Trails.UNREALIZED_CAPITAL_GAINS:
                return Trail.of(getSecurityName(), getUnrealizedCapitalGains().get().getCapitalGainsTrail());
            case Trails.UNREALIZED_CAPITAL_GAINS_FOREX:
                return Trail.of(getSecurityName(), getUnrealizedCapitalGains().get().getForexCapitalGainsTrail());
            default:
                return Optional.empty();
        }
    }

}
