package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TextUtil;

public class TradeCategory
{
    public static final class ByDescription implements Comparator<TradeCategory>
    {
        @Override
        public int compare(TradeCategory c1, TradeCategory c2)
        {
            return TextUtil.compare(c1.getClassification().getName(), c2.getClassification().getName());
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

    private final Classification classification;
    private final CurrencyConverter converter;
    private final List<WeightedTrade> weightedTrades = new ArrayList<>();

    // lazy calculations
    private Money totalProfitLoss;
    private Money totalProfitLossWithoutTaxesAndFees;
    private double averageReturn;
    private double averageIRR;
    private long averageHoldingPeriod;
    private double winRate;
    private boolean calculated = false;

    /* package */ TradeCategory(Classification classification, CurrencyConverter converter)
    {
        this.classification = classification;
        this.converter = converter;
    }

    public Classification getClassification()
    {
        return classification;
    }

    /* package */ void addTrade(Trade trade, double weight)
    {
        this.weightedTrades.add(new WeightedTrade(trade, weight));
        this.calculated = false;
    }

    public List<Trade> getTrades()
    {
        return weightedTrades.stream().map(wt -> wt.trade).distinct().collect(Collectors.toList());
    }

    public long getTradeCount()
    {
        ensureCalculated();
        return Math.round(weightedTrades.stream().mapToDouble(wt -> wt.weight).sum());
    }

    public Money getTotalProfitLoss()
    {
        ensureCalculated();
        return totalProfitLoss;
    }

    public Money getTotalProfitLossWithoutTaxesAndFees()
    {
        ensureCalculated();
        return totalProfitLossWithoutTaxesAndFees;
    }

    public double getAverageReturn()
    {
        ensureCalculated();
        return averageReturn;
    }

    public double getAverageIRR()
    {
        ensureCalculated();
        return averageIRR;
    }

    public long getAverageHoldingPeriod()
    {
        ensureCalculated();
        return averageHoldingPeriod;
    }

    public double getWinRate()
    {
        ensureCalculated();
        return winRate;
    }

    public long getWinningTradesCount()
    {
        return Math.round(weightedTrades.stream().filter(wt -> !wt.trade.isLoss()).mapToDouble(wt -> wt.weight).sum());
    }

    public long getLosingTradesCount()
    {
        return Math.round(weightedTrades.stream().filter(wt -> wt.trade.isLoss()).mapToDouble(wt -> wt.weight).sum());
    }

    /**
     * Calculates the category-level IRR by combining all cash flows from all
     * trades in this category. This is the mathematically correct approach,
     * as opposed to averaging individual trade IRRs.
     */
    private double calculateCategoryIRR()
    {
        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (WeightedTrade wt : weightedTrades)
        {
            Trade trade = wt.trade;
            double weight = wt.weight;
            boolean isLong = trade.isLong();

            // Collect cash flows from all transactions in this trade
            double[] collateral = {0};
            trade.getTransactions().forEach(txPair -> {
                dates.add(txPair.getTransaction().getDateTime().toLocalDate());

                double amount = txPair.getTransaction().getMonetaryAmount()
                                .with(converter.at(txPair.getTransaction().getDateTime())).getAmount()
                                / Values.Amount.divider();

                // Apply weight to the cash flow
                amount *= weight;

                if (txPair.getTransaction().getType().isPurchase() == isLong)
                {
                    collateral[0] += amount;
                    amount = -amount;
                }
                else if (!isLong)
                {
                    // for short trade, for the closing transaction, we look
                    // how much collateral we should return
                    amount = collateral[0] - amount;
                }

                values.add(amount);
            });

            // If trade is still open, add current market value as final cash flow
            if (!trade.isClosed())
            {
                dates.add(LocalDate.now());
                double amount = trade.getExitValue().getAmount() / Values.Amount.divider();
                amount *= weight;
                if (!isLong)
                    amount = collateral[0] - amount;
                values.add(amount);
            }

            // For short trades, add final collateral return
            if (!isLong)
            {
                LocalDate endDate = trade.isClosed() ? trade.getEnd().get().toLocalDate() : LocalDate.now();
                dates.add(endDate);
                values.add(collateral[0]);
            }
        }

        // If we have no cash flows, return 0
        if (dates.isEmpty() || values.isEmpty())
            return 0;

        // Calculate IRR from combined cash flows
        double irr = IRR.calculate(dates, values);

        // Filter out invalid results
        return Double.isFinite(irr) ? irr : 0;
    }

    private void ensureCalculated()
    {
        if (calculated)
            return;

        double totalWeight = weightedTrades.stream().mapToDouble(wt -> wt.weight).sum();

        if (totalWeight == 0)
        {
            this.totalProfitLoss = Money.of(converter.getTermCurrency(), 0);
            this.totalProfitLossWithoutTaxesAndFees = Money.of(converter.getTermCurrency(), 0);
            this.averageReturn = 0;
            this.averageIRR = 0;
            this.averageHoldingPeriod = 0;
            this.winRate = 0;
        }
        else
        {
            this.totalProfitLoss = weightedTrades.stream()
                            .map(wt -> wt.trade.getProfitLoss().multiplyAndRound(wt.weight))
                            .collect(MoneyCollectors.sum(converter.getTermCurrency()));

            this.totalProfitLossWithoutTaxesAndFees = weightedTrades.stream()
                            .map(wt -> wt.trade.getProfitLossWithoutTaxesAndFees().multiplyAndRound(wt.weight))
                            .collect(MoneyCollectors.sum(converter.getTermCurrency()));

            // Calculate category-level IRR by combining all cash flows
            this.averageIRR = calculateCategoryIRR();
            
            // Calculate category-level return from aggregate P&L and entry value
            Money totalEntryValue = weightedTrades.stream()
                            .map(wt -> wt.trade.getEntryValue().multiplyAndRound(wt.weight))
                            .collect(MoneyCollectors.sum(converter.getTermCurrency()));
            Money totalExitValue = weightedTrades.stream()
                            .map(wt -> wt.trade.getExitValue().multiplyAndRound(wt.weight))
                            .collect(MoneyCollectors.sum(converter.getTermCurrency()));
            
            if (totalEntryValue.getAmount() > 0)
            {
                // For long trades: (exit / entry) - 1
                // This assumes all trades in category are long (standard case)
                this.averageReturn = (totalExitValue.getAmount() / (double) totalEntryValue.getAmount()) - 1;
            }
            else
            {
                this.averageReturn = 0;
            }

            this.averageHoldingPeriod = Math.round(
                            weightedTrades.stream().mapToDouble(wt -> wt.trade.getHoldingPeriod() * wt.weight).sum()
                                            / totalWeight);

            double winningWeight = weightedTrades.stream().filter(wt -> !wt.trade.isLoss())
                            .mapToDouble(wt -> wt.weight).sum();
            this.winRate = winningWeight / totalWeight;
        }

        this.calculated = true;
    }

    public List<TradeCategory> sortByProfitLoss()
    {
        List<TradeCategory> answer = new ArrayList<>();
        answer.add(this);
        Collections.sort(answer, (c1, c2) -> c2.getTotalProfitLoss().compareTo(c1.getTotalProfitLoss()));
        return answer;
    }
}
