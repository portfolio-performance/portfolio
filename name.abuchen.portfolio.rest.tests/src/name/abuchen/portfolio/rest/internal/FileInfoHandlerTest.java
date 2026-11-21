package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;

/**
 * The file itself: the currency it counts in and how much of each kind of thing
 * it holds. Nothing here reads a transaction, so the answer is what a reader
 * takes first in order to know which of the larger reports is worth asking for.
 */
@SuppressWarnings("nls")
public class FileInfoHandlerTest
{
    private static Account account(Client client, String name)
    {
        var account = new Account();
        account.setName(name);
        account.setCurrencyCode("EUR");
        client.addAccount(account);
        return account;
    }

    private static Security security(Client client, String name)
    {
        var security = new Security();
        security.setName(name);
        security.setCurrencyCode("EUR");
        client.addSecurity(security);
        return security;
    }

    @Test
    public void testEmptyFileStillNamesItsCurrency()
    {
        var json = FileInfoHandler.info(new Client());

        assertThat(json.get("baseCurrency").getAsString(), is("EUR"));
        assertThat(json.get("currencies").getAsJsonArray().size(), is(1));
        assertThat(json.get("counts").getAsJsonObject().get("instruments").getAsInt(), is(0));
        assertThat(json.get("counts").getAsJsonObject().get("classes").getAsInt(), is(0));
    }

    @Test
    public void testBaseCurrencyAsTheFileSetsIt()
    {
        var client = new Client();
        client.setBaseCurrency("USD");

        assertThat(FileInfoHandler.info(client).get("baseCurrency").getAsString(), is("USD"));
    }

    /** every kind of thing the file can hold, counted */
    @Test
    public void testCounts()
    {
        var client = new Client();

        var cash = account(client, "Giro");
        account(client, "Tagesgeld");

        var portfolio = new Portfolio();
        portfolio.setName("Depot");
        portfolio.setReferenceAccount(cash);
        client.addPortfolio(portfolio);

        security(client, "ACME");
        security(client, "Other");

        var plan = new InvestmentPlan("Sparplan");
        plan.setType(InvestmentPlan.Type.DEPOSIT);
        plan.setAccount(cash);
        plan.setStart(LocalDateTime.of(2021, 1, 4, 0, 0));
        // the file keeps a weekly plan as a number above a threshold; one below it
        // is the monthly plan the application shows
        plan.setInterval(1);
        plan.setAmount(100_00L);
        client.addPlan(plan);

        var watchlist = new Watchlist();
        watchlist.setName("Beobachtung");
        client.addWatchlist(watchlist);

        new TaxonomyBuilder().addClassification("equity").addClassification("equity", "growth").addTo(client);

        var counts = FileInfoHandler.info(client).get("counts").getAsJsonObject();
        assertThat(counts.get("instruments").getAsInt(), is(2));
        assertThat(counts.get("cashAccounts").getAsInt(), is(2));
        assertThat(counts.get("investmentAccounts").getAsInt(), is(1));
        assertThat(counts.get("investmentPlans").getAsInt(), is(1));
        assertThat(counts.get("watchlists").getAsInt(), is(1));
        assertThat(counts.get("taxonomies").getAsInt(), is(1));
        // the tree as the application shows it: root, class, and the one below it
        assertThat(counts.get("classes").getAsInt(), is(3));
    }
}
