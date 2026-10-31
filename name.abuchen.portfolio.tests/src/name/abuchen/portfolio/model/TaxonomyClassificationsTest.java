package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * The index returned by
 * {@link Taxonomy#getClassificationsByInvestmentVehicle()} must contain the
 * same classifications in the same order as
 * {@link Taxonomy#getClassifications(InvestmentVehicle)}.
 */
public class TaxonomyClassificationsTest
{
    @Test
    public void testIndexIsOrderedLikeGetClassifications()
    {
        Taxonomy taxonomy = new Taxonomy("test");
        Classification root = new Classification("root", "root");
        taxonomy.setRootNode(root);

        // children are deliberately not stored in rank order
        Classification second = new Classification(root, "second", "second");
        second.setRank(2);
        Classification first = new Classification(root, "first", "first");
        first.setRank(1);
        Classification nested = new Classification(second, "nested", "nested");
        root.addChild(second);
        root.addChild(first);
        second.addChild(nested);

        Security security = new Security("A", CurrencyUnit.EUR);
        Security unassigned = new Security("B", CurrencyUnit.EUR);

        second.addAssignment(new Assignment(security));
        nested.addAssignment(new Assignment(security));
        first.addAssignment(new Assignment(security));

        Map<InvestmentVehicle, List<Classification>> index = taxonomy.getClassificationsByInvestmentVehicle();

        // children sorted by rank, descendants before the own assignments
        assertThat(index.get(security), contains(first, nested, second));
        assertThat(index.get(security), is(taxonomy.getClassifications(security)));
        assertThat(index.get(unassigned), is(nullValue()));
    }

    @Test
    public void testIndexMatchesGetClassificationsForAllTemplates()
    {
        Random random = new Random(42);

        for (TaxonomyTemplate template : TaxonomyTemplate.list())
        {
            Taxonomy taxonomy = template.build();
            List<Classification> classifications = taxonomy.getAllClassifications();

            List<Security> securities = new ArrayList<>();
            for (int ii = 0; ii < 50; ii++)
                securities.add(new Security("S" + ii, CurrencyUnit.EUR));

            // assign securities (partly multiple times) to random
            // classifications and shuffle the ranks to cover unsorted children
            for (int ii = 0; ii < 120; ii++)
            {
                Classification c = classifications.get(random.nextInt(classifications.size()));
                c.addAssignment(new Assignment(securities.get(random.nextInt(securities.size()))));
            }

            for (Classification c : classifications)
                c.setRank(random.nextInt(5));

            Map<InvestmentVehicle, List<Classification>> index = taxonomy.getClassificationsByInvestmentVehicle();

            for (Security security : securities)
                assertThat(index.getOrDefault(security, List.of()), is(taxonomy.getClassifications(security)));
        }
    }
}
