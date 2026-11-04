package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class TaxonomyCopyTest
{
    private Security security;
    private Taxonomy taxonomy;

    @Before
    public void setUp()
    {
        security = new Security("Test Security", CurrencyUnit.EUR);

        taxonomy = new Taxonomy("Test Taxonomy");

        var root = new Classification(null, "root", "Test Taxonomy", "#000000");
        root.setNote("Root note");
        taxonomy.setRootNode(root);

        var level1 = new Classification(root, "level-1", "Level 1", "#FF0000");
        level1.setNote("Level 1 note");
        level1.setKey("level-1-key");
        level1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        level1.setRank(1);
        root.addChild(level1);

        var level1b = new Classification(root, "level-1b", "Level 1b", "#00FF00");
        level1b.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        level1b.setRank(2);
        root.addChild(level1b);

        var level2 = new Classification(level1, "level-2", "Level 2", "#0000FF");
        level2.setNote("Level 2 note");
        level2.setKey("level-2-key");
        level2.setRank(3);
        level1.addChild(level2);

        var assignment = new Assignment(security, Classification.ONE_HUNDRED_PERCENT / 4);
        assignment.setRank(7);
        level2.addAssignment(assignment);

        taxonomy.setUsedForRebalancing(security, false);
    }

    @Test
    public void testCopyKeepsAllPropertiesOfClassifications()
    {
        var copy = taxonomy.copy();

        assertThat(copy.getName(), is(taxonomy.getName()));
        assertClassificationEquals(copy.getRoot(), taxonomy.getRoot());
    }

    @Test
    public void testCopyKeepsNotes()
    {
        var copy = taxonomy.copy();

        assertThat(copy.getRoot().getNote(), is("Root note"));
        assertThat(copy.getRoot().getChildren().get(0).getNote(), is("Level 1 note"));
        assertThat(copy.getRoot().getChildren().get(0).getChildren().get(0).getNote(), is("Level 2 note"));
    }

    @Test
    public void testCopyKeepsRebalancingFlag()
    {
        var copy = taxonomy.copy();

        assertThat(copy.isUsedForRebalancing(security), is(false));
    }

    @Test
    public void testCopyGeneratesNewIds()
    {
        var copy = taxonomy.copy();

        assertThat(copy.getId(), is(not(taxonomy.getId())));

        var originalIds = new HashSet<String>();
        taxonomy.getAllClassifications().forEach(c -> originalIds.add(c.getId()));

        for (Classification c : copy.getAllClassifications())
            assertThat(originalIds, not(hasItem(c.getId())));
    }

    private void assertClassificationEquals(Classification actual, Classification expected)
    {
        assertThat(actual, is(not(sameInstance(expected))));

        assertThat(actual.getName(), is(expected.getName()));
        assertThat(actual.getNote(), is(expected.getNote()));
        assertThat(actual.getColor(), is(expected.getColor()));
        assertThat(actual.getWeight(), is(expected.getWeight()));
        assertThat(actual.getRank(), is(expected.getRank()));
        assertThat(actual.getKey(), is(expected.getKey()));

        assertThat(actual.getAssignments().size(), is(expected.getAssignments().size()));
        for (var index = 0; index < expected.getAssignments().size(); index++)
        {
            var actualAssignment = actual.getAssignments().get(index);
            var expectedAssignment = expected.getAssignments().get(index);

            assertThat(actualAssignment, is(not(sameInstance(expectedAssignment))));
            assertThat(actualAssignment.getInvestmentVehicle(),
                            is(sameInstance(expectedAssignment.getInvestmentVehicle())));
            assertThat(actualAssignment.getWeight(), is(expectedAssignment.getWeight()));
            assertThat(actualAssignment.getRank(), is(expectedAssignment.getRank()));
        }

        assertThat(actual.getChildren().size(), is(expected.getChildren().size()));
        for (var index = 0; index < expected.getChildren().size(); index++)
        {
            var actualChild = actual.getChildren().get(index);
            assertThat(actualChild.getParent(), is(sameInstance(actual)));
            assertClassificationEquals(actualChild, expected.getChildren().get(index));
        }
    }
}
