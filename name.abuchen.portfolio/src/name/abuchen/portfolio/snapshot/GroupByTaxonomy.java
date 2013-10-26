package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Values;

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
    private final long valuation;
    private final List<AssetCategory> categories = new ArrayList<AssetCategory>();

    private GroupByTaxonomy(Taxonomy taxonomy, long valuation)
    {
        this.taxonomy = taxonomy;
        this.valuation = valuation;
    }

    /* package */GroupByTaxonomy(Taxonomy taxonomy, ClientSnapshot snapshot)
    {
        this(taxonomy, snapshot.getAssets());

        Map<InvestmentVehicle, Item> vehicle2position = new HashMap<InvestmentVehicle, Item>();

        // cash
        for (AccountSnapshot a : snapshot.getAccounts())
        {
            SecurityPosition sp = new SecurityPosition(null);
            sp.setShares(Values.Share.factor());
            sp.setPrice(new SecurityPrice(snapshot.getTime(), a.getFunds()));
            vehicle2position.put(a.getAccount(), new Item(sp));
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
        this(taxonomy, snapshot.getValue());

        Map<InvestmentVehicle, Item> vehicle2position = new HashMap<InvestmentVehicle, Item>();

        for (SecurityPosition pos : snapshot.getPositions())
            vehicle2position.put(pos.getSecurity(), new Item(pos));

        doGrouping(vehicle2position);
    }

    private void doGrouping(final Map<InvestmentVehicle, Item> vehicle2position)
    {
        if (taxonomy != null)
        {
            createCategoriesAndAllocate(vehicle2position);
            sortCategoriesByRank();
        }

        allocateLeftOvers(vehicle2position);
    }

    private void createCategoriesAndAllocate(final Map<InvestmentVehicle, Item> vehicle2position)
    {
        for (Classification classification : taxonomy.getRoot().getChildren())
        {
            final Map<InvestmentVehicle, Item> vehicle2item = new HashMap<InvestmentVehicle, Item>();

            // first: assign items to categories

            // item.weight records both the weight
            // (a) already assigned to any category
            // (b) assigned to this category

            classification.accept(new Taxonomy.Visitor()
            {
                @Override
                public void visit(Classification classification, Assignment assignment)
                {
                    Item item = vehicle2position.get(assignment.getInvestmentVehicle());
                    if (item != null)
                    {
                        item.weight += assignment.getWeight(); // record (a)

                        SecurityPosition position = item.position;
                        if (assignment.getWeight() == Classification.ONE_HUNDRED_PERCENT)
                        {
                            vehicle2item.put(assignment.getInvestmentVehicle(), item);
                        }
                        else
                        {
                            Item other = vehicle2item.get(assignment.getInvestmentVehicle());
                            if (other == null)
                            {
                                other = new Item(position);
                                vehicle2item.put(assignment.getInvestmentVehicle(), other);
                            }

                            // record (b) into the copy
                            other.weight += assignment.getWeight();
                        }
                    }
                }
            });

            // second: create asset category and positions

            if (!vehicle2item.isEmpty())
            {
                AssetCategory category = new AssetCategory(classification, valuation);
                categories.add(category);

                for (Entry<InvestmentVehicle, Item> entry : vehicle2item.entrySet())
                {
                    Item item = entry.getValue();
                    SecurityPosition position = item.position;
                    if (item.weight != Classification.ONE_HUNDRED_PERCENT)
                        position = SecurityPosition.split(position, item.weight);

                    category.addPosition(new AssetPosition(entry.getKey(), position, getValuation()));
                }

                // sort positions by name
                Collections.sort(category.getPositions());
            }
        }
    }

    private void sortCategoriesByRank()
    {
        Collections.sort(categories, new Comparator<AssetCategory>()
        {
            @Override
            public int compare(AssetCategory o1, AssetCategory o2)
            {
                int rank1 = o1.getClassification().getRank();
                int rank2 = o2.getClassification().getRank();
                return rank1 < rank2 ? -1 : rank1 == rank2 ? 0 : 1;
            }
        });
    }

    private void allocateLeftOvers(final Map<InvestmentVehicle, Item> vehicle2position)
    {
        Classification classification = new Classification(null, Classification.UNASSIGNED_ID,
                        Messages.LabelWithoutClassification);
        AssetCategory unassigned = new AssetCategory(classification, getValuation());

        for (Entry<InvestmentVehicle, Item> entry : vehicle2position.entrySet())
        {
            Item item = entry.getValue();

            if (item.weight < Classification.ONE_HUNDRED_PERCENT)
            {
                SecurityPosition position = item.position;
                if (item.weight != 0)
                    position = SecurityPosition.split(position, Classification.ONE_HUNDRED_PERCENT - item.weight);

                unassigned.addPosition(new AssetPosition(entry.getKey(), position, getValuation()));
            }
        }

        if (!unassigned.getPositions().isEmpty())
            categories.add(unassigned);
    }

    public long getValuation()
    {
        return valuation;
    }

    public long getFIFOPurchaseValue()
    {
        long purchaseValue = 0;
        for (AssetCategory category : categories)
            purchaseValue += category.getFIFOPurchaseValue();
        return purchaseValue;
    }

    public long getProfitLoss()
    {
        long profitLoss = 0;
        for (AssetCategory category : categories)
            profitLoss += category.getProfitLoss();
        return profitLoss;
    }

    public List<AssetCategory> asList()
    {
        return categories;
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
