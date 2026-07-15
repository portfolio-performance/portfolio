package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TextUtil;

public class TradeCategory
{
    private record TradeCategoryCostResult(Money totalEntryValue, Money totalProfitLoss,
                    Money totalProfitLossWithoutTaxesAndFees, double averageReturn, double winRate)
    {
    }

    private record TradeCategoryCommonResult(Money totalExitValue, double averageIRR, long averageHoldingPeriod)
    {
    }

    private interface CostMethodCalculation
    {
        TradeCategoryCostResult getResult();

        long getWinningTradesCount();

        long getLosingTradesCount();

        void invalidate();
    }

    public static final class ByDescription implements Comparator<TradeCategory>
    {
        @Override
        public int compare(TradeCategory c1, TradeCategory c2)
        {
            return TextUtil.compare(c1.getClassification().getName(), c2.getClassification().getName());
        }
    }

    public static final class TradeAssignment
    {
        private final Trade trade;
        private final double weight;

        private TradeAssignment(Trade trade, double weight)
        {
            this.trade = trade;
            this.weight = weight;
        }

        public Trade getTrade()
        {
            return trade;
        }

        public double getWeight()
        {
            return weight;
        }
    }

    private static final class WeightedTrade
    {
        private final Trade trade;
        private final double weight; // 0.0 to 1.0

        private WeightedTrade(Trade trade, double weight)
        {
            this.trade = trade;
            this.weight = weight;
        }
    }

    private static final class WeightedCashFlow
    {
        private final LocalDate date;
        private final double amount;
        private final int order;

        private WeightedCashFlow(LocalDate date, double amount, int order)
        {
            this.date = date;
            this.amount = amount;
            this.order = order;
        }
    }

    private static final class CollateralLot
    {
        private long shares;
        private double amount;

        private CollateralLot(long shares, double amount)
        {
            this.shares = shares;
            this.amount = amount;
        }
    }

    private static double releaseCollateral(Deque<CollateralLot> lots, long sharesToCover)
    {
        double released = 0;
        long remainingShares = sharesToCover;

        while (remainingShares > 0 && !lots.isEmpty())
        {
            CollateralLot lot = lots.peekFirst();
            if (lot == null)
                break;

            if (remainingShares >= lot.shares)
            {
                released += lot.amount;
                remainingShares -= lot.shares;
                lots.removeFirst();
            }
            else if (lot.shares > 0)
            {
                double fraction = (double) remainingShares / (double) lot.shares;
                double partialAmount = lot.amount * fraction;
                released += partialAmount;
                lot.amount -= partialAmount;
                lot.shares -= remainingShares;
                remainingShares = 0;
            }
            else
            {
                lots.removeFirst();
            }
        }

        return released;
    }

    private final Classification classification;
    private final Classification taxonomyClassification;
    private final CurrencyConverter converter;
    private final String currencyKey;
    private final List<WeightedTrade> weightedTrades = new ArrayList<>();

    private final FifoCalculation fifoCalculation = new FifoCalculation();
    private final MovingAverageCalculation movingAverageCalculation = new MovingAverageCalculation();
    private TradeCategoryCommonResult commonResult;

    /* package */ TradeCategory(Classification classification, CurrencyConverter converter)
    {
        this.classification = classification;
        this.taxonomyClassification = classification;
        this.converter = converter;
        this.currencyKey = converter.getTermCurrency();
    }

