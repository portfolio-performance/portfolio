package name.abuchen.portfolio.snapshot.security;

import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import name.abuchen.portfolio.math.Risk;
import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.security.CostCalculation.CostCalculationResult;
import name.abuchen.portfolio.snapshot.security.FeesAndTaxesCalculation.FeesAndTaxesResult;
import name.abuchen.portfolio.snapshot.security.DividendCalculation.DividendCalculationResult;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.LazyValue;

public final class LazySecurityPerformanceRecord extends BaseSecurityPerformanceRecord
                implements Adaptable
{
    /**
     * The lazy weak value computes the value only when needed but also keeps
     * only a weak reference to it.
     */
    private static final class LazyWeakValue<V>
    {
        private WeakReference<V> reference = new WeakReference<>(null);
        private final Supplier<V> computeFunction;

        public LazyWeakValue(Supplier<V> computeFunction)
        {
            this.computeFunction = computeFunction;
        }

        public V get()
        {
            var answer = reference.get();
            if (answer != null)
                return answer;

            answer = computeFunction.get();
            reference = new WeakReference<>(answer);

            return answer;
        }
    }

    /**
     * internal rate of return of security {@link #calculateIRR()}
     */
    private final LazyValue<Double> irr = new LazyValue<>(
                    () -> Calculation.perform(IRRCalculation.class, converter, security, lineItems).getIRR());

    /**
     * weak reference to the performance index, because it can consume a lot of
     * memory in particular for large intervals
     */
    private final LazyWeakValue<PerformanceIndex> performanceIndex = new LazyWeakValue<>(
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

    /**
     * Quote in term currency, i.e., the reporting currency used to calculate
     * the record.
     */
    private final LazyValue<Quote> quoteInTermCurrency = new LazyValue<>(() -> {
        var price = quote.get();
        return converter.convert(price.getDate(), Quote.of(security.getCurrencyCode(), price.getValue()));
    });

    private final LazyValue<FeesAndTaxesResult> feesAndTaxes = new LazyValue<>(
                    () -> Calculation.perform(FeesAndTaxesCalculation.class, converter, security, lineItems)
                                    .getResult());

    private final LazyValue<DividendCalculationResult> dividendCalculation = new LazyValue<>(
                    () -> Calculation.perform(DividendCalculation.class, converter, security, lineItems).getResult());

    private final LazyValue<CostCalculationResult> fifoCosts = new LazyValue<>(
                    () -> calculateCosts(CostMethod.FIFO));
    private final LazyValue<CostCalculationResult> movingAverageCosts = new LazyValue<>(
                    () -> calculateCosts(CostMethod.MOVING_AVERAGE));
    private final LazyValue<Double> fifoDividendRateOfReturn = new LazyValue<>(
                    () -> calculateDividendRateOfReturn(CostMethod.FIFO));
    private final LazyValue<Double> movingAverageDividendRateOfReturn = new LazyValue<>(
                    () -> calculateDividendRateOfReturn(CostMethod.MOVING_AVERAGE));
    private final LazyValue<CapitalGainsCalculationFifo> fifoCapitalGains = new LazyValue<>(
                    () -> Calculation.perform(CapitalGainsCalculationFifo.class, converter, security, lineItems));
    private final LazyValue<CapitalGainsCalculationMovingAverage> movingAverageCapitalGains = new LazyValue<>(
                    () -> Calculation.perform(CapitalGainsCalculationMovingAverage.class, converter, security,
                                    lineItems));

    /* package */ LazySecurityPerformanceRecord(Client client, Security security, CurrencyConverter converter,
                    Interval interval)
    {
        super(client, security, converter, interval);
    }

    private CostCalculationResult calculateCosts(CostMethod costMethod)
    {
        return Calculation.perform(() -> new CostCalculation(costMethod), converter, security, lineItems).getResult();
    }

    private CostCalculationResult getCosts(CostMethod costMethod)
    {
        return switch (Objects.requireNonNull(costMethod))
        {
            case FIFO -> fifoCosts.get();
            case MOVING_AVERAGE -> movingAverageCosts.get();
        };
    }

    private Double calculateDividendRateOfReturn(CostMethod costMethod)
    {
        CostCalculationResult result = getCosts(costMethod);

        return Calculation.perform(() -> new DividendRateOfReturnCalculation(result::getDividendCost), converter,
                        security, lineItems).getRateOfReturnPerYear();
    }

    private Double getDividendRateOfReturn(CostMethod costMethod)
    {
        return switch (Objects.requireNonNull(costMethod))
        {
            case FIFO -> fifoDividendRateOfReturn.get();
            case MOVING_AVERAGE -> movingAverageDividendRateOfReturn.get();
        };
    }

    public Double getIrr()
    {
        return irr.get();
    }

    public Double getTrueTimeWeightedRateOfReturn()
    {
        return twror.get();
    }

    public Double getTrueTimeWeightedRateOfReturnAnnualized()
    {
        return twrorpa.get();
    }

    public Drawdown getDrawdown()
    {
        return drawdown.get();
    }

    public Volatility getVolatility()
    {
        return volatility.get();
    }

    public Money getDelta()
    {
        return delta.get();
    }

    public Double getDeltaPercent()
    {
        return deltaPercent.get();
    }

    public Money getMarketValue()
    {
        return marketValue.get();
    }

    public Quote getQuote()
    {
        return Quote.of(security.getCurrencyCode(), quote.get().getValue());
    }

    public Quote getQuoteInTermCurrency()
    {
        return quoteInTermCurrency.get();
    }

    public SecurityPrice getLatestSecurityPrice()
    {
        return quote.get();
    }

    /**
     * cost of shares held
     */
    public Money getCost(CostMethod costMethod, TaxesAndFees taxesAndFees)
    {
        CostCalculationResult result = getCosts(costMethod);
        return taxesAndFees.isIncluded() ? result.cost() : result.netCost();
    }

    public Money getCapitalGainsOnHoldings(CostMethod costMethod)
    {
        return marketValue.get().subtract(getCost(costMethod, TaxesAndFees.INCLUDED));
    }

    public Double getCapitalGainsOnHoldingsPercent(CostMethod costMethod)
    {
        Money value = marketValue.get();
        Money cost = getCost(costMethod, TaxesAndFees.INCLUDED);

        if (value.getAmount() == 0L && cost.getAmount() == 0L)
            return 0d;

        return value.getAmount() / (double) cost.getAmount() - 1;
    }

    public Money getFees()
    {
        return feesAndTaxes.get().fees();
    }

    public Money getTaxes()
    {
        return feesAndTaxes.get().taxes();
    }

    public Long getSharesHeld(CostMethod costMethod)
    {
        return getCosts(costMethod).sharesHeld();
    }

    public Quote getCostPerSharesHeld(CostMethod costMethod, TaxesAndFees taxesAndFees)
    {
        long shares = getCosts(costMethod).sharesHeld();
        Money cost = getCost(costMethod, taxesAndFees);

        return Quote.of(cost.getCurrencyCode(), Math.round(cost.getAmount() / (double) shares
                        * Values.Share.factor() * Values.Quote.factorToMoney()));
    }

    public Money getSumOfDividends()
    {
        return dividendCalculation.get().sum();
    }

    public Integer getDividendEventCount()
    {
        return dividendCalculation.get().numOfEvents();
    }

    public LocalDate getLastDividendPayment()
    {
        return dividendCalculation.get().lastDividendPayment();
    }

    public Periodicity getPeriodicity()
    {
        return dividendCalculation.get().periodicity();
    }

    /**
     * Gets the rate of return of dividends per year as a percentage of
     * invested.
     *
     * @return rate of return per year on success, else 0
     */
    public Double getRateOfReturnPerYear(CostMethod costMethod)
    {
        return getDividendRateOfReturn(costMethod);
    }

    public double getPersonalDividendYield(CalculationLineItem.DividendPayment payment, CostMethod costMethod)
    {
        CostCalculation.DividendCostContext context = getCosts(costMethod).getDividendCost(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Missing dividend cost context")); //$NON-NLS-1$

        return payment.getPersonalDividendYield(context.cost(), context.totalShares());
    }

    public Double getTotalRateOfReturnDiv(CostMethod costMethod)
    {
        CostCalculationResult result = getCosts(costMethod);
        Money cost = getCost(costMethod, TaxesAndFees.INCLUDED);

        return result.sharesHeld() > 0 ? dividendCalculation.get().sum().getAmount() / (double) cost.getAmount()
                        : 0;
    }

    public CapitalGainsRecord getRealizedCapitalGains(CostMethod costMethod)
    {
        return switch (Objects.requireNonNull(costMethod))
        {
            case FIFO -> fifoCapitalGains.get().getRealizedCapitalGains();
            case MOVING_AVERAGE -> movingAverageCapitalGains.get().getRealizedCapitalGains();
        };
    }

    public CapitalGainsRecord getUnrealizedCapitalGains(CostMethod costMethod)
    {
        return switch (Objects.requireNonNull(costMethod))
        {
            case FIFO -> fifoCapitalGains.get().getUnrealizedCapitalGains();
            case MOVING_AVERAGE -> movingAverageCapitalGains.get().getUnrealizedCapitalGains();
        };
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

    public Optional<Trail> explain(String key, CostMethod costMethod)
    {
        return switch (key)
        {
            case Trails.COST -> getCosts(costMethod).trail()
                            .flatMap(trail -> Trail.of(getSecurityName(), trail));
            case Trails.REALIZED_CAPITAL_GAINS -> Trail.of(getSecurityName(),
                            getRealizedCapitalGains(costMethod).getCapitalGainsTrail());
            case Trails.REALIZED_CAPITAL_GAINS_FOREX -> Trail.of(getSecurityName(),
                            getRealizedCapitalGains(costMethod).getForexCapitalGainsTrail());
            case Trails.UNREALIZED_CAPITAL_GAINS -> Trail.of(getSecurityName(),
                            getUnrealizedCapitalGains(costMethod).getCapitalGainsTrail());
            case Trails.UNREALIZED_CAPITAL_GAINS_FOREX -> Trail.of(getSecurityName(),
                            getUnrealizedCapitalGains(costMethod).getForexCapitalGainsTrail());
            default -> Optional.empty();
        };
    }

}
