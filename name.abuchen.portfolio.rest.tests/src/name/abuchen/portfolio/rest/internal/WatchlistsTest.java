package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;

/** Watchlists, addressed by name */
@SuppressWarnings("nls")
public class WatchlistsTest
{
    private TransactionFixture f;
    private IdempotencyIndex idempotency;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
        idempotency = new IdempotencyIndex();
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private Watchlist watchlist(String name)
    {
        return f.client.getWatchlists().stream().filter(w -> w.getName().equals(name)).findFirst().orElse(null);
    }

    private Watchlist add(String name, Security... securities)
    {
        var watchlist = new Watchlist();
        watchlist.setName(name);
        for (var security : securities)
            watchlist.addSecurity(security);
        f.client.addWatchlist(watchlist);
        f.file.setDirty(false);
        return watchlist;
    }

    private static ApiException fails(Runnable runnable)
    {
        try
        {
            runnable.run();
            Assert.fail("expected ApiException");
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    @Test
    public void testCreate()
    {
        var result = WatchlistsHandler.create(f.context(false, null), idempotency, json("{'name':'Tech','instruments':['"
                        + f.usdSecurity.getUUID() + "','" + f.eurSecurity.getUUID() + "']}"));

        assertThat(result.changed(), is(true));
        assertThat(watchlist("Tech").getSecurities(), contains(f.usdSecurity, f.eurSecurity));
        assertThat(result.entity().getAsJsonArray("instruments").get(0).getAsJsonObject().get("uuid").getAsString(),
                        is(f.usdSecurity.getUUID()));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCreateValidation()
    {
        add("Tech");

        var e = fails(() -> WatchlistsHandler.create(f.context(false, null), idempotency,
                        json("{'name':'Tech','instruments':['nope','" + f.eurSecurity.getUUID() + "','"
                                        + f.eurSecurity.getUUID() + "',1],'x':1}")));

        assertError(e, "name", "already-exists");
        assertError(e, "instruments[0]", "unknown-reference");
        assertError(e, "instruments[2]", "invalid-value");
        assertError(e, "instruments[3]", "invalid-type");
        assertError(e, "x", "unknown-field");
        assertError(fails(() -> WatchlistsHandler.create(f.context(false, null), idempotency, json("{}"))), "name",
                        "required");
        assertThat(f.client.getWatchlists().size(), is(1));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testCreateDryRun()
    {
        var result = WatchlistsHandler.create(f.context(true, null), idempotency, json("{'name':'Preview'}"));

        assertThat(result.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(f.client.getWatchlists(), is(empty()));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testRenameAndReplaceInstruments()
    {
        var watchlist = add("Old", f.eurSecurity);

        WatchlistsHandler.patch(f.context(false, null), "Old",
                        json("{'name':'New','instruments':['" + f.usdSecurity.getUUID() + "']}"));

        assertThat(watchlist.getName(), is("New"));
        assertThat(watchlist.getSecurities(), contains(f.usdSecurity));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testPatchNoOpDryRunAndConflicts()
    {
        var watchlist = add("A", f.eurSecurity);
        add("B");

        var noop = WatchlistsHandler.patch(f.context(false, null), "A", json("{'name':'A'}"));
        assertThat(noop.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));

        var dry = WatchlistsHandler.patch(f.context(true, null), "A", json("{'instruments':null}"));
        assertThat(dry.entity().getAsJsonArray("instruments").size(), is(0));
        assertThat(watchlist.getSecurities(), contains(f.eurSecurity));
        assertThat(f.file.isDirty(), is(false));

        assertError(fails(() -> WatchlistsHandler.patch(f.context(false, null), "A", json("{'name':'B'}"))), "name",
                        "already-exists");

        try
        {
            WatchlistsHandler.patch(f.context(false, null), "C", json("{}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    @Test
    public void testAmbiguousNameIs409()
    {
        add("Twin");
        add("Twin");

        try
        {
            WatchlistsHandler.get(f.client, "Twin");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(409));
            assertThat(e.getType(), is("ambiguous-name"));
        }
    }

    @Test
    public void testAddAndRemoveInstrument()
    {
        var watchlist = add("W");

        WatchlistsHandler.addInstrument(f.context(false, null), "W", f.eurSecurity.getUUID());
        assertThat(watchlist.getSecurities(), contains(f.eurSecurity));
        assertThat(f.file.isDirty(), is(true));

        f.file.setDirty(false);
        var again = WatchlistsHandler.addInstrument(f.context(false, null), "W", f.eurSecurity.getUUID());
        assertThat(again.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));

        WatchlistsHandler.removeInstrument(f.context(true, null), "W", f.eurSecurity.getUUID());
        assertThat(watchlist.getSecurities(), contains(f.eurSecurity));

        WatchlistsHandler.removeInstrument(f.context(false, null), "W", f.eurSecurity.getUUID());
        assertThat(watchlist.getSecurities(), is(empty()));
        assertThat(f.client.getSecurities().contains(f.eurSecurity), is(true));

        try
        {
            WatchlistsHandler.removeInstrument(f.context(false, null), "W", f.eurSecurity.getUUID());
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    @Test
    public void testDelete()
    {
        add("W", f.eurSecurity);

        var preview = WatchlistsHandler.delete(f.context(true, null), "W");
        assertThat(preview.get("dryRun").getAsBoolean(), is(true));
        assertThat(watchlist("W") != null, is(true));

        assertThat(WatchlistsHandler.delete(f.context(false, null), "W"), is(nullValue()));
        assertThat(watchlist("W"), is(nullValue()));
        assertThat(f.client.getSecurities().contains(f.eurSecurity), is(true));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testThroughRouterWithAnEncodedName() throws Exception
    {
        var created = f.call("POST", "/watchlists", Map.of(), "{'name':'Tech/Growth 1+1','clientRef':'w-1'}");
        assertThat(created.status(), is(201));
        assertThat(created.headers().get("Location"),
                        is("/v1/files/" + f.fileId() + "/watchlists/Tech%2FGrowth%201%2B1"));

        var replay = f.call("POST", "/watchlists", Map.of(), "{'name':'Other','clientRef':'w-1'}");
        assertThat(replay.status(), is(200));
        assertThat(body(replay).get("replayed").getAsBoolean(), is(true));

        var put = f.call("PUT", "/watchlists/Tech%2FGrowth%201+1/instruments/" + f.eurSecurity.getUUID(), Map.of(),
                        null);
        assertThat(put.status(), is(200));
        assertThat(watchlist("Tech/Growth 1+1").getSecurities(), contains(f.eurSecurity));

        var get = f.call("GET", "/watchlists/Tech%2FGrowth%201%2B1", Map.of(), null);
        assertThat(body(get).getAsJsonArray("instruments").size(), is(1));

        var list = f.call("GET", "/watchlists", Map.of(), null);
        assertThat(body(list).getAsJsonArray("items").size(), is(1));

        assertThat(f.call("DELETE", "/watchlists/Tech%2FGrowth%201%2B1", Map.of(), null).status(), is(204));
        assertThat(f.client.getWatchlists(), is(empty()));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
