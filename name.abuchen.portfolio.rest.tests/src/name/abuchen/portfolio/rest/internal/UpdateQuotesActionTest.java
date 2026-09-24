package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Status;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.spi.PriceUpdateTarget;

@SuppressWarnings("nls")
public class UpdateQuotesActionTest
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

    /** the fake price feed: a new latest price for the EUR share, a historical one for the USD share */
    private void updatePrices()
    {
        f.eurSecurity.setLatest(new LatestSecurityPrice(LocalDate.now(), Values.Quote.factorize(12)));
        f.usdSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-01-02"), Values.Quote.factorize(7)));
        f.client.markDirty();
    }

    private ApiException fails(String body)
    {
        try
        {
            f.call("POST", "/actions/update-quotes", Map.of(), body);
            Assert.fail("expected ApiException");
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
        catch (Exception e)
        {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testStartsAJobAndReportsItsCompletion() throws Exception
    {
        var response = f.call("POST", "/actions/update-quotes", Map.of(), null);

        assertThat(response.status(), is(202));
        var job = body(response);
        assertThat(job.get("status").getAsString(), is("running"));
        var jobId = job.get("jobId").getAsString();
        assertThat(response.headers().get("Location"), is("/v1/files/" + f.fileId() + "/jobs/" + jobId));

        var update = f.host().priceUpdates().get(0);
        assertThat(update.securities().size(), is(2));
        assertThat(update.targets(), is(EnumSet.allOf(PriceUpdateTarget.class)));
        assertThat(update.file(), is(f.file));

        var polled = body(f.call("GET", "/jobs/" + jobId, Map.of(), null));
        assertThat(polled.get("status").getAsString(), is("running"));

        updatePrices();
        update.complete(Status.OK_STATUS);

        polled = body(f.call("GET", "/jobs/" + jobId, Map.of(), null));
        assertThat(polled.get("status").getAsString(), is("done"));
        var summary = polled.getAsJsonObject("summary");
        assertThat(summary.get("instruments").getAsInt(), is(2));
        assertThat(summary.get("latestUpdated").getAsInt(), is(1));
        assertThat(summary.get("historicUpdated").getAsInt(), is(1));
        assertThat(summary.get("dirty").getAsBoolean(), is(true));

        var updated = summary.getAsJsonArray("updated");
        assertThat(updated.get(0).getAsJsonObject().get("uuid").getAsString(), is(f.eurSecurity.getUUID()));
        assertThat(updated.get(0).getAsJsonObject().get("latest").getAsBoolean(), is(true));
        assertThat(updated.get(1).getAsJsonObject().get("historicPricesAdded").getAsInt(), is(1));

        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testWaitAnswersTheFinishedJob() throws Exception
    {
        f.host().onPriceUpdate(update -> new Thread(() -> {
            updatePrices();
            update.complete(Status.OK_STATUS);
        }).start());

        var response = f.call("POST", "/actions/update-quotes", Map.of("wait", "30"),
                        "{'instruments':['" + f.eurSecurity.getUUID() + "'],'targets':['latest']}");

        assertThat(response.status(), is(200));
        var job = body(response);
        assertThat(job.get("status").getAsString(), is("done"));
        assertThat(job.getAsJsonObject("summary").get("instruments").getAsInt(), is(1));

        var update = f.host().priceUpdates().get(0);
        assertThat(update.securities(), is(List.of(f.eurSecurity)));
        assertThat(update.targets(), is(Set.of(PriceUpdateTarget.LATEST)));
    }

    @Test
    public void testFailedUpdate() throws Exception
    {
        f.host().onPriceUpdate(update -> update.complete(Status.CANCEL_STATUS));

        var response = f.call("POST", "/actions/update-quotes", Map.of("wait", "1"), "{}");
        var job = body(response);
        assertThat(job.get("status").getAsString(), is("failed"));
        assertThat(job.get("error").getAsString(), is("cancelled"));
    }

    @Test
    public void testDryRunStartsNothing() throws Exception
    {
        var response = f.call("POST", "/actions/update-quotes", Map.of("dry_run", "true"),
                        "{'instruments':['" + f.usdSecurity.getUUID() + "']}");
        assertThat(response.status(), is(200));
        var json = body(response);
        assertThat(json.get("dryRun").getAsBoolean(), is(true));
        assertThat(json.getAsJsonArray("instruments").size(), is(1));
        assertThat(json.getAsJsonArray("targets").size(), is(2));
        assertThat(f.host().priceUpdates().isEmpty(), is(true));
    }

    @Test
    public void testValidation() throws Exception
    {
        var e = fails("{'instruments':['nope', 1],'targets':['sometimes'],'foo':true}");
        assertError(e, "instruments[0]", "unknown-reference");
        assertError(e, "instruments[1]", "invalid-type");
        assertError(e, "targets", "invalid-value");
        assertError(e, "foo", "unknown-field");

        e = fails("{'instruments':[]}");
        assertError(e, "instruments", "invalid-value");
        assertThat(f.host().priceUpdates().isEmpty(), is(true));

        try
        {
            f.call("POST", "/actions/update-quotes", Map.of("wait", "61"), null);
            Assert.fail();
        }
        catch (ApiException ex)
        {
            assertThat(ex.getStatus(), is(400));
        }
    }

    @Test
    public void testLockedWhileTheUserEdits() throws Exception
    {
        f.host().setUserEditing(true);
        try
        {
            f.call("POST", "/actions/update-quotes", Map.of(), null);
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
        }
        assertThat(f.host().priceUpdates().isEmpty(), is(true));
    }

    @Test
    public void testUnknownJobIs404() throws Exception
    {
        try
        {
            f.call("GET", "/jobs/nope", Map.of(), null);
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }
}
