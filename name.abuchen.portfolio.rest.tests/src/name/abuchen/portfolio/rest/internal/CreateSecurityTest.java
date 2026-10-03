package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;

/** {@code POST /instruments} */
@SuppressWarnings("nls")
public class CreateSecurityTest
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

    private MasterDataWrites.WriteResult create(boolean dryRun, String body)
    {
        return SecuritiesHandler.create(f.context(dryRun, null), idempotency, json(body));
    }

    private ApiException createFails(String body)
    {
        try
        {
            create(false, body);
            Assert.fail("expected ApiException for " + body);
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    private Security find(String uuid)
    {
        return f.client.getSecurities().stream().filter(s -> s.getUUID().equals(uuid)).findFirst().orElse(null);
    }

    @Test
    public void testCreateAddsTheInstrumentAndMarksTheFileDirty()
    {
        var result = create(false, "{'name':'Apple Inc.','currencyCode':'USD','isin':'US0378331005',"
                        + "'tickerSymbol':'AAPL','feed':'YAHOO','note':'core'}");

        assertThat(result.changed(), is(true));
        var security = find(result.entity().get("uuid").getAsString());
        assertThat(security.getName(), is("Apple Inc."));
        assertThat(security.getCurrencyCode(), is("USD"));
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getTickerSymbol(), is("AAPL"));
        assertThat(security.getFeed(), is("YAHOO"));
        assertThat(security.getNote(), is("core"));
        assertThat(f.file.isDirty(), is(true));
        assertThat(result.entity().get("feed").getAsString(), is("YAHOO"));
    }

    @Test
    public void testDefaultsFollowTheApplication()
    {
        var security = find(create(false, "{'name':'Plain'}").entity().get("uuid").getAsString());

        assertThat(security.getCurrencyCode(), is(f.client.getBaseCurrency()));
        assertThat(security.getFeed(), is(QuoteFeed.MANUAL));
        assertThat(security.isExchangeRate(), is(false));
    }

    @Test
    public void testExplicitNullCurrencyCreatesAnIndex()
    {
        var entity = create(false, "{'name':'DAX','currencyCode':null}").entity();

        assertThat(find(entity.get("uuid").getAsString()).getCurrencyCode(), is(nullValue()));
        assertThat(entity.get("currencyCode").isJsonNull(), is(true));
    }

    @Test
    public void testTargetCurrencyCreatesAnExchangeRate()
    {
        var entity = create(false, "{'name':'EUR/USD','currencyCode':'EUR','targetCurrencyCode':'USD'}").entity();

        var security = find(entity.get("uuid").getAsString());
        assertThat(security.isExchangeRate(), is(true));
        assertThat(security.getTargetCurrencyCode(), is("USD"));
        assertThat(entity.get("targetCurrencyCode").getAsString(), is("USD"));

        // an exchange rate needs its currency
        assertError(createFails("{'name':'X','currencyCode':null,'targetCurrencyCode':'USD'}"), "currencyCode",
                        "exchange-rate-requires-currency");
    }

    @Test
    public void testAttributesAndFeedPropertiesAreSet()
    {
        var entity = create(false, "{'name':'JSON','feed':'GENERIC-JSON','feedUrl':'https://example.com/x',"
                        + "'feedProperties':{'GENERIC-JSON-DATE':'$.date'}}").entity();

        var security = find(entity.get("uuid").getAsString());
        assertThat(security.getFeedURL(), is("https://example.com/x"));
        assertThat(entity.getAsJsonObject("feedProperties").get("GENERIC-JSON-DATE").getAsString(), is("$.date"));
    }

    @Test
    public void testAllViolationsAreReportedAndNothingIsCreated()
    {
        var count = f.client.getSecurities().size();

        var e = createFails("{'currencyCode':'XYZ','feed':'NO-SUCH-FEED','calendar':'nowhere','colour':'red'}");

        assertError(e, "name", "required");
        assertError(e, "currencyCode", "unknown-currency");
        assertError(e, "feed", "unknown-feed");
        assertError(e, "calendar", "unknown-calendar");
        assertError(e, "colour", "unknown-field");
        assertThat(f.client.getSecurities().size(), is(count));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testBlankNameIsRejected()
    {
        assertError(createFails("{'name':' '}"), "name", "required");
    }

    @Test
    public void testDryRunCreatesNothing()
    {
        var count = f.client.getSecurities().size();

        var result = create(true, "{'name':'Preview','currencyCode':'USD'}");

        assertThat(result.changed(), is(false));
        assertThat(result.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(result.entity().get("name").getAsString(), is("Preview"));
        assertThat(f.client.getSecurities().size(), is(count));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testThroughRouterIs201WithLocationAndOnTheUIThread() throws Exception
    {
        var response = f.call("POST", "/instruments", Map.of(), "{'name':'Routed'}");

        assertThat(response.status(), is(201));
        var uuid = body(response).get("uuid").getAsString();
        assertThat(response.headers().get("Location"), is("/v1/files/" + f.fileId() + "/instruments/" + uuid));
        assertThat(find(uuid).getName(), is("Routed"));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testSameClientRefThroughRouterReplays() throws Exception
    {
        var count = f.client.getSecurities().size();

        var first = f.call("POST", "/instruments", Map.of(), "{'name':'Once','clientRef':'sec-1'}");
        f.file.setDirty(false);
        var second = f.call("POST", "/instruments", Map.of(), "{'name':'Twice','clientRef':'sec-1'}");

        assertThat(first.status(), is(201));
        assertThat(second.status(), is(200));
        assertThat(body(second).get("replayed").getAsBoolean(), is(true));
        assertThat(body(second).get("uuid").getAsString(), is(body(first).get("uuid").getAsString()));
        assertThat(f.client.getSecurities().size(), is(count + 1));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testReplayOfADeletedInstrumentCreatesAgain() throws Exception
    {
        var first = body(f.call("POST", "/instruments", Map.of(), "{'name':'Gone','clientRef':'sec-2'}"));
        SecuritiesHandler.delete(f.client, first.get("uuid").getAsString());

        var second = f.call("POST", "/instruments", Map.of(), "{'name':'Back','clientRef':'sec-2'}");

        assertThat(second.status(), is(201));
        assertThat(body(second).get("name").getAsString(), is("Back"));
    }

    @Test
    public void testWriteIsLockedWhileTheUserEdits() throws Exception
    {
        f.host().setUserEditing(true);
        try
        {
            f.call("POST", "/instruments", Map.of(), "{'name':'Locked'}");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
        }
    }
}
