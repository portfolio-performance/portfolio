package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TaxonomyBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;

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
                        .addPrice("2010-01-01", Values.Quote.factorize(10)) //
                        .assign(taxonomy, "debt") //
                        .addTo(client);

        Security c = new SecurityBuilder() //
                        .addPrice("2010-01-01", Values.Quote.factorize(12)) //
                        .assign(taxonomy, "equity") //
                        .addTo(client);

        Security d = new SecurityBuilder() //
                        .addPrice("2010-01-01", Values.Quote.factorize(12)) //
                        .assign(taxonomy, "equity") //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", Values.Share.factorize(10), 10000) //
                        .inbound_delivery(c, "2010-01-01", Values.Share.factorize(10), 12000) //
                        .inbound_delivery(d, "2010-01-01", Values.Share.factorize(10), 12000) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-01");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt.getValuation(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(debt.getPositions().size(), is(1));

        AssetCategory stocks = grouping.byClassification(taxonomy.getClassificationById("equity"));
        assertThat(stocks.getValuation(), is(Money.of(CurrencyUnit.EUR, 240_00)));
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
                        .addPrice("2010-01-01", Values.Quote.factorize(10)) //
                        .assign(taxonomy, "debt", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .assign(taxonomy, "equity", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", Values.Share.factorize(10), 10000) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-01");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt.getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(debt.getPositions().size(), is(1));

        AssetCategory equity = grouping.byClassification(taxonomy.getClassificationById("equity"));
        assertThat(equity.getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
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
                        .addPrice("2010-01-01", Values.Quote.factorize(10)) //
                        .assign(taxonomy, "cat1", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .assign(taxonomy, "cat2", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", Values.Share.factorize(10), 10000) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-01");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        assertThat(grouping.asList().size(), is(1));

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));

        assertThat(debt.getPositions().size(), is(1));

        assertThat(debt.getValuation(), is(Money.of(CurrencyUnit.EUR, 100_00)));
    }

    @Test
    public void testSecuritiesWithoutAssignment()
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", Values.Quote.factorize(10)) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", Values.Share.factorize(10), 10000) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-01");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt, nullValue());

        List<AssetCategory> categories = grouping.asList();
        assertThat(categories.size(), is(1));

        AssetCategory unassigned = categories.get(0);
        assertThat(unassigned.getValuation(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(unassigned.getPositions().size(), is(1));
    }

    @SuppressWarnings("null")
    @Test
    public void testSecuritiesWithPartialAssignment()
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", Values.Quote.factorize(10)) //
                        .assign(taxonomy, "debt", Classification.ONE_HUNDRED_PERCENT / 2) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .inbound_delivery(a, "2010-01-01", Values.Share.factorize(10), 10000) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-01");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        assertThat(grouping.asList().size(), is(2));

        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt.getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(debt.getPositions().size(), is(1));

        AssetCategory unassigned = null;
        for (AssetCategory category : grouping.asList())
            if (category.getClassification().getId().equals(Classification.UNASSIGNED_ID))
                unassigned = category;

        assertThat(unassigned, notNullValue());
        assertThat(unassigned.getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(unassigned.getPositions().size(), is(1));
    }

    @Test
    public void testThatAccountsAreClassifiedCorrectlyWhenFiltered()
    {
        // bug report:
        // https://forum.portfolio-performance.info/t/vermoegensaufstellung-klassifizierung-mit-filter/1129

        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("debt") //
                        .addTo(client);

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", Values.Quote.factorize(10)) //
                        .assign(taxonomy, "debt", Classification.ONE_HUNDRED_PERCENT) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2010-01-01", Values.Amount.factorize(100))
                        .assign(taxonomy, "debt", Classification.ONE_HUNDRED_PERCENT) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .inbound_delivery(a, "2010-01-01", Values.Share.factorize(10), 10000) //
                        .addTo(client);

        ClientFilter filter = new ClientClassificationFilter(taxonomy.getClassificationById("debt"));
        
        LocalDate date = LocalDate.parse("2010-01-01");
        ClientSnapshot snapshot = ClientSnapshot.create(filter.filter(client), new TestCurrencyConverter(), date);
        assertNotNull(snapshot);

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(taxonomy);

        // everything is classified
        assertThat(grouping.asList().size(), is(1));

        // two positions in 'debt' category
        AssetCategory debt = grouping.byClassification(taxonomy.getClassificationById("debt"));
        assertThat(debt.getValuation(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(200))));
        assertThat(debt.getPositions().size(), is(2));
        
        // nothing in unassigned
        AssetCategory unassigned = null;
        for (AssetCategory category : grouping.asList())
            if (category.getClassification().getId().equals(Classification.UNASSIGNED_ID))
                unassigned = category;
        
        assertThat(unassigned, nullValue());
    }
}
