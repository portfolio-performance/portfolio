package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** {@code retired} on instruments, cash accounts and investment accounts */
@SuppressWarnings("nls")
public class RetiredFlagTest
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

    @Test
    public void testRetireAnInstrumentThroughRouter() throws Exception
    {
        var dry = f.call("PATCH", "/instruments/" + f.eurSecurity.getUUID(), Map.of("dry_run", "true"),
                        "{'retired':true}");
        assertThat(body(dry).get("retired").getAsBoolean(), is(true));
        assertThat(f.eurSecurity.isRetired(), is(false));
        assertThat(f.file.isDirty(), is(false));

        var real = f.call("PATCH", "/instruments/" + f.eurSecurity.getUUID(), Map.of(), "{'retired':true}");
        assertThat(body(real).get("retired").getAsBoolean(), is(true));
        assertThat(f.eurSecurity.isRetired(), is(true));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCreateARetiredInstrument()
    {
        var entity = SecuritiesHandler.create(f.context(false, null), new IdempotencyIndex(),
                        json("{'name':'Old','retired':true}")).entity();

        assertThat(entity.get("retired").getAsBoolean(), is(true));
    }

    @Test
    public void testRetireAccounts()
    {
        AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(), json("{'retired':true}"));
        assertThat(f.usdCash.isRetired(), is(true));

        var result = PortfoliosHandler.patch(f.context(false, null), f.broker2.getUUID(), json("{'retired':true}"));
        assertThat(f.broker2.isRetired(), is(true));
        assertThat(result.entity().get("retired").getAsBoolean(), is(true));

        var created = AccountsHandler.create(f.context(false, null), new IdempotencyIndex(),
                        json("{'name':'Closed','retired':true}")).entity();
        assertThat(created.get("retired").getAsBoolean(), is(true));
    }

    @Test
    public void testRetiredMustBeABoolean()
    {
        for (var body : new String[] { "{'retired':null}", "{'retired':'yes'}" })
        {
            try
            {
                AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(), json(body));
                Assert.fail("expected ApiException for " + body);
            }
            catch (ApiException e)
            {
                assertError(e, "retired", "invalid-type");
            }
        }
        assertThat(f.usdCash.isRetired(), is(false));
    }
}
