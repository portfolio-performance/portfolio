package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

        Set<String> distinctCurrencies = allTrades.stream()
                        .map(t -> t.getProfitLoss() != null ? t.getProfitLoss().getCurrencyCode() : null)
                        .filter(Objects::nonNull).collect(Collectors.toSet());

        final boolean multiCurrencyMode = distinctCurrencies.size() > 1
                        || distinctCurrencies.stream().anyMatch(code -> !code.equals(converter.getTermCurrency()));

        // track how much weight has been assigned to each trade
        Map<Trade, Integer> tradeAssignedWeights = new HashMap<>();
        for (Trade trade : allTrades)
            tradeAssignedWeights.put(trade, 0);

        // create category for each classification and assign trades
        Map<Object, TradeCategory> keyToCategory = new HashMap<>();

        taxonomy.getRoot().accept(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification)
            {
                // Categories are created on-the-fly
            }

            @Override
            public void visit(Classification classification, Assignment assignment)
            {
                if (classification.getParent() == null) // skip root
                    return;

                InvestmentVehicle vehicle = assignment.getInvestmentVehicle();
                if (!(vehicle instanceof Security))
                    return;

                Security security = (Security) vehicle;

                // find all trades for this security and add them to the
                // category
                for (Trade trade : allTrades)
                {
                    if (trade.getSecurity().equals(security))
                    {
                        if (trade.getProfitLoss() == null || trade.getProfitLoss().getCurrencyCode() == null)
                            continue;

                        String currencyCode = trade.getProfitLoss().getCurrencyCode();

                        Object key = multiCurrencyMode
                                        ? new AbstractMap.SimpleImmutableEntry<>(classification, currencyCode)
                                        : classification;

                        TradeCategory category = keyToCategory.computeIfAbsent(key,
                                        k -> multiCurrencyMode
                                                        ? new TradeCategory(classification, converter, currencyCode)
                                                        : new TradeCategory(classification, converter));

                        double weight = assignment.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT;
                        category.addTrade(trade, weight);

                        tradeAssignedWeights.merge(trade, assignment.getWeight(), Integer::sum);
                    }
                }
            }
        });

        categories.addAll(keyToCategory.values().stream().filter(c -> c.getTotalWeight() > 0)
                        .collect(Collectors.toList()));

        // sort by classification rank, id, and currency to keep multi-currency
        // clones deterministic
        Collections.sort(categories,
                        Comparator.comparingInt((TradeCategory c) -> c.getClassification().getRank())
                                        .thenComparing(c -> c.getClassification().getId())
                                        .thenComparing(TradeCategory::getCurrencyKey));

        // handle unassigned trades
        createUnassignedCategory(tradeAssignedWeights, multiCurrencyMode);
    }

    private void createUnassignedCategory(Map<Trade, Integer> tradeAssignedWeights, boolean multiCurrencyMode)
    {
        Classification unassignedClassification = new Classification(null, Classification.UNASSIGNED_ID,
                        Messages.LabelWithoutClassification);

        Map<String, TradeCategory> unassignedCategories = new HashMap<>();

        for (Map.Entry<Trade, Integer> entry : tradeAssignedWeights.entrySet())
        {
            Trade trade = entry.getKey();
            int assignedWeight = entry.getValue();

            if (assignedWeight < Classification.ONE_HUNDRED_PERCENT)
            {
                if (trade.getProfitLoss() == null || trade.getProfitLoss().getCurrencyCode() == null)
                    continue;

                String currencyCode = trade.getProfitLoss().getCurrencyCode();

                TradeCategory unassignedCategory = unassignedCategories.computeIfAbsent(currencyCode,
                                cc -> multiCurrencyMode ? new TradeCategory(unassignedClassification, converter, cc)
                                                : new TradeCategory(unassignedClassification, converter));

                double unassignedWeight = (Classification.ONE_HUNDRED_PERCENT - assignedWeight)
                                / (double) Classification.ONE_HUNDRED_PERCENT;
                unassignedCategory.addTrade(trade, unassignedWeight);
            }
        }

        for (TradeCategory unassignedCategory : unassignedCategories.values())
        {
            if (unassignedCategory.getTotalWeight() > 0)
                categories.add(unassignedCategory);
        }
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

    public List<Trade> getTrades()
    {
        return Collections.unmodifiableList(allTrades);
    }

    public Money getTotalProfitLoss()
    {
        return allTrades.stream().map(trade -> {
            Money pnl = trade.getProfitLoss();
            if (pnl == null)
                return Money.of(converter.getTermCurrency(), 0);

            LocalDate date = trade.getEnd().map(LocalDate::from).orElse(LocalDate.now());
            return pnl.with(converter.at(date));
        }).collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public Money getTotalProfitLossWithoutTaxesAndFees()
    {
        return allTrades.stream().map(trade -> {
            Money pnl = trade.getProfitLossWithoutTaxesAndFees();
            if (pnl == null)
                return Money.of(converter.getTermCurrency(), 0);

            LocalDate date = trade.getEnd().map(LocalDate::from).orElse(LocalDate.now());
            return pnl.with(converter.at(date));
        }).collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    /* package */ TradeCategory byClassification(Classification classification)
    {
        for (TradeCategory category : categories)
        {
            if (category.getTaxonomyClassification() == classification)
                return category;
        }

        return null;
    }
}
