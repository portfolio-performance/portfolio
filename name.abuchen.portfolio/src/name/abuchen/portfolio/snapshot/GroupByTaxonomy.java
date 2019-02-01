package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

public final class GroupByTaxonomy
{
    private static final class Item
    {
        private SecurityPosition position;
        private int weight;

        private Item(SecurityPosition position)
        {
            this.position = position;
        }
    }

    private final Taxonomy taxonomy;
    private final CurrencyConverter converter;
    private final LocalDate date;
    private final Money valuation;
    private final List<AssetCategory> categories = new ArrayList<>();

    private GroupByTaxonomy(Taxonomy taxonomy, CurrencyConverter converter, LocalDate date, Money valuation)
    {
        this.taxonomy = taxonomy;
        this.converter = converter;
        this.date = date;
        this.valuation = valuation;
    }

    /* package */ GroupByTaxonomy(Taxonomy taxonomy, ClientSnapshot snapshot)
    {
        this(taxonomy, snapshot.getCurrencyConverter(), snapshot.getTime(), snapshot.getMonetaryAssets());

        Map<InvestmentVehicle, Item> vehicle2position = new HashMap<>();

        // cash
        for (AccountSnapshot account : snapshot.getAccounts())
        {
            if (account.getFunds().isZero())
                continue;

            vehicle2position.put(account.unwrapAccount(), new Item(new SecurityPosition(account)));
        }

        // portfolio
        if (snapshot.getJointPortfolio() != null)
        {
            for (SecurityPosition pos : snapshot.getJointPortfolio().getPositions())
                vehicle2position.put(pos.getSecurity(), new Item(pos));
        }

        doGrouping(vehicle2position);
    }

    /* package */public GroupByTaxonomy(Taxonomy taxonomy, PortfolioSnapshot snapshot)
    {
        this(taxonomy, snapshot.getCurrencyConverter(), snapshot.getTime(), snapshot.getValue());

        Map<InvestmentVehicle, Item> vehicle2position = new HashMap<>();

        for (SecurityPosition pos : snapshot.getPositions())
            vehicle2position.put(pos.getSecurity(), new Item(pos));

        doGrouping(vehicle2position);
    }

    private void doGrouping(final Map<InvestmentVehicle, Item> vehicle2position)
    {
        if (taxonomy != null)
        {
            createCategoriesAndAllocate(vehicle2position);

            Collections.sort(categories, (r, l) -> Integer.compare(r.getClassification().getRank(),
                            l.getClassification().getRank()));
        }

        allocateLeftOvers(vehicle2position);
    }

    private void createCategoriesAndAllocate(final Map<InvestmentVehicle, Item> vehicle2position)
    {
        for (Classification classification : taxonomy.getRoot().getChildren())
        {
            final Map<InvestmentVehicle, Item> vehicle2item = new HashMap<>();

            // first: assign items to categories

            // item.weight records the weight
            // (a) already assigned to any category (in vechile2position)
            // (b) assigned to this category (in vehicle2item)

            classification.accept(new Taxonomy.Visitor()
            {
                @Override
                public void visit(Classification classification, Assignment assignment)
                {
                    Item item = vehicle2position.get(assignment.getInvestmentVehicle());
                    if (item != null)
                    {
                        // record (a)
                        item.weight += assignment.getWeight();

                        // add to map of this classification + record (b)
                        vehicle2item.computeIfAbsent(assignment.getInvestmentVehicle(),
                                        v -> new Item(item.position)).weight += assignment.getWeight();
                    }
                }
            });

            // second: create asset category and positions

            if (!vehicle2item.isEmpty())
            {
                AssetCategory category = new AssetCategory(classification, converter, date, valuation);
                categories.add(category);

                for (Entry<InvestmentVehicle, Item> entry : vehicle2item.entrySet())
                {
                    Item item = entry.getValue();
                    SecurityPosition position = item.position;
                    if (item.weight != Classification.ONE_HUNDRED_PERCENT)
                        position = SecurityPosition.split(position, item.weight);

                    category.addPosition(new AssetPosition(position, converter, date, getValuation()));
                }

                // sort positions by name
                Collections.sort(category.getPositions(), new AssetPosition.ByDescription());
            }
        }
    }

    private void allocateLeftOvers(final Map<InvestmentVehicle, Item> vehicle2position)
    {
        Classification classification = new Classification(null, Classification.UNASSIGNED_ID,
                        Messages.LabelWithoutClassification);
        AssetCategory unassigned = new AssetCategory(classification, converter, date, getValuation());

        for (Entry<InvestmentVehicle, Item> entry : vehicle2position.entrySet())
        {
            Item item = entry.getValue();

            if (item.weight < Classification.ONE_HUNDRED_PERCENT)
            {
                SecurityPosition position = item.position;
                if (item.weight != 0)
                    position = SecurityPosition.split(position, Classification.ONE_HUNDRED_PERCENT - item.weight);

                unassigned.addPosition(new AssetPosition(position, converter, date, getValuation()));
            }
        }

        if (!unassigned.getPositions().isEmpty())
            categories.add(unassigned);
    }

    public LocalDate getDate()
    {
        return date;
    }

    public Money getValuation()
    {
        return valuation;
    }

    public Money getFIFOPurchaseValue()
    {
        return categories.stream().map(AssetCategory::getFIFOPurchaseValue)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public Money getMovingAveragePurchaseValue()
    {
        return categories.stream().map(AssetCategory::getMovingAveragePurchaseValue)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public Money getProfitLoss()
    {
        return categories.stream().map(AssetCategory::getProfitLoss)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public List<AssetCategory> asList()
    {
        return categories;
    }

    public Stream<AssetCategory> getCategories()
    {
        return categories.stream();
    }

    /* package */AssetCategory byClassification(Classification classification)
    {
        for (AssetCategory category : categories)
        {
            if (category.getClassification().equals(classification))
                return category;
        }

        return null;
    }
}
