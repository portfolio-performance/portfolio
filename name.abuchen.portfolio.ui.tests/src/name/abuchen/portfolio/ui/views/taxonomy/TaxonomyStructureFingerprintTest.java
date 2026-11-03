package name.abuchen.portfolio.ui.views.taxonomy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TaxonomyStructureFingerprintTest
{
    private Security securityA;
    private Security securityB;

    private Taxonomy taxonomy;
    private Classification root;
    private Classification x;
    private Classification y;
    private Assignment assignmentA;

    // @formatter:off
    // - Root
    //   - X (50 %)
    //     - Security A (100 %)
    //   - Y (50 %)
    //     - Security B (100 %)
    // @formatter:on
    @Before
    public void setUp()
    {
        securityA = new Security("A", CurrencyUnit.EUR);
        securityA.setLatest(new LatestSecurityPrice(LocalDate.now(), 100L * Values.Quote.factorToMoney()));
        securityB = new Security("B", CurrencyUnit.EUR);

        taxonomy = new Taxonomy("Taxonomy");
        root = new Classification(null, "root", "Taxonomy");
        taxonomy.setRootNode(root);

        x = new Classification(root, "x", "X");
        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        x.setRank(1);
        root.addChild(x);

        y = new Classification(root, "y", "Y");
        y.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        y.setRank(2);
        root.addChild(y);

        assignmentA = new Assignment(securityA);
        x.addAssignment(assignmentA);

        y.addAssignment(new Assignment(securityB));
    }

    @Test
    public void testSameTreeGivesSameValue()
    {
        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(TaxonomyStructureFingerprint.of(taxonomy)));
    }

    @Test
    public void testAddedCategoryChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        root.addChild(new Classification(root, "z", "Z"));

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testRemovedCategoryChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        root.getChildren().remove(y);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testMovedCategoryChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        root.getChildren().remove(y);
        y.setParent(x);
        x.addChild(y);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testChangedCategoryRankChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        x.setRank(3);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testChangedCategoryWeightChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        x.setWeight(Classification.ONE_HUNDRED_PERCENT);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testAddedAssignmentChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        y.addAssignment(new Assignment(securityA, 0));

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testRemovedAssignmentChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        x.removeAssignment(assignmentA);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testMovedAssignmentChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        x.removeAssignment(assignmentA);
        y.addAssignment(assignmentA);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testChangedAssignmentWeightChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        assignmentA.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testChangedAssignmentRankChangesValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        assignmentA.setRank(5);

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(not(before)));
    }

    @Test
    public void testChangedSecurityPriceKeepsValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        securityA.setLatest(new LatestSecurityPrice(LocalDate.now(), 200L * Values.Quote.factorToMoney()));

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(before));
    }

    @Test
    public void testChangedNameColorAndNoteKeepValue()
    {
        var before = TaxonomyStructureFingerprint.of(taxonomy);

        x.setName("X renamed");
        x.setColor("#123456");
        x.setNote("Note");
        taxonomy.setName("Taxonomy renamed");

        assertThat(TaxonomyStructureFingerprint.of(taxonomy), is(before));
    }

    @Test
    public void testTaxonomyWithoutRoot()
    {
        var empty = new Taxonomy("Empty");

        assertThat(TaxonomyStructureFingerprint.of(empty), is(TaxonomyStructureFingerprint.of(empty)));
    }
}
