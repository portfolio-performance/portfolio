package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public class TradeTotals
{
    private record TradeTotalsCostResult(Money totalEntryValue, Money totalProfitLoss,
                    Money totalProfitLossWithoutTaxesAndFees, double averageReturn)
    {
    }

    private final CurrencyConverter converter;
    private final List<Trade> trades;
    private final TradeCategory aggregate;

    private final Money totalExitValue;
    private final long totalShares;
    private TradeTotalsCostResult fifoResult;
    private TradeTotalsCostResult movingAverageResult;

    public TradeTotals(TradesGroupedByTaxonomy groupedTrades)
    {
        this.converter = groupedTrades.getCurrencyConverter();
        this.trades = groupedTrades.getTrades();

        Classification classification = new Classification(null, TradeTotals.class.getName(), "Totals"); //$NON-NLS-1$
        this.aggregate = new TradeCategory(classification, converter);
        this.trades.stream().distinct().forEach(trade -> aggregate.addTrade(trade, 1.0));

        this.totalShares = trades.stream().mapToLong(Trade::getShares).sum();
        this.totalExitValue = sumMoney(Trade::getExitValue, trade -> trade.getEnd().orElse(LocalDateTime.now()));
    }

    private TradeTotalsCostResult getCostResult(CostMethod costMethod)
    {
        return switch (Objects.requireNonNull(costMethod))
        {
            case FIFO -> getOrCalculateFifoResult();
            case MOVING_AVERAGE -> getOrCalculateMovingAverageResult();
        };
    }

    private TradeTotalsCostResult getOrCalculateFifoResult()
    {
        if (fifoResult == null)
            fifoResult = calculateFifoResult();

        return fifoResult;
    }

    private TradeTotalsCostResult getOrCalculateMovingAverageResult()
    {
        if (movingAverageResult == null)
            movingAverageResult = calculateMovingAverageResult();

        return movingAverageResult;
    }

    private TradeTotalsCostResult calculateFifoResult()
    {
        Money totalEntryValue = sumMoney(
                        trade -> trade.getEntryValue(CostMethod.FIFO, TaxesAndFees.INCLUDED), Trade::getStart);
        Money totalProfitLoss = aggregate.getTotalProfitLoss(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money totalProfitLossWithoutTaxesAndFees = aggregate.getTotalProfitLoss(CostMethod.FIFO,
                        TaxesAndFees.NOT_INCLUDED);
        return new TradeTotalsCostResult(totalEntryValue, totalProfitLoss, totalProfitLossWithoutTaxesAndFees,
                        aggregate.getAverageReturn(CostMethod.FIFO));
    }

    private TradeTotalsCostResult calculateMovingAverageResult()
    {
        Money totalEntryValue = sumMoney(
                        trade -> trade.getEntryValue(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), Trade::getStart);
        Money totalProfitLoss = sumMoney(
                        trade -> trade.getProfitLoss(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        trade -> trade.getEnd().orElse(LocalDateTime.now()));
        Money totalProfitLossWithoutTaxesAndFees = sumMoney(
                        trade -> trade.getProfitLoss(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        trade -> trade.getEnd().orElse(LocalDateTime.now()));
        double averageReturn = totalEntryValue.isZero() ? 0
                        : totalProfitLoss.getAmount() / (double) totalEntryValue.getAmount();
        return new TradeTotalsCostResult(totalEntryValue, totalProfitLoss, totalProfitLossWithoutTaxesAndFees,
                        averageReturn);
    }

    private Money sumMoney(Function<Trade, Money> extractor, Function<Trade, LocalDateTime> dateExtractor)
    {
        return trades.stream().map(trade -> {
            Money value = extractor.apply(trade);
            if (value == null)
                return Money.of(converter.getTermCurrency(), 0);

            LocalDateTime date = Objects.requireNonNullElseGet(dateExtractor.apply(trade), LocalDateTime::now);
            return value.with(converter.at(date));
        }).collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public String getCurrencyCode()
    {
        return converter.getTermCurrency();
    }

    public long getTradeCount()
    {
        return aggregate.getTradeCount();
    }

    public long getTotalShares()
    {
        return totalShares;
    }

    public Money getTotalEntryValue(CostMethod costMethod)
    {
        return getCostResult(Objects.requireNonNull(costMethod)).totalEntryValue();
    }

    public Money getTotalExitValue()
    {
        return totalExitValue;
    }

    public Money getTotalProfitLoss(CostMethod costMethod, TaxesAndFees taxesAndFees)
    {
        Objects.requireNonNull(costMethod);
        Objects.requireNonNull(taxesAndFees);
        TradeTotalsCostResult result = getCostResult(costMethod);
        return taxesAndFees == TaxesAndFees.INCLUDED ? result.totalProfitLoss()
                        : result.totalProfitLossWithoutTaxesAndFees();
    }

    public long getAverageHoldingPeriod()
    {
        return aggregate.getAverageHoldingPeriod();
    }

    public double getAverageIRR()
    {
        return aggregate.getAverageIRR();
    }

    public double getAverageReturn(CostMethod costMethod)
    {
        return getCostResult(Objects.requireNonNull(costMethod)).averageReturn();
    }

    public Money getAverageEntryPrice(CostMethod costMethod)
    {
        Objects.requireNonNull(costMethod);
        if (totalShares == 0)
            return null;
        Money totalEntryValue = getCostResult(costMethod).totalEntryValue();
        long amount = Math.round(totalEntryValue.getAmount() / (double) totalShares * Values.Share.factor());
        return Money.of(totalEntryValue.getCurrencyCode(), amount);
    }

    public Money getAverageExitPrice()
    {
        if (totalShares == 0)
            return null;
        long amount = Math.round(totalExitValue.getAmount() / (double) totalShares * Values.Share.factor());
        return Money.of(totalExitValue.getCurrencyCode(), amount);
    }
}
