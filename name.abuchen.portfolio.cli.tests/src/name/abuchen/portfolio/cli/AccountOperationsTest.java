package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;

public class AccountOperationsTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path inputFolder;
    private File file;

    @Before
    public void setUp() throws IOException
    {
        inputFolder = folder.newFolder("input").toPath();
        file = ClientFiles.xml(folder.newFolder("client").toPath());
    }

    private CliRun apply(String... operations) throws IOException
    {
        var input = Files.createTempFile(inputFolder, "batch", ".json");
        Files.writeString(input, "{\"operations\":[" + String.join(",", operations) + "]}", StandardCharsets.UTF_8);
        return CliRun.run("apply", "--file", file.getPath(), "--input", input.toString());
    }

    private Client reload() throws IOException
    {
        return ClientFactory.load(file, null, new NullProgressMonitor());
    }

    private static JsonObject result(CliRun run, int index)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("results").get(index)
                        .getAsJsonObject();
    }

    private static JsonArray failures(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonArray("failures");
    }

    private static JsonObject failure(CliRun run, int index)
    {
        return failures(run).get(index).getAsJsonObject();
    }

    private static List<String> failureKeys(CliRun run)
    {
        var keys = new ArrayList<String>();
        for (var element : failures(run))
        {
            var failure = element.getAsJsonObject();
            keys.add(failure.get("operationIndex").getAsInt() + ":" + failure.get("field").getAsString() + ":"
                            + failure.get("code").getAsString());
        }
        return keys;
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
        return reload().getAccounts().get(0);
    }

    @Test
    public void addsACashAccountAndReportsTheCreatedRecord() throws IOException
    {
        var run = apply(cashAdd("\"name\":\"Savings\",\"currencyCode\":\"USD\",\"note\":\"rainy day\",\"retired\":true"));

        assertThat(run.stdout, run.exitCode, is(0));

        var account = cashAccount(reload(), "Savings");
        var created = result(run, 0).getAsJsonArray("created").get(0).getAsJsonObject();
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
    public void refusesACashAccountWithoutNameOrCurrencyAndLeavesTheFileUntouched() throws IOException
    {
        var before = Files.readAllBytes(file.toPath());

        var run = apply(cashAdd("\"note\":\"nameless\""), cashAdd("\"name\":\"   \",\"currencyCode\":\"EUR\""),
                        cashAdd("\"name\":\"Odd\",\"currencyCode\":\"ZZZ\""));

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run), containsInAnyOrder("0:name:required", "0:currencyCode:required",
                        "1:name:required", "2:currencyCode:invalid-currency"));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
    }

    @Test
    public void updatesOnlyThePresentFieldsOfACashAccountAndReportsBeforeAndAfter() throws IOException
    {
        var uuid = usedCashAccount().getUUID();
        apply(cashUpdate(uuid, "\"name\":\"Old\",\"note\":\"first\""));

        var run = apply(cashUpdate(uuid, "\"name\":\"Renamed\",\"note\":null,\"retired\":true"));

        assertThat(run.stdout, run.exitCode, is(0));

        var updated = result(run, 0).getAsJsonArray("updated").get(0).getAsJsonObject();
        assertThat(updated.get("entity").getAsString(), is("cash-account"));
        assertThat(updated.get("uuid").getAsString(), is(uuid));
        assertThat(updated.getAsJsonObject("before").keySet(), containsInAnyOrder("name", "note", "retired"));
        assertThat(updated.getAsJsonObject("before").get("name").getAsString(), is("Old"));
        assertThat(updated.getAsJsonObject("after").get("name").getAsString(), is("Renamed"));
        assertThat(updated.getAsJsonObject("after").get("note").isJsonNull(), is(true));
        assertThat(updated.getAsJsonObject("after").get("retired").getAsBoolean(), is(true));

        var account = reload().getAccounts().get(0);
        assertThat(account.getName(), is("Renamed"));
        assertThat(account.getNote() == null, is(true));
        assertThat(account.isRetired(), is(true));
    }

    @Test
    public void refusesToUpdateAnUnknownCashAccountOrOneAddedInTheSameBatch() throws IOException
    {
        var run = apply(cashAdd("\"name\":\"New\",\"currencyCode\":\"EUR\""),
                        cashUpdate("00000000-0000-0000-0000-000000000000", "\"note\":\"x\""),
                        "{\"operation\":\"cash-account.update\",\"note\":\"x\"}");

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run), containsInAnyOrder("1:uuid:reference-not-found", "2:uuid:required"));
    }

    @Test
    public void refusesACurrencyChangeOnceTheCashAccountHasTransactions() throws IOException
    {
        var used = usedCashAccount();
        var other = used.getCurrencyCode().equals("USD") ? "GBP" : "USD";
        var before = Files.readAllBytes(file.toPath());

        var run = apply(cashUpdate(used.getUUID(), "\"currencyCode\":\"" + other + "\""));

        assertThat(run.exitCode, is(1));
        assertThat(failure(run, 0).get("field").getAsString(), is("currencyCode"));
        assertThat(failure(run, 0).get("code").getAsString(), is("currency-immutable"));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
    }

    @Test
    public void allowsRepeatingTheCurrencyOfACashAccountWithTransactions() throws IOException
    {
        var used = usedCashAccount();

        var run = apply(cashUpdate(used.getUUID(),
                        "\"currencyCode\":\"" + used.getCurrencyCode() + "\",\"note\":\"kept\""));

        assertThat(run.stdout, run.exitCode, is(0));
    }

    @Test
    public void allowsACurrencyChangeBeforeTheCashAccountHasTransactions() throws IOException
    {
        apply(cashAdd("\"name\":\"Empty\",\"currencyCode\":\"EUR\""));
        var empty = cashAccount(reload(), "Empty");

        var run = apply(cashUpdate(empty.getUUID(), "\"currencyCode\":\"CHF\""));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(cashAccount(reload(), "Empty").getCurrencyCode(), is("CHF"));
    }

    @Test
    public void refusesToClearTheNameOrCurrencyOfACashAccount() throws IOException
    {
        apply(cashAdd("\"name\":\"Empty\",\"currencyCode\":\"EUR\""));
        var empty = cashAccount(reload(), "Empty");

        var run = apply(cashUpdate(empty.getUUID(), "\"name\":null,\"currencyCode\":\"\""));

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run), containsInAnyOrder("0:name:required", "0:currencyCode:required"));
    }

    @Test
    public void refusesTwoUpdatesOfTheSameCashAccountInOneBatch() throws IOException
    {
        var uuid = usedCashAccount().getUUID();

        var run = apply(cashUpdate(uuid, "\"note\":\"one\""), cashUpdate(uuid, "\"note\":\"two\""));

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run), containsInAnyOrder("1:uuid:duplicate-target"));
    }

    @Test
    public void addsAnInvestmentAccountAndReportsTheCreatedRecord() throws IOException
    {
        var reference = usedCashAccount();

        var run = apply(investmentAdd("\"name\":\"Depot\",\"referenceCashAccount\":\"" + reference.getUUID()
                        + "\",\"note\":\"long term\",\"retired\":true"));

        assertThat(run.stdout, run.exitCode, is(0));

        var depot = investmentAccount(reload(), "Depot");
        var created = result(run, 0).getAsJsonArray("created").get(0).getAsJsonObject();
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
    public void refusesAnInvestmentAccountWithoutNameOrReferenceCashAccount() throws IOException
    {
        var before = Files.readAllBytes(file.toPath());

        var run = apply(investmentAdd("\"note\":\"bare\""));

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run), containsInAnyOrder("0:name:required", "0:referenceCashAccount:required"));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
    }

    @Test
    public void refusesAnUnknownReferenceCashAccount() throws IOException
    {
        var run = apply(investmentAdd(
                        "\"name\":\"Depot\",\"referenceCashAccount\":\"00000000-0000-0000-0000-000000000000\""));

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run), containsInAnyOrder("0:referenceCashAccount:reference-not-found"));
    }

    @Test
    public void refusesAReferenceCashAccountThatIsAnInvestmentAccount() throws IOException
    {
        var depot = reload().getPortfolios().get(0);

        var run = apply(investmentAdd("\"name\":\"Depot\",\"referenceCashAccount\":\"" + depot.getUUID() + "\""));

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run), containsInAnyOrder("0:referenceCashAccount:reference-not-found"));
    }

    @Test
    public void updatesOnlyThePresentFieldsOfAnInvestmentAccountAndReportsBeforeAndAfter() throws IOException
    {
        var depot = reload().getPortfolios().get(0);
        var oldReference = depot.getReferenceAccount().getUUID();
        apply(cashAdd("\"name\":\"Second\",\"currencyCode\":\"EUR\""));
        var second = cashAccount(reload(), "Second").getUUID();
        apply(investmentUpdate(depot.getUUID(), "\"name\":\"Old\",\"note\":\"first\""));

        var run = apply(investmentUpdate(depot.getUUID(), "\"name\":\"Renamed\",\"note\":null,\"retired\":true,"
                        + "\"referenceCashAccount\":\"" + second + "\""));

        assertThat(run.stdout, run.exitCode, is(0));

        var updated = result(run, 0).getAsJsonArray("updated").get(0).getAsJsonObject();
        assertThat(updated.get("entity").getAsString(), is("investment-account"));
        assertThat(updated.get("uuid").getAsString(), is(depot.getUUID()));
        assertThat(updated.getAsJsonObject("before").keySet(),
                        containsInAnyOrder("name", "note", "retired", "referenceCashAccount"));
        assertThat(updated.getAsJsonObject("before").get("referenceCashAccount").getAsString(), is(oldReference));
        assertThat(updated.getAsJsonObject("after").get("referenceCashAccount").getAsString(), is(second));
        assertThat(updated.getAsJsonObject("after").get("note").isJsonNull(), is(true));

        var reloaded = reload().getPortfolios().get(0);
        assertThat(reloaded.getName(), is("Renamed"));
        assertThat(reloaded.getNote() == null, is(true));
        assertThat(reloaded.isRetired(), is(true));
        assertThat(reloaded.getReferenceAccount().getUUID(), is(second));
    }

    @Test
    public void anInvestmentAccountUpdateMayRepeatTheReferenceCashAccount() throws IOException
    {
        var depot = reload().getPortfolios().get(0);

        var run = apply(investmentUpdate(depot.getUUID(),
                        "\"referenceCashAccount\":\"" + depot.getReferenceAccount().getUUID() + "\",\"note\":\"n\""));

        assertThat(run.stdout, run.exitCode, is(0));
        var updated = result(run, 0).getAsJsonArray("updated").get(0).getAsJsonObject();
        assertThat(updated.getAsJsonObject("after").keySet(), containsInAnyOrder("note"));
    }

    @Test
    public void refusesBadInvestmentAccountUpdates() throws IOException
    {
        var depot = reload().getPortfolios().get(0);
        var cash = usedCashAccount();

        var run = apply(
                        investmentUpdate(depot.getUUID(),
                                        "\"name\":null,\"referenceCashAccount\":null"),
                        investmentUpdate(depot.getUUID(),
                                        "\"referenceCashAccount\":\"00000000-0000-0000-0000-000000000000\""),
                        investmentUpdate(cash.getUUID(), "\"note\":\"a cash account is not a depot\""),
                        "{\"operation\":\"investment-account.update\",\"note\":\"x\",\"colour\":\"red\"}");

        assertThat(run.exitCode, is(1));
        assertThat(failureKeys(run),
                        containsInAnyOrder("0:name:required", "0:referenceCashAccount:required",
                                        "1:uuid:duplicate-target", "1:referenceCashAccount:reference-not-found",
                                        "2:uuid:reference-not-found", "3:uuid:required", "3:colour:unknown-field"));
    }
}
