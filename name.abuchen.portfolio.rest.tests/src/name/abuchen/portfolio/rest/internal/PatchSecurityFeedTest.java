package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeed;

/** The quote feed fields, the target currency and the dry run of {@code PATCH /instruments/{uuid}}. */
@SuppressWarnings("nls")
public class PatchSecurityFeedTest
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

    private SecuritiesHandler.PatchResult patch(String body)
    {
        return SecuritiesHandler.patch(f.client, f.eurSecurity.getUUID(), json(body));
    }

    private ApiException patchFails(String body)
    {
        try
        {
            patch(body);
            Assert.fail("expected ApiException for " + body);
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    @Test
    public void testFeedFieldsAreWrittenAndExposed()
    {
        var result = patch("{'feed':'GENERIC-JSON','feedUrl':'https://example.com/{TICKER}','latestFeed':'MANUAL',"
                        + "'latestFeedUrl':'https://example.com/latest','calendar':'TARGET2',"
                        + "'feedProperties':{'GENERIC-JSON-DATE':'$.d','GENERIC-JSON-CLOSE':'$.c'}}");

        var security = f.eurSecurity;
        assertThat(security.getFeed(), is("GENERIC-JSON"));
        assertThat(security.getFeedURL(), is("https://example.com/{TICKER}"));
        assertThat(security.getLatestFeed(), is(QuoteFeed.MANUAL));
        assertThat(security.getLatestFeedURL(), is("https://example.com/latest"));
        assertThat(security.getCalendar(), is("TARGET2"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.FEED, "GENERIC-JSON-CLOSE"), is(Optional.of("$.c")));

        var json = result.entity().getAsJsonObject();
        assertThat(json.get("feed").getAsString(), is("GENERIC-JSON"));
        assertThat(json.get("feedUrl").getAsString(), is("https://example.com/{TICKER}"));
        assertThat(json.get("latestFeed").getAsString(), is("MANUAL"));
        assertThat(json.get("latestFeedUrl").getAsString(), is("https://example.com/latest"));
        assertThat(json.get("calendar").getAsString(), is("TARGET2"));
        assertThat(json.getAsJsonObject("feedProperties").get("GENERIC-JSON-DATE").getAsString(), is("$.d"));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testNullClearsFeedFieldsAndRemovesFeedProperties()
    {
        patch("{'latestFeed':'MANUAL','calendar':'TARGET2','feedProperties':{'A':'1','B':'2'}}");

        var result = patch("{'latestFeed':null,'calendar':null,'feedProperties':{'A':null}}");

        assertThat(f.eurSecurity.getLatestFeed(), is(nullValue()));
        assertThat(f.eurSecurity.getCalendar(), is(nullValue()));
        assertThat(f.eurSecurity.getPropertyValue(SecurityProperty.Type.FEED, "A"), is(Optional.empty()));
        assertThat(f.eurSecurity.getPropertyValue(SecurityProperty.Type.FEED, "B"), is(Optional.of("2")));
        assertThat(result.changes().size(), is(3));
    }

    @Test
    public void testUnknownFeedAndCalendarAreRejected()
    {
        var e = patchFails("{'feed':'NOPE','latestFeed':'ALSO-NOPE','calendar':'mars','feedProperties':{'x':1}}");

        assertError(e, "feed", "unknown-feed");
        assertError(e, "latestFeed", "unknown-feed");
        assertError(e, "calendar", "unknown-calendar");
        assertError(e, "feedProperties.x", "invalid-type");
        assertThat(f.eurSecurity.getFeed(), is(nullValue()));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testTargetCurrencyOnlyForExchangeRates()
    {
        assertError(patchFails("{'targetCurrencyCode':'USD'}"), "targetCurrencyCode", "not-allowed-for-type");

        f.eurSecurity.setTargetCurrencyCode("USD");
        patch("{'targetCurrencyCode':'CHF'}");
        assertThat(f.eurSecurity.getTargetCurrencyCode(), is("CHF"));

        assertError(patchFails("{'targetCurrencyCode':null}"), "targetCurrencyCode",
                        "exchange-rate-requires-currency");
        assertError(patchFails("{'targetCurrencyCode':'XYZ'}"), "targetCurrencyCode", "unknown-currency");
    }

    @Test
    public void testDryRunChangesNothing()
    {
        var result = SecuritiesHandler.patch(f.client, f.eurSecurity.getUUID(),
                        json("{'name':'Renamed','feed':'MANUAL','feedProperties':{'A':'1'}}"), true);

        var json = result.entity().getAsJsonObject();
        assertThat(json.get("dryRun").getAsBoolean(), is(true));
        assertThat(json.get("uuid").getAsString(), is(f.eurSecurity.getUUID()));
        assertThat(json.get("name").getAsString(), is("Renamed"));
        assertThat(json.getAsJsonObject("feedProperties").get("A").getAsString(), is("1"));

        assertThat(f.eurSecurity.getName(), is("EUR Share"));
        assertThat(f.eurSecurity.getFeed(), is(nullValue()));
        assertThat(f.eurSecurity.getPropertyValue(SecurityProperty.Type.FEED, "A"), is(Optional.empty()));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatchWithCurrentValuesDoesNotMarkDirty()
    {
        var result = patch("{'name':'EUR Share','currencyCode':'EUR','feed':null}");

        assertThat(result.changes().isEmpty(), is(true));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDryRunThroughRouter() throws Exception
    {
        var response = f.call("PATCH", "/instruments/" + f.eurSecurity.getUUID(), Map.of("dry_run", "true"),
                        "{'note':'preview'}");

        assertThat(response.status(), is(200));
        assertThat(body(response).get("note").getAsString(), is("preview"));
        assertThat(body(response).get("dryRun").getAsBoolean(), is(true));
        assertThat(f.eurSecurity.getNote(), is(nullValue()));
        assertThat(f.file.isDirty(), is(false));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testDryRunDeleteThroughRouter() throws Exception
    {
        var security = f.eurSecurity;
        var count = f.client.getSecurities().size();

        var response = f.call("DELETE", "/instruments/" + security.getUUID(), Map.of("dry_run", "true"), null);

        assertThat(response.status(), is(200));
        assertThat(body(response).getAsJsonArray("removed").get(0).getAsJsonObject().get("uuid").getAsString(),
                        is(security.getUUID()));
        assertThat(f.client.getSecurities().size(), is(count));
        assertThat(f.file.isDirty(), is(false));

        // a real delete answers 204
        assertThat(f.call("DELETE", "/instruments/" + security.getUUID(), Map.of(), null).status(), is(204));
        assertThat(f.client.getSecurities().size(), is(count - 1));
    }

    @Test
    public void testDryRunDeleteOfReferencedInstrumentIsBlocked() throws Exception
    {
        f.create("{'type':'buy','date':'2026-03-02','investmentAccount':'" + f.broker.getUUID() + "','cashAccount':'"
                        + f.cash.getUUID() + "','instrument':'" + f.eurSecurity.getUUID()
                        + "','shares':'1','quote':'10'}");

        try
        {
            f.call("DELETE", "/instruments/" + f.eurSecurity.getUUID(), Map.of("dry_run", "true"), null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(409));
            assertThat(e.getType(), is("delete-blocked"));
        }
    }
}
