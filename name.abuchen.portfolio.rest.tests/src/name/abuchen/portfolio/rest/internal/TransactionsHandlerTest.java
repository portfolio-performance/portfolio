package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * The bookings of a file as one list: both families in one answer, oldest first,
 * filterable by period, by either side of a booking and by type.
 */
public class TransactionsHandlerTest
{
    private static final String DEPOSIT = "2021-01-04";
    private static final String BUY = "2021-03-01";
    private static final String DIVIDEND = "2021-06-15";

    private Account cash;
    private Portfolio portfolio;

    /** a deposit and a dividend in the cash account, a purchase in the depot */
    private Client client()
    {
        var client = new Client();

        cash = new Account();
        cash.setName("Giro");
        client.addAccount(cash);

        portfolio = new Portfolio();
        portfolio.setName("Depot");
        portfolio.setReferenceAccount(cash);
        client.addPortfolio(portfolio);

        var security = new Security();
        security.setName("Aktie");
        security.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(security);

        cash.addTransaction(new AccountTransaction(LocalDateTime.of(2021, 1, 4, 9, 0), CurrencyUnit.EUR, 50_000L, null, //$NON-NLS-1$
                        AccountTransaction.Type.DEPOSIT));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.of(2021, 3, 1, 10, 30), CurrencyUnit.EUR, 10_123L, //$NON-NLS-1$
                        security, 10_000L, PortfolioTransaction.Type.BUY, 123L, 0L));
        cash.addTransaction(new AccountTransaction(LocalDateTime.of(2021, 6, 15, 8, 0), CurrencyUnit.EUR, 250L, security, //$NON-NLS-1$
                        AccountTransaction.Type.DIVIDENDS));

        return client;
    }

    /** a purchase names both sides, a deposit only the money side */
    @Test
    public void testBothFamiliesOldestFirst()
    {
        var items = items(client(), null, null);

        assertThat(items.size(), is(3));
        assertThat(items.get(0).getAsJsonObject().get("date").getAsString(), is(DEPOSIT));
        assertThat(items.get(1).getAsJsonObject().get("date").getAsString(), is(BUY));
        assertThat(items.get(2).getAsJsonObject().get("date").getAsString(), is(DIVIDEND));

        var buy = items.get(1).getAsJsonObject();
        assertThat(buy.get("type").getAsString(), is("BUY"));
        assertThat(buy.get("time").getAsString(), is("10:30:00"));
        assertThat(buy.getAsJsonObject("instrument").get("name").getAsString(), is("Aktie"));
        assertThat(buy.getAsJsonObject("investmentAccount").get("name").getAsString(), is("Depot"));
        // the settlement account is not on the booking, the file derives it
        assertThat(buy.getAsJsonObject("cashAccount").get("name").getAsString(), is("Giro"));
        assertThat(buy.has("shares"), is(true));
        assertThat(buy.getAsJsonObject("fees").get("value").getAsDouble(), is(1.23d));

        var deposit = items.get(0).getAsJsonObject();
        assertThat(deposit.get("type").getAsString(), is("DEPOSIT"));
        assertThat(deposit.has("instrument"), is(false));
        assertThat(deposit.has("investmentAccount"), is(false));
        assertThat(deposit.has("grossAmount"), is(false));
        assertThat(deposit.has("shares"), is(false));
    }

    /** both ends of the period belong to the answer */
    @Test
    public void testPeriodIsInclusiveOnBothEnds()
    {
        var client = client();
        assertThat(items(client, "2021-01-04", "2021-06-15").size(), is(3)); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(items(client, "2021-01-05", null).size(), is(2)); //$NON-NLS-1$
        assertThat(items(client, null, "2021-06-14").size(), is(2)); //$NON-NLS-1$
    }

    /** a deposit is booked in money and never in a securities account */
    @Test
    public void testAnInvestmentAccountNeverYieldsADeposit()
    {
        var client = client();

        var ofPortfolio = items(list(client, null, null, null, portfolio.getUUID(), null, null, null, null));
        assertThat(ofPortfolio.size(), is(1));
        assertThat(ofPortfolio.get(0).getAsJsonObject().get("type").getAsString(), is("BUY"));

        // the same cash account also holds the money side of the purchase
        var ofCash = items(list(client, null, null, cash.getUUID(), null, null, null, null, null));
        assertThat(ofCash.size(), is(3));
    }

    /** a name of the other family selects nothing rather than failing */
    @Test
    public void testTypesSelectAndUnknownTypesFail()
    {
        var client = client();
        assertThat(items(list(client, null, null, null, null, null, "dividends", null, null)).size(), is(1)); //$NON-NLS-1$
        assertThat(items(list(client, null, null, null, null, null, "BUY,SELL", null, null)).size(), is(1)); //$NON-NLS-1$
        assertThat(items(list(client, null, null, null, null, null, "deposit,buy", null, null)).size(), is(2)); //$NON-NLS-1$

        try
        {
            list(client, null, null, null, null, null, "kaufen", null, null); //$NON-NLS-1$
            org.junit.Assert.fail("expected ApiException"); //$NON-NLS-1$
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("type")); //$NON-NLS-1$
            assertThat(e.getErrors().get(0).code(), is("invalid-value")); //$NON-NLS-1$
        }
    }

    /** the page after the last one is empty, and says so */
    @Test
    public void testPagesContinueWhereThePageBeforeStopped()
    {
        var client = client();

        var first = list(client, null, null, null, null, null, null, null, "2");
        assertThat(items(first).size(), is(2));

        var page = first.get("page").getAsJsonObject();
        assertThat(page.get("total").getAsInt(), is(3));
        assertThat(page.has("nextCursor"), is(true));

        var second = list(client, null, null, null, null, null, null, page.get("nextCursor").getAsString(), "2");
        assertThat(items(second).size(), is(1));
        assertThat(items(second).get(0).getAsJsonObject().get("date").getAsString(), is(DIVIDEND));
        assertThat(second.get("page").getAsJsonObject().get("nextCursor").isJsonNull(), is(true));
    }

    /** an unknown account is a field error, not an empty list */
    @Test
    public void testAnUnknownReferenceFails()
    {
        try
        {
            list(client(), null, null, "no-such-account", null, null, null, null, null); //$NON-NLS-1$
            org.junit.Assert.fail("expected ApiException"); //$NON-NLS-1$
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("cashAccount")); //$NON-NLS-1$
            assertThat(e.getErrors().get(0).code(), is("invalid-value")); //$NON-NLS-1$
        }
    }

    private static JsonArray items(Client client, String from, String to)
    {
        return items(list(client, from, to, null, null, null, null, null, null));
    }

    private static JsonArray items(JsonObject json)
    {
        return json.get("items").getAsJsonArray(); //$NON-NLS-1$
    }

    private static JsonObject list(Client client, String from, String to, String cashAccount,
                    String investmentAccount, String instrument, String type, String cursor, String limit)
    {
        return TransactionsHandler.list(client, from, to, cashAccount, investmentAccount, instrument, type, cursor,
                        limit);
    }
}
