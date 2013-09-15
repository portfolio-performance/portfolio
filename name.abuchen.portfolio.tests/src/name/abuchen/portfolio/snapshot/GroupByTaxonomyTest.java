package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

import org.joda.time.DateTime;
import org.junit.Test;

@SuppressWarnings("nls")
public class GroupByTaxonomyTest
{
    @Test
    public void testThatSecuritiesAreGroupedIntoClassifications()
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addClassification("equity") //
                        .addClassification("realestate") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1000) //
                        .assign(taxonomy, "debt") //
                        .addTo(client);

        Security c = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1200) //
                        .assign(taxonomy, "equity") //
                        .addTo(client);

        Security d = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1200) //
                        .assign(taxonomy, "equity") //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", 1000000, 10000) //
                        .inbound_delivery(c, "2010-01-01", 1000000, 12000) //
                        .inbound_delivery(d, "2010-01-01", 1000000, 12000) //
                        .addTo(client);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new DateTime("2010-01-01").toDate());
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt.getValuation(), is(10000L));
        assertThat(debt.getPositions().size(), is(1));

        AssetCategory stocks = grouping.byClassification(taxonomy.getClassificationById("equity"));
        assertThat(stocks.getValuation(), is(24000L));
        assertThat(stocks.getPositions().size(), is(2));

        AssetCategory realEstate = grouping.byClassification(taxonomy.getClassificationById("realestate"));
        assertThat(realEstate, nullValue());
    }

    @Test
    public void testThatPartialAssignmentsAreSeparated()
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addClassification("equity") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1000) //
                        .assign(taxonomy, "debt", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .assign(taxonomy, "equity", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", 1000000, 10000) //
                        .addTo(client);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new DateTime("2010-01-01").toDate());
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt.getValuation(), is(5000L));
        assertThat(debt.getPositions().size(), is(1));

        AssetCategory equity = grouping.byClassification(taxonomy.getClassificationById("equity"));
        assertThat(equity.getValuation(), is(5000L));
        assertThat(equity.getPositions().size(), is(1));
    }

    @Test
    public void testThatPartialAssignmentsInSubClassificationsAreMerged()
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addClassification("debt", "cat1") //
                        .addClassification("debt", "cat2") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1000) //
                        .assign(taxonomy, "cat1", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .assign(taxonomy, "cat2", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", 1000000, 10000) //
                        .addTo(client);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new DateTime("2010-01-01").toDate());
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        assertThat(grouping.asList().size(), is(1));

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));

        assertThat(debt.getPositions().size(), is(1));

        assertThat(debt.getValuation(), is(10000L));
    }

    @Test
    public void testSecuritiesWithoutAssignment()
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1000) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", 1000000, 10000) //
                        .addTo(client);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new DateTime("2010-01-01").toDate());
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt, nullValue());

        List<AssetCategory> categories = grouping.asList();
        assertThat(categories.size(), is(1));

        AssetCategory unassigned = categories.get(0);
        assertThat(unassigned.getValuation(), is(10000L));
        assertThat(unassigned.getPositions().size(), is(1));
    }

    @Test
    public void testSecuritiesWithPartialAssignment()
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1000) //
                        .assign(taxonomy, "debt", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", 1000000, 10000) //
                        .addTo(client);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new DateTime("2010-01-01").toDate());
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        assertThat(grouping.asList().size(), is(2));

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt.getValuation(), is(5000L));
        assertThat(debt.getPositions().size(), is(1));

        AssetCategory unassigned = null;
        for (AssetCategory category : grouping.asList())
            if (category.getClassification().getId().equals(Classification.UNASSIGNED_ID))
                unassigned = category;

        assertThat(unassigned, notNullValue());
        assertThat(unassigned.getValuation(), is(5000L));
        assertThat(unassigned.getPositions().size(), is(1));
    }

}
