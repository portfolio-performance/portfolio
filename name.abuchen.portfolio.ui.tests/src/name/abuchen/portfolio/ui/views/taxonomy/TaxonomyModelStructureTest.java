package name.abuchen.portfolio.ui.views.taxonomy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TaxonomyModelStructureTest
{
    private Client client;
    private Security security;
    private Taxonomy taxonomy;
    private Classification x;
    private Classification y;
    private Assignment assignment;
    private TaxonomyModel model;

    @Before
    public void setUp()
    {
        client = new Client();

        security = new Security("A", CurrencyUnit.EUR);
        security.setLatest(new LatestSecurityPrice(LocalDate.now(), 100L * Values.Quote.factorToMoney()));
        client.addSecurity(security);

        taxonomy = new Taxonomy("Taxonomy");
        var root = new Classification(null, "root", "Taxonomy");
        taxonomy.setRootNode(root);

        x = new Classification(root, "x", "X");
        root.addChild(x);

        y = new Classification(root, "y", "Y");
        y.setWeight(0);
        root.addChild(y);

        assignment = new Assignment(security);
        x.addAssignment(assignment);

        client.addTaxonomy(taxonomy);

        model = new TaxonomyModel(new ExchangeRateProviderFactory(client), client, taxonomy);
    }

    @Test
    public void testNewModelIsNotOutdated()
    {
        assertThat(model.isStructureOutdated(), is(false));
    }

    @Test
    public void testMovedAssignmentMakesModelOutdated()
    {
        // e.g. the instrument was moved to another category in the edit
        // security dialog
        x.removeAssignment(assignment);
        y.addAssignment(assignment);

        assertThat(model.isStructureOutdated(), is(true));
    }

    @Test
    public void testAddedCategoryMakesModelOutdated()
    {
        // e.g. a taxonomy import added a category
        taxonomy.getRoot().addChild(new Classification(taxonomy.getRoot(), "z", "Z"));

        assertThat(model.isStructureOutdated(), is(true));
    }

    @Test
    public void testOwnEditDoesNotMakeModelOutdated()
    {
        // edits of the view go through the node tree and TaxonomyModel#markDirty
        var nodeX = model.getClassificationRootNode().getChildren().get(0);
        nodeX.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        model.markDirty();

        assertThat(model.isStructureOutdated(), is(false));
    }

    @Test
    public void testMarkDirtyNotifiesListenerAfterBaselineIsRefreshed()
    {
        var isOutdatedWhenNotified = new boolean[] { true };
        model.addDirtyListener(() -> isOutdatedWhenNotified[0] = model.isStructureOutdated());

        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        model.markDirty();

        assertThat(isOutdatedWhenNotified[0], is(false));
    }

    @Test
    public void testChangedPriceDoesNotMakeModelOutdated()
    {
        security.setLatest(new LatestSecurityPrice(LocalDate.now(), 200L * Values.Quote.factorToMoney()));
        model.updateClientSnapshot(client);

        assertThat(model.isStructureOutdated(), is(false));
    }
}
