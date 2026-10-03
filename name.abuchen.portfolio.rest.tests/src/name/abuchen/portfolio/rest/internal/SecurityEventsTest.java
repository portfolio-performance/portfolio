package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.SecurityEvent;

/** {@code GET}, {@code POST} and {@code DELETE /instruments/{uuid}/events} */
@SuppressWarnings("nls")
public class SecurityEventsTest
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

    private String uuid()
    {
        return f.eurSecurity.getUUID();
    }

    private MasterDataWrites.WriteResult add(boolean dryRun, String body)
    {
        return SecurityEventsHandler.create(f.context(dryRun, null), idempotency, uuid(), json(body));
    }

    private ApiException addFails(String body)
    {
        try
        {
            add(false, body);
            Assert.fail("expected ApiException for " + body);
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    @Test
    public void testAddStockSplitRecordsTheEventOnly()
    {
        var result = add(false, "{'type':'stock-split','date':'2026-06-10','details':'4:1'}");

        assertThat(result.changed(), is(true));
        assertThat(result.entity().get("type").getAsString(), is("stock-split"));
        var event = f.eurSecurity.getEvents().get(0);
        assertThat(event.getType(), is(SecurityEvent.Type.STOCK_SPLIT));
        assertThat(event.getDate(), is(LocalDate.parse("2026-06-10")));
        assertThat(event.getDetails(), is("4:1"));
        assertThat(f.file.isDirty(), is(true));

        var instrument = SecuritiesHandler.get(f.client, uuid()).getAsJsonObject();
        assertThat(instrument.getAsJsonArray("events").get(0).getAsJsonObject().get("details").getAsString(),
                        is("4:1"));
        var list = SecurityEventsHandler.list(f.client, uuid()).getAsJsonObject().getAsJsonArray("items");
        assertThat(list.size(), is(1));
    }

    @Test
    public void testAddNote()
    {
        add(false, "{'type':'note','date':'2026-06-10','details':'AGM'}");

        assertThat(f.eurSecurity.getEvents().get(0).getType(), is(SecurityEvent.Type.NOTE));
    }

    @Test
    public void testInvalidEventsAreRejected()
    {
        assertError(addFails("{'type':'stock-split','date':'2026-06-10','details':'four for one'}"), "details",
                        "invalid-value");
        assertError(addFails("{'type':'stock-split','date':'2026-06-10','details':'1:1'}"), "details",
                        "invalid-value");
        assertError(addFails("{'type':'stock-split','date':'2026-06-10','details':'0:1'}"), "details",
                        "invalid-value");
        assertError(addFails("{'type':'dividend-payment','date':'2026-06-10','details':'x'}"), "type",
                        "invalid-value");

        var e = addFails("{'type':'note','foo':1}");
        assertError(e, "date", "required");
        assertError(e, "details", "required");
        assertError(e, "foo", "unknown-field");

        assertThat(f.eurSecurity.getEvents().isEmpty(), is(true));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDryRunAddsNothing()
    {
        var result = add(true, "{'type':'note','date':'2026-06-10','details':'x'}");

        assertThat(result.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(f.eurSecurity.getEvents().isEmpty(), is(true));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDeleteByDateTypeAndDetails()
    {
        f.eurSecurity.addEvent(new SecurityEvent(LocalDate.parse("2026-06-10"), SecurityEvent.Type.NOTE, "a"));
        f.eurSecurity.addEvent(new SecurityEvent(LocalDate.parse("2026-06-10"), SecurityEvent.Type.NOTE, "b"));
        f.eurSecurity.addEvent(new SecurityEvent(LocalDate.parse("2026-06-10"), SecurityEvent.Type.STOCK_SPLIT, "2:1"));

        var removed = SecurityEventsHandler.delete(f.context(false, null), uuid(), "2026-06-10", "note", "a");
        assertThat(removed.getAsJsonArray("removed").size(), is(1));
        assertThat(f.eurSecurity.getEvents().size(), is(2));
        assertThat(f.file.isDirty(), is(true));

        SecurityEventsHandler.delete(f.context(false, null), uuid(), "2026-06-10", "stock-split", null);
        assertThat(f.eurSecurity.getEvents().size(), is(1));
        assertThat(f.eurSecurity.getEvents().get(0).getDetails(), is("b"));
    }

    @Test
    public void testDryRunDeleteAndNoMatch()
    {
        f.eurSecurity.addEvent(new SecurityEvent(LocalDate.parse("2026-06-10"), SecurityEvent.Type.NOTE, "a"));

        var preview = SecurityEventsHandler.delete(f.context(true, null), uuid(), "2026-06-10", "note", null);
        assertThat(preview.get("dryRun").getAsBoolean(), is(true));
        assertThat(preview.getAsJsonArray("removed").size(), is(1));
        assertThat(f.eurSecurity.getEvents().size(), is(1));
        assertThat(f.file.isDirty(), is(false));

        try
        {
            SecurityEventsHandler.delete(f.context(false, null), uuid(), "2026-06-11", "note", null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }

        try
        {
            SecurityEventsHandler.delete(f.context(false, null), uuid(), null, "split", null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().size(), is(2));
        }
    }

    @Test
    public void testThroughRouterWithReplay() throws Exception
    {
        var body = "{'type':'note','date':'2026-06-10','details':'x','clientRef':'ev-1'}";
        var first = f.call("POST", "/instruments/" + uuid() + "/events", Map.of(), body);
        var second = f.call("POST", "/instruments/" + uuid() + "/events", Map.of(), body);

        assertThat(first.status(), is(201));
        assertThat(second.status(), is(200));
        assertThat(body(second).get("replayed").getAsBoolean(), is(true));
        assertThat(f.eurSecurity.getEvents().size(), is(1));

        var list = f.call("GET", "/instruments/" + uuid() + "/events", Map.of(), null);
        assertThat(body(list).getAsJsonArray("items").size(), is(1));

        var delete = f.call("DELETE", "/instruments/" + uuid() + "/events",
                        Map.of("date", "2026-06-10", "type", "note"), null);
        assertThat(delete.status(), is(200));
        assertThat(f.eurSecurity.getEvents().isEmpty(), is(true));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
