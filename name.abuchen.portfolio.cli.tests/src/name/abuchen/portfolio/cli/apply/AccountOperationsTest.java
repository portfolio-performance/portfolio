package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

public class AccountOperationsTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    @Before
    public void setUp() throws IOException
    {
        cli.use(ClientFiles.xml(cli.clientFolder()));
    }

    private static Account cashAccount(Client client, String name)
    {
        return client.getAccounts().stream().filter(a -> name.equals(a.getName())).findFirst().orElseThrow();
    }

    private static Portfolio investmentAccount(Client client, String name)
    {
        return client.getPortfolios().stream().filter(p -> name.equals(p.getName())).findFirst().orElseThrow();
    }

    private static String cashAdd(String fields)
    {
        return "{\"operation\":\"cash-account.add\"," + fields + "}";
    }

    private static String cashUpdate(String uuid, String fields)
    {
        return "{\"operation\":\"cash-account.update\",\"uuid\":\"" + uuid + "\"," + fields + "}";
    }

    private static String investmentAdd(String fields)
    {
        return "{\"operation\":\"investment-account.add\"," + fields + "}";
    }

    private static String investmentUpdate(String uuid, String fields)
    {
        return "{\"operation\":\"investment-account.update\",\"uuid\":\"" + uuid + "\"," + fields + "}";
    }

    /** The cash account that has transactions. */
    private Account usedCashAccount() throws IOException
    {
        return cli.reload().getAccounts().get(0);
    }

    @Test
    public void addsACashAccountAndReportsTheCreatedRecord() throws IOException
    {
        var run = cli.apply(
                        cashAdd("\"name\":\"Savings\",\"currencyCode\":\"USD\",\"note\":\"rainy day\",\"retired\":true"));

        assertThat(run.stdout, run.exitCode, is(0));

        var account = cashAccount(cli.reload(), "Savings");
        var created = run.result(0).getAsJsonArray("created").get(0).getAsJsonObject();
        assertThat(created.get("entity").getAsString(), is("cash-account"));
        assertThat(created.get("uuid").getAsString(), is(account.getUUID()));
        assertThat(created.getAsJsonObject("after").keySet(),
                        containsInAnyOrder("name", "currencyCode", "note", "retired"));
        assertThat(created.getAsJsonObject("after").get("currencyCode").getAsString(), is("USD"));

        assertThat(account.getCurrencyCode(), is("USD"));
        assertThat(account.getNote(), is("rainy day"));
        assertThat(account.isRetired(), is(true));
    }

    @Test
    public void updatesOnlyThePresentFieldsOfACashAccountAndReportsBeforeAndAfter() throws IOException
    {
        var uuid = usedCashAccount().getUUID();
        cli.apply(cashUpdate(uuid, "\"name\":\"Old\",\"note\":\"first\""));

        var run = cli.apply(cashUpdate(uuid, "\"name\":\"Renamed\",\"note\":null,\"retired\":true"));

        assertThat(run.stdout, run.exitCode, is(0));

        var updated = run.result(0).getAsJsonArray("updated").get(0).getAsJsonObject();
        assertThat(updated.get("entity").getAsString(), is("cash-account"));
        assertThat(updated.get("uuid").getAsString(), is(uuid));
        assertThat(updated.getAsJsonObject("before").keySet(), containsInAnyOrder("name", "note", "retired"));
        assertThat(updated.getAsJsonObject("before").get("name").getAsString(), is("Old"));
        assertThat(updated.getAsJsonObject("after").get("name").getAsString(), is("Renamed"));
        assertThat(updated.getAsJsonObject("after").get("note").isJsonNull(), is(true));
        assertThat(updated.getAsJsonObject("after").get("retired").getAsBoolean(), is(true));

        var account = cli.reload().getAccounts().get(0);
        assertThat(account.getName(), is("Renamed"));
        assertThat(account.getNote() == null, is(true));
        assertThat(account.isRetired(), is(true));
    }

    @Test
    public void refusesToUpdateAnUnknownCashAccountOrOneAddedInTheSameBatch() throws IOException
    {
        var run = cli.apply(cashAdd("\"name\":\"New\",\"currencyCode\":\"EUR\""),
                        cashUpdate("00000000-0000-0000-0000-000000000000", "\"note\":\"x\""),
                        "{\"operation\":\"cash-account.update\",\"note\":\"x\"}");

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(), containsInAnyOrder("1:uuid:reference-not-found", "2:uuid:required"));
    }

    @Test
    public void addsAnInvestmentAccountAndReportsTheCreatedRecord() throws IOException
    {
        var reference = usedCashAccount();

        var run = cli.apply(investmentAdd("\"name\":\"Depot\",\"referenceCashAccount\":\"" + reference.getUUID()
                        + "\",\"note\":\"long term\",\"retired\":true"));

        assertThat(run.stdout, run.exitCode, is(0));

        var depot = investmentAccount(cli.reload(), "Depot");
        var created = run.result(0).getAsJsonArray("created").get(0).getAsJsonObject();
        assertThat(created.get("entity").getAsString(), is("investment-account"));
        assertThat(created.get("uuid").getAsString(), is(depot.getUUID()));
        assertThat(created.getAsJsonObject("after").keySet(),
                        containsInAnyOrder("name", "referenceCashAccount", "note", "retired"));
        assertThat(created.getAsJsonObject("after").get("referenceCashAccount").getAsString(),
                        is(reference.getUUID()));

        assertThat(depot.getReferenceAccount().getUUID(), is(reference.getUUID()));
        assertThat(depot.getNote(), is("long term"));
        assertThat(depot.isRetired(), is(true));
    }

    @Test
    public void updatesOnlyThePresentFieldsOfAnInvestmentAccountAndReportsBeforeAndAfter() throws IOException
    {
        var depot = cli.reload().getPortfolios().get(0);
        var oldReference = depot.getReferenceAccount().getUUID();
        cli.apply(cashAdd("\"name\":\"Second\",\"currencyCode\":\"EUR\""));
        var second = cashAccount(cli.reload(), "Second").getUUID();
        cli.apply(investmentUpdate(depot.getUUID(), "\"name\":\"Old\",\"note\":\"first\""));

        var run = cli.apply(investmentUpdate(depot.getUUID(), "\"name\":\"Renamed\",\"note\":null,\"retired\":true,"
                        + "\"referenceCashAccount\":\"" + second + "\""));

        assertThat(run.stdout, run.exitCode, is(0));

        var updated = run.result(0).getAsJsonArray("updated").get(0).getAsJsonObject();
        assertThat(updated.get("entity").getAsString(), is("investment-account"));
        assertThat(updated.get("uuid").getAsString(), is(depot.getUUID()));
        assertThat(updated.getAsJsonObject("before").keySet(),
                        containsInAnyOrder("name", "note", "retired", "referenceCashAccount"));
        assertThat(updated.getAsJsonObject("before").get("referenceCashAccount").getAsString(), is(oldReference));
        assertThat(updated.getAsJsonObject("after").get("referenceCashAccount").getAsString(), is(second));
        assertThat(updated.getAsJsonObject("after").get("note").isJsonNull(), is(true));

        var reloaded = cli.reload().getPortfolios().get(0);
        assertThat(reloaded.getName(), is("Renamed"));
        assertThat(reloaded.getNote() == null, is(true));
        assertThat(reloaded.isRetired(), is(true));
        assertThat(reloaded.getReferenceAccount().getUUID(), is(second));
    }

    @Test
    public void refusesBadInvestmentAccountUpdates() throws IOException
    {
        var depot = cli.reload().getPortfolios().get(0);
        var cash = usedCashAccount();

        var run = cli.apply(
                        investmentUpdate(depot.getUUID(),
                                        "\"name\":null,\"referenceCashAccount\":null"),
                        investmentUpdate(depot.getUUID(),
                                        "\"referenceCashAccount\":\"00000000-0000-0000-0000-000000000000\""),
                        investmentUpdate(cash.getUUID(), "\"note\":\"a cash account is not a depot\""),
                        "{\"operation\":\"investment-account.update\",\"note\":\"x\",\"colour\":\"red\"}");

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(),
                        containsInAnyOrder("0:name:required", "0:referenceCashAccount:required",
                                        "1:uuid:duplicate-target", "1:referenceCashAccount:reference-not-found",
                                        "2:uuid:reference-not-found", "3:uuid:required", "3:colour:unknown-field"));
    }
}
