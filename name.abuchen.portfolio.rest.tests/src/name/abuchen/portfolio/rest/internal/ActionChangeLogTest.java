package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

/** The actions (split, plan generation, price update) log what they did; dry runs log nothing */
@SuppressWarnings("nls")
public class ActionChangeLogTest
{
    private TransactionFixture f;
    private final List<IStatus> captured = new CopyOnWriteArrayList<>();
    private final ILogListener listener = (status, plugin) -> captured.add(status);
    private ILog log;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2020-01-02"), Values.Quote.factorize(10)));

        log = Platform.getLog(FrameworkUtil.getBundle(PortfolioLog.class));
        log.addLogListener(listener);
    }

    @After
    public void tearDown() throws Exception
    {
        log.removeLogListener(listener);
        f.dispose();
    }

    private List<String> messages()
    {
        return captured.stream().filter(s -> s.getMessage().startsWith("REST API")).map(IStatus::getMessage)
                        .toList();
    }

    @Test
    public void testStockSplit() throws Exception
    {
        var path = "/instruments/" + f.eurSecurity.getUUID() + "/actions/split";
        var body = "{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}";

        f.call("POST", path, Map.of("dry_run", "true"), body);
        assertThat(messages().isEmpty(), is(true));

        f.call("POST", path, Map.of(), body);
        assertThat(messages().size(), is(1));
        assertThat(messages().get(0), containsString("2:1"));
        assertThat(messages().get(0), containsString("EUR Share"));
        assertThat(messages().get(0), containsString("\"tx\""));
    }

    @Test
    public void testPlanGeneration() throws Exception
    {
        InvestmentPlansHandler.create(f.context(false, null), new IdempotencyIndex(),
                        json("{'name':'Savings','kind':'deposit','cashAccount':'" + f.cash.getUUID() + "','start':'"
                                        + LocalDate.now().minusDays(10) + "','intervalMonths':1,'amount':'100'}"));
        captured.clear();

        f.call("POST", "/investment-plans/Savings/actions/generate", Map.of("dry_run", "true"), null);
        assertThat(messages().isEmpty(), is(true));

        f.call("POST", "/investment-plans/Savings/actions/generate", Map.of(), null);
        assertThat(messages().size(), is(1));
        assertThat(messages().get(0), containsString("Savings"));
        assertThat(messages().get(0), containsString("\"tx\""));
    }

    @Test
    public void testPriceUpdate() throws Exception
    {
        f.host().onPriceUpdate(update -> update.complete(Status.OK_STATUS));

        f.call("POST", "/actions/update-quotes", Map.of("dry_run", "true"), null);
        assertThat(messages().isEmpty(), is(true));

        f.call("POST", "/actions/update-quotes", Map.of(), null);
        assertThat(messages().size(), is(1));
        assertThat(messages().get(0), containsString("\"tx\""));
    }
}
