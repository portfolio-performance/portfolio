package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public class TradeTotals
{
    private final CurrencyConverter converter;
    private final List<Trade> trades;
    private final TradeCategory aggregate;

    private final Money totalEntryValue;
    private final Money totalEntryValueMovingAverage;
    private final Money totalExitValue;
    private final Money totalProfitLossMovingAverage;
    private final Money totalProfitLossMovingAverageWithoutTaxesAndFees;
    private final long totalShares;

    public TradeTotals(TradesGroupedByTaxonomy groupedTrades)
    {
        this.converter = groupedTrades.getCurrencyConverter();
        this.trades = groupedTrades.getTrades();

        Classification classification = new Classification(null, TradeTotals.class.getName(), "Totals"); //$NON-NLS-1$
        this.aggregate = new TradeCategory(classification, converter);
        this.trades.stream().distinct().forEach(trade -> aggregate.addTrade(trade, 1.0));

        this.totalShares = trades.stream().mapToLong(Trade::getShares).sum();
        this.totalEntryValue = sumMoney(Trade::getEntryValue, Trade::getStart);
        this.totalEntryValueMovingAverage = sumMoney(Trade::getEntryValueMovingAverage, Trade::getStart);
        this.totalExitValue = sumMoney(Trade::getExitValue, trade -> trade.getEnd().orElse(LocalDateTime.now()));
        this.totalProfitLossMovingAverage = sumMoney(Trade::getProfitLossMovingAverage,
                        trade -> trade.getEnd().orElse(LocalDateTime.now()));
        this.totalProfitLossMovingAverageWithoutTaxesAndFees = sumMoney(
                        Trade::getProfitLossMovingAverageWithoutTaxesAndFees,
                        trade -> trade.getEnd().orElse(LocalDateTime.now()));
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

    public Money getTotalEntryValue()
    {
        return totalEntryValue;
    }

    public Money getTotalEntryValueMovingAverage()
    {
        return totalEntryValueMovingAverage;
    }

    public Money getTotalExitValue()
    {
        return totalExitValue;
    }

    public Money getTotalProfitLoss()
    {
        return aggregate.getTotalProfitLoss();
    }

    public Money getTotalProfitLossWithoutTaxesAndFees()
    {
        return aggregate.getTotalProfitLossWithoutTaxesAndFees();
    }

    public Money getTotalProfitLossMovingAverage()
    {
        return totalProfitLossMovingAverage;
    }

    public Money getTotalProfitLossMovingAverageWithoutTaxesAndFees()
    {
        return totalProfitLossMovingAverageWithoutTaxesAndFees;
    }

    public long getAverageHoldingPeriod()
    {
        return aggregate.getAverageHoldingPeriod();
    }

    public double getAverageIRR()
    {
        return aggregate.getAverageIRR();
    }

    public double getAverageReturn()
    {
        return aggregate.getAverageReturn();
    }

    public double getAverageReturnMovingAverage()
    {
        if (totalEntryValueMovingAverage.isZero())
            return 0;
        return totalProfitLossMovingAverage.getAmount() / (double) totalEntryValueMovingAverage.getAmount();
    }

    public Money getAverageEntryPrice()
    {
        if (totalShares == 0)
            return null;
        long amount = Math.round(totalEntryValue.getAmount() / (double) totalShares * Values.Share.factor());
        return Money.of(totalEntryValue.getCurrencyCode(), amount);
    }

    public Money getAverageEntryPriceMovingAverage()
    {
        if (totalShares == 0)
            return null;
        long amount = Math
                        .round(totalEntryValueMovingAverage.getAmount() / (double) totalShares * Values.Share.factor());
        return Money.of(totalEntryValueMovingAverage.getCurrencyCode(), amount);
    }

    public Money getAverageExitPrice()
    {
        if (totalShares == 0)
            return null;
        long amount = Math.round(totalExitValue.getAmount() / (double) totalShares * Values.Share.factor());
        return Money.of(totalExitValue.getCurrencyCode(), amount);
    }
}
