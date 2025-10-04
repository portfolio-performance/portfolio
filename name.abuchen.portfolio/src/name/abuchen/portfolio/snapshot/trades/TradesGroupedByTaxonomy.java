package name.abuchen.portfolio.snapshot.trades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

public class TradesGroupedByTaxonomy
{
    private final Taxonomy taxonomy;
    private final List<Trade> allTrades;
    private final CurrencyConverter converter;
    private final List<TradeCategory> categories = new ArrayList<>();

    public TradesGroupedByTaxonomy(Taxonomy taxonomy, List<Trade> trades, CurrencyConverter converter)
    {
        this.taxonomy = taxonomy;
        this.allTrades = trades;
        this.converter = converter;

        doGrouping();
    }

    private void doGrouping()
    {
        if (taxonomy == null)
            return;

        // track how much weight has been assigned to each trade
        Map<Trade, Integer> tradeAssignedWeights = new HashMap<>();
        for (Trade trade : allTrades)
            tradeAssignedWeights.put(trade, 0);

        // create category for each classification and assign trades
        Map<Classification, TradeCategory> classificationToCategory = new HashMap<>();

        taxonomy.getRoot().accept(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification)
            {
                if (classification.getParent() != null) // skip root
                {
                    TradeCategory category = new TradeCategory(classification, converter);
                    classificationToCategory.put(classification, category);
                }
            }

            @Override
            public void visit(Classification classification, Assignment assignment)
            {
                InvestmentVehicle vehicle = assignment.getInvestmentVehicle();
                if (!(vehicle instanceof Security))
                    return;

                Security security = (Security) vehicle;
                TradeCategory category = classificationToCategory.get(classification);
                if (category == null)
                    return;

                // find all trades for this security and add them to the category
                for (Trade trade : allTrades)
                {
                    if (trade.getSecurity().equals(security))
                    {
                        double weight = assignment.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT;
                        category.addTrade(trade, weight);

                        // track total assigned weight
                        tradeAssignedWeights.merge(trade, assignment.getWeight(), Integer::sum);
                    }
                }
            }
        });

        // collect all categories
        for (Classification classification : taxonomy.getRoot().getChildren())
        {
            TradeCategory category = classificationToCategory.get(classification);
            if (category != null && category.getTradeCount() > 0)
                categories.add(category);
        }

        // sort by classification rank
        Collections.sort(categories,
                        Comparator.comparingInt(c -> c.getClassification().getRank()));

        // handle unassigned trades
        createUnassignedCategory(tradeAssignedWeights);
    }

    private void createUnassignedCategory(Map<Trade, Integer> tradeAssignedWeights)
    {
        Classification unassignedClassification = new Classification(null, Classification.UNASSIGNED_ID,
                        Messages.LabelWithoutClassification);
        TradeCategory unassignedCategory = new TradeCategory(unassignedClassification, converter);

        for (Map.Entry<Trade, Integer> entry : tradeAssignedWeights.entrySet())
        {
            Trade trade = entry.getKey();
            int assignedWeight = entry.getValue();

            if (assignedWeight < Classification.ONE_HUNDRED_PERCENT)
            {
                double unassignedWeight = (Classification.ONE_HUNDRED_PERCENT - assignedWeight)
                                / (double) Classification.ONE_HUNDRED_PERCENT;
                unassignedCategory.addTrade(trade, unassignedWeight);
            }
        }

        if (unassignedCategory.getTradeCount() > 0)
            categories.add(unassignedCategory);
    }

    public Taxonomy getTaxonomy()
    {
        return taxonomy;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public List<TradeCategory> asList()
    {
        return Collections.unmodifiableList(categories);
    }

    public Money getTotalProfitLoss()
    {
        return categories.stream().map(TradeCategory::getTotalProfitLoss)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public Money getTotalProfitLossWithoutTaxesAndFees()
    {
        return categories.stream().map(TradeCategory::getTotalProfitLossWithoutTaxesAndFees)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public long getTotalTradeCount()
    {
        return categories.stream().mapToLong(TradeCategory::getTradeCount).sum();
    }

    /* package */ TradeCategory byClassification(Classification classification)
    {
        for (TradeCategory category : categories)
        {
            if (category.getClassification().equals(classification))
                return category;
        }

        return null;
    }
}