    /**
     * Alternative constructor for multi-currency mode.
     * <p>
     * Creates a new currency-specific classification and a new converter with
     * the given currency code.
     *
     * @param classification
     *            the original classification
     * @param converter
     *            the original currency converter
     * @param currencyCode
     *            the currency for this category
     */
    /* package */ TradeCategory(Classification classification, CurrencyConverter converter, String currencyCode)
    {
        this.converter = converter.with(currencyCode);
        this.currencyKey = this.converter.getTermCurrency();
        this.taxonomyClassification = classification;
        this.classification = new Classification(classification.getParent(), //
                        classification.getId(), //
                        classification.getName() + " (" + currencyCode + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        this.classification.setRank(classification.getRank());
    }

    public Classification getClassification()
    {
        return classification;
    }

    public Classification getTaxonomyClassification()
    {
        return taxonomyClassification;
    }

    public String getCurrencyKey()
    {
        return currencyKey;
    }

    /* package */ void addTrade(Trade trade, double weight)
    {
        this.weightedTrades.add(new WeightedTrade(trade, weight));
        this.fifoCalculation.invalidate();
        this.movingAverageCalculation.invalidate();
        this.commonResult = null;
    }

    public List<Trade> getTrades()
    {
        return weightedTrades.stream().map(wt -> wt.trade).distinct().collect(Collectors.toList());
    }

    public List<TradeAssignment> getTradeAssignments()
    {
        return weightedTrades.stream() //
                        .map(wt -> new TradeAssignment(wt.trade, wt.weight)) //
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public long getTradeCount()
    {
        return weightedTrades.stream().map(wt -> wt.trade).distinct().count();
    }

    /* package */ double getTotalWeight()
    {
        return weightedTrades.stream().mapToDouble(wt -> wt.weight).sum();
    }

    public Money getTotalEntryValue(CostMethod costMethod)
    {
        return getCalculation(costMethod).getResult().totalEntryValue();
    }

    public Money getTotalExitValue()
    {
        return getCommonResult().totalExitValue();
    }

    public Money getTotalProfitLoss(CostMethod costMethod, TaxesAndFees taxesAndFees)
    {
        CostMethodCalculation calculation = getCalculation(costMethod);

        Objects.requireNonNull(taxesAndFees);

        TradeCategoryCostResult result = calculation.getResult();
        return taxesAndFees == TaxesAndFees.INCLUDED ? result.totalProfitLoss()
                        : result.totalProfitLossWithoutTaxesAndFees();
    }

    public double getAverageReturn(CostMethod costMethod)
    {
        return getCalculation(costMethod).getResult().averageReturn();
    }

    public double getAverageIRR()
    {
        return getCommonResult().averageIRR();
    }

    public long getAverageHoldingPeriod()
    {
        return getCommonResult().averageHoldingPeriod();
    }

    public double getWinRate(CostMethod costMethod)
    {
        return getCalculation(costMethod).getResult().winRate();
    }

    public long getWinningTradesCount(CostMethod costMethod)
    {
        return getCalculation(costMethod).getWinningTradesCount();
    }

    public long getLosingTradesCount(CostMethod costMethod)
    {
        return getCalculation(costMethod).getLosingTradesCount();
    }

    /**
     * Calculates the category-level IRR by combining all cash flows from all
     * trades in this category. This is the mathematically correct approach, as
     * opposed to averaging individual trade IRRs.
     */
    private double calculateCategoryIRR()
    {
        List<WeightedCashFlow> cashflows = new ArrayList<>();
        int sequence = 0;

        for (WeightedTrade wt : weightedTrades)
        {
            Trade trade = wt.trade;
            double weight = wt.weight;
            boolean isLong = trade.isLong();

            Deque<CollateralLot> collateralLots = new ArrayDeque<>();
            double totalCollateral = 0;
            double remainingCollateral = 0;

            // Collect cash flows from all transactions in this trade
            for (TransactionPair<PortfolioTransaction> txPair : trade.getTransactions())
            {
                LocalDate date = txPair.getTransaction().getDateTime().toLocalDate();
                double amount = txPair.getTransaction().getMonetaryAmount()
                                .with(converter.at(txPair.getTransaction().getDateTime())).getAmount()
                                / Values.Amount.divider();

                // Apply weight to the cash flow
                amount *= weight;

                if (txPair.getTransaction().getType().isPurchase() == isLong)
                {
                    if (!isLong)
                    {
                        collateralLots.addLast(new CollateralLot(txPair.getTransaction().getShares(), amount));
                        totalCollateral += amount;
                        remainingCollateral += amount;
                    }
                    amount = -amount;
                }
                else if (!isLong)
                {
                    long sharesToCover = txPair.getTransaction().getShares();
                    double collateralReleased = releaseCollateral(collateralLots, sharesToCover);
                    remainingCollateral = Math.max(0, remainingCollateral - collateralReleased);

                    // For short trades, the closing purchase 'amount' is
                    // negative.
                    // The cash flow is the collateral returned minus the cost
                    // to close.
                    amount = collateralReleased - amount;
                }

                cashflows.add(new WeightedCashFlow(date, amount, sequence++));
            }

            // If trade is still open, add current market value as final cash
            // flow
            if (!trade.isClosed())
            {
                LocalDate date = LocalDate.now();
                double amount = trade.getExitValue().with(converter.at(date)).getAmount() / Values.Amount.divider();
                amount *= weight;
                if (!isLong)
                    amount = remainingCollateral - amount;
                cashflows.add(new WeightedCashFlow(date, amount, sequence++));
            }

            // For short trades, add final collateral return
            if (!isLong)
            {
                LocalDate endDate = trade.isClosed() ? trade.getEnd().get().toLocalDate() : LocalDate.now();
                cashflows.add(new WeightedCashFlow(endDate, totalCollateral, sequence++));
            }
        }

        // If we have no cash flows, return 0
        if (cashflows.isEmpty())
            return 0;

        cashflows.sort(Comparator.comparing((WeightedCashFlow cf) -> cf.date).thenComparingInt(cf -> cf.order));

        List<LocalDate> dates = new ArrayList<>(cashflows.size());
        List<Double> values = new ArrayList<>(cashflows.size());

        for (WeightedCashFlow cashflow : cashflows)
        {
            dates.add(cashflow.date);
            values.add(cashflow.amount);
        }

        // Calculate IRR from combined cash flows
        double irr = IRR.calculate(dates, values);

        // Filter out invalid results
        return Double.isFinite(irr) ? irr : 0;
    }

    private CostMethodCalculation getCalculation(CostMethod costMethod)
    {
        return switch (Objects.requireNonNull(costMethod))
        {
            case FIFO -> fifoCalculation;
            case MOVING_AVERAGE -> movingAverageCalculation;
        };
    }

    private final class FifoCalculation implements CostMethodCalculation
    {
        private TradeCategoryCostResult result;

        @Override
        public TradeCategoryCostResult getResult()
        {
            if (result == null)
                result = calculateResult();

            return result;
        }

        private TradeCategoryCostResult calculateResult()
        {
            double totalWeight = getTotalWeight();
            if (totalWeight == 0)
            {
                Money zero = Money.of(converter.getTermCurrency(), 0);
                return new TradeCategoryCostResult(zero, zero, zero, 0, 0);
            }

            Money totalEntryValue = weightedTrades.stream() //
                            .map(wt -> {
                                Money value = wt.trade.getEntryValue(CostMethod.FIFO, TaxesAndFees.INCLUDED);
                                if (value == null)
                                    return Money.of(converter.getTermCurrency(), 0);
                                LocalDate date = wt.trade.getStart().toLocalDate();
                                return value.with(converter.at(date)).multiplyAndRound(wt.weight);
                            }) //
                            .collect(MoneyCollectors.sum(converter.getTermCurrency()));

            Money totalProfitLoss = calculateTotalProfitLoss(TaxesAndFees.INCLUDED);
            Money totalProfitLossWithoutTaxesAndFees = calculateTotalProfitLoss(TaxesAndFees.NOT_INCLUDED);
            double averageReturn = totalEntryValue.getAmount() != 0
                            ? totalProfitLoss.getAmount() / (double) totalEntryValue.getAmount()
                            : 0;
            double winningWeight = weightedTrades.stream().filter(wt -> !wt.trade.isLoss(CostMethod.FIFO))
                            .mapToDouble(wt -> wt.weight).sum();

            return new TradeCategoryCostResult(totalEntryValue, totalProfitLoss, totalProfitLossWithoutTaxesAndFees,
                            averageReturn, winningWeight / totalWeight);
        }

        private Money calculateTotalProfitLoss(TaxesAndFees taxesAndFees)
        {
            return weightedTrades.stream() //
                            .map(wt -> {
                                Money pnl = wt.trade.getProfitLoss(CostMethod.FIFO, taxesAndFees);
                                LocalDate date = wt.trade.getEnd().map(LocalDate::from).orElse(LocalDate.now());
                                return pnl.with(converter.at(date)).multiplyAndRound(wt.weight);
                            }) //
                            .collect(MoneyCollectors.sum(converter.getTermCurrency()));
        }

        @Override
        public long getWinningTradesCount()
        {
            return weightedTrades.stream().filter(wt -> !wt.trade.isLoss(CostMethod.FIFO)).map(wt -> wt.trade)
                            .distinct().count();
        }

        @Override
        public long getLosingTradesCount()
        {
            return weightedTrades.stream().filter(wt -> wt.trade.isLoss(CostMethod.FIFO)).map(wt -> wt.trade)
                            .distinct().count();
        }

        @Override
        public void invalidate()
        {
            result = null;
        }
    }

    private final class MovingAverageCalculation implements CostMethodCalculation
    {
        private TradeCategoryCostResult result;

        @Override
        public TradeCategoryCostResult getResult()
        {
            if (result == null)
                result = calculateResult();

            return result;
        }

        private TradeCategoryCostResult calculateResult()
        {
            double totalWeight = getTotalWeight();
            if (totalWeight == 0)
            {
                Money zero = Money.of(converter.getTermCurrency(), 0);
                return new TradeCategoryCostResult(zero, zero, zero, 0, 0);
            }

            Money totalEntryValue = weightedTrades.stream() //
                        .map(wt -> {
                            Money value = wt.trade.getEntryValue(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED);
                            if (value == null)
                                return Money.of(converter.getTermCurrency(), 0);
                            LocalDate date = wt.trade.getStart().toLocalDate();
                            return value.with(converter.at(date)).multiplyAndRound(wt.weight);
                        }) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));

            Money totalProfitLoss = calculateTotalProfitLoss(TaxesAndFees.INCLUDED);
            Money totalProfitLossWithoutTaxesAndFees = calculateTotalProfitLoss(TaxesAndFees.NOT_INCLUDED);
            double averageReturn = totalEntryValue.getAmount() != 0
                        ? totalProfitLoss.getAmount() / (double) totalEntryValue.getAmount()
                        : 0;
            double winningWeight = weightedTrades.stream().filter(wt -> !wt.trade.isLoss(CostMethod.MOVING_AVERAGE))
                        .mapToDouble(wt -> wt.weight).sum();

            return new TradeCategoryCostResult(totalEntryValue, totalProfitLoss, totalProfitLossWithoutTaxesAndFees,
                        averageReturn, winningWeight / totalWeight);
        }

        private Money calculateTotalProfitLoss(TaxesAndFees taxesAndFees)
        {
            return weightedTrades.stream() //
                        .map(wt -> {
                            Money pnl = wt.trade.getProfitLoss(CostMethod.MOVING_AVERAGE, taxesAndFees);
                            LocalDate date = wt.trade.getEnd().map(LocalDate::from).orElse(LocalDate.now());
                            return pnl.with(converter.at(date)).multiplyAndRound(wt.weight);
                        }) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
        }

        @Override
        public long getWinningTradesCount()
        {
            return weightedTrades.stream().filter(wt -> !wt.trade.isLoss(CostMethod.MOVING_AVERAGE))
                            .map(wt -> wt.trade).distinct().count();
        }

        @Override
        public long getLosingTradesCount()
        {
            return weightedTrades.stream().filter(wt -> wt.trade.isLoss(CostMethod.MOVING_AVERAGE))
                            .map(wt -> wt.trade).distinct().count();
        }

        @Override
        public void invalidate()
        {
            result = null;
        }
    }

    private TradeCategoryCommonResult getCommonResult()
    {
        if (commonResult == null)
            commonResult = calculateCommonResult();
        return commonResult;
    }

    private TradeCategoryCommonResult calculateCommonResult()
    {
        double totalWeight = getTotalWeight();
        if (totalWeight == 0)
            return new TradeCategoryCommonResult(Money.of(converter.getTermCurrency(), 0), 0, 0);

        Money totalExitValue = weightedTrades.stream() //
                        .map(wt -> {
                            Money value = wt.trade.getExitValue();
                            if (value == null)
                                return Money.of(converter.getTermCurrency(), 0);
                            LocalDate date = wt.trade.getEnd().map(LocalDate::from).orElse(LocalDate.now());
                            return value.with(converter.at(date)).multiplyAndRound(wt.weight);
                        }) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));

        long averageHoldingPeriod = Math.round(
                        weightedTrades.stream().mapToDouble(wt -> wt.trade.getHoldingPeriod() * wt.weight).sum()
                                        / totalWeight);

        return new TradeCategoryCommonResult(totalExitValue, calculateCategoryIRR(), averageHoldingPeriod);
    }

}
