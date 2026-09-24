package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;

/** A create with an already used {@code clientRef} answers the existing transaction. */
@SuppressWarnings("nls")
public class IdempotencyTest
{
    private TransactionFixture f;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private String buy(String clientRef)
    {
        return "{'type':'buy','date':'2026-03-02','clientRef':'" + clientRef + "','investmentAccount':'"
                        + f.broker.getUUID() + "','cashAccount':'" + f.cash.getUUID() + "','instrument':'"
                        + f.eurSecurity.getUUID() + "','shares':'10','quote':'100'}";
    }

    @Test
    public void testSecondCreateWithSameClientRefReturnsTheFirst()
    {
        var first = TransactionsHandler.create(f.context(false, "broker-1"), json(buy("broker-1")));
        f.file.setDirty(false);

        // a retry, even with a different body, creates nothing
        var second = TransactionsHandler.create(f.context(false, "broker-1"),
                        json(buy("broker-1").replace("'quote':'100'", "'quote':'200'")));

        assertThat(first.changed(), is(true));
        assertThat(second.changed(), is(false));
        assertThat(second.entity().get("replayed").getAsBoolean(), is(true));
        assertThat(second.entity().get("uuid").getAsString(), is(first.entity().get("uuid").getAsString()));
        assertThat(second.entity().getAsJsonObject("linked").get("uuid").getAsString(),
                        is(first.entity().getAsJsonObject("linked").get("uuid").getAsString()));
        assertThat(f.legCount(), is(2));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testSourceIsSetOnBothLegs()
    {
        var json = TransactionsHandler.create(f.context(false, "broker-2"), json(buy("broker-2"))).entity();

        var entry = (BuySellEntry) f.find(json.get("uuid").getAsString()).getTransaction().getCrossEntry();
        assertThat(entry.getPortfolioTransaction().getSource(), is("api:broker-2"));
        assertThat(entry.getAccountTransaction().getSource(), is("api:broker-2"));
        assertThat(json.get("clientRef").getAsString(), is("broker-2"));

        var transfer = TransactionsHandler.create(f.context(false, "t-1"),
                        json("{'type':'cash-transfer','date':'2026-05-01','fromCashAccount':'" + f.cash.getUUID()
                                        + "','toCashAccount':'" + f.cash2.getUUID() + "','amount':'1'}"))
                        .entity();
        var transferEntry = (AccountTransferEntry) f.find(transfer.get("uuid").getAsString()).getTransaction()
                        .getCrossEntry();
        assertThat(transferEntry.getSourceTransaction().getSource(), is("api:t-1"));
        assertThat(transferEntry.getTargetTransaction().getSource(), is("api:t-1"));
    }

    @Test
    public void testDifferentClientRefsCreateTwoTransactions()
    {
        TransactionsHandler.create(f.context(false, "a"), json(buy("a")));
        TransactionsHandler.create(f.context(false, "b"), json(buy("b")));
        assertThat(f.legCount(), is(4));
    }

    @Test
    public void testReplayThroughRouterIs200() throws Exception
    {
        var first = f.call("POST", "/transactions", Map.of(), buy("r-1"));
        var second = f.call("POST", "/transactions", Map.of(), buy("r-1"));

        assertThat(first.status(), is(201));
        assertThat(second.status(), is(200));
        assertThat(TransactionFixture.body(second).get("replayed").getAsBoolean(), is(true));
        assertThat(TransactionFixture.body(second).get("uuid").getAsString(),
                        is(TransactionFixture.body(first).get("uuid").getAsString()));
        assertThat(f.legCount(), is(2));
    }
}
