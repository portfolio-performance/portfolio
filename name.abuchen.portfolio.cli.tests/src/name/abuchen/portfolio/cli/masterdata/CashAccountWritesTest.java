package name.abuchen.portfolio.cli.masterdata;

import static name.abuchen.portfolio.cli.masterdata.CashAccounts.CASH_ACCOUNTS;
import static name.abuchen.portfolio.cli.masterdata.Writing.ALL_EXISTED;
import static name.abuchen.portfolio.cli.masterdata.Writing.UNCLAIMED;
import static name.abuchen.portfolio.cli.masterdata.Writing.applied;
import static name.abuchen.portfolio.cli.masterdata.Writing.asJson;
import static name.abuchen.portfolio.cli.masterdata.Writing.failures;
import static name.abuchen.portfolio.cli.masterdata.Writing.input;
import static name.abuchen.portfolio.cli.masterdata.Writing.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

public class CashAccountWritesTest
{
    private final Client client = new Client();

    private Account account(String name)
    {
        var account = new AccountBuilder("USD").addTo(client);
        account.setName(name);
        return account;
    }

    private Outcome add(String input)
    {
        return CASH_ACCOUNTS.writes(client, ALL_EXISTED).add(json(input));
    }

    private Outcome update(Account account, String fields)
    {
        return CASH_ACCOUNTS.writes(client, ALL_EXISTED)
                        .update(json("{\"uuid\":\"" + account.getUUID() + "\"," + fields + "}"), UNCLAIMED);
    }

    private static Account byUuid(Client in, String uuid)
    {
        return in.getAccounts().stream().filter(a -> a.getUUID().equals(uuid)).findFirst().orElseThrow();
    }

    @Test
    public void renderAddRenderRoundTripsAndUpdatingWithTheRenderChangesNothing()
    {
        var original = account("Savings");
        original.setNote("rainy day");
        original.setRetired(true);
        var rendered = CASH_ACCOUNTS.renderer(client).apply(original);

        var copy = new Client();
        var added = applied(CASH_ACCOUNTS.writes(copy, ALL_EXISTED).add(input(rendered, "uuid", "updatedAt")));

        assertThat(added.kind(), is(Outcome.Entity.CASH_ACCOUNT));
        assertThat(added.after(),
                        is(Map.of("name", "Savings", "currencyCode", "USD", "note", "rainy day", "retired", true)));
        var reRendered = CASH_ACCOUNTS.renderer(copy).apply(byUuid(copy, added.recordId()));
        assertThat(input(reRendered, "uuid", "updatedAt"), is(input(rendered, "uuid", "updatedAt")));

        assertThat(CASH_ACCOUNTS.writes(client, ALL_EXISTED).update(asJson(rendered), UNCLAIMED),
                        is(new Outcome.Unchanged(original.getUUID())));
    }

    @Test
    public void anUpdateChangesOnlyThePresentMembers()
    {
        var account = account("Old");
        account.setNote("first");

        var updated = applied(update(account, "\"name\":\"Renamed\",\"note\":null,\"retired\":true"));

        assertThat(updated.before().toString(), is("{name=Old, note=first, retired=false}"));
        assertThat(updated.after().toString(), is("{name=Renamed, note=null, retired=true}"));
        assertThat(account.getNote(), is(nullValue()));
        assertThat(account.getCurrencyCode(), is("USD"));
    }

    @Test
    public void theNameAndTheCurrencyAreRequiredAndCannotBeCleared()
    {
        var account = account("Kept");

        assertThat(failures(add("{\"note\":\"nameless\"}")), is(Set.of("name:required", "currencyCode:required")));
        assertThat(failures(update(account, "\"name\":null,\"currencyCode\":\"\"")),
                        is(Set.of("name:required", "currencyCode:required")));
        assertThat(client.getAccounts().size(), is(1));
        assertThat(account.getCurrencyCode(), is("USD"));
    }

    @Test
    public void refusesMistypedMembers()
    {
        assertThat(failures(add("{\"name\":\"Typed\",\"currencyCode\":1,\"retired\":\"no\"}")),
                        is(Set.of("currencyCode:invalid-type", "retired:invalid-type")));
    }

    @Test
    public void refusesAnUnknownCurrency()
    {
        assertThat(failures(add("{\"name\":\"Odd\",\"currencyCode\":\"ZZZ\"}")),
                        is(Set.of("currencyCode:invalid-currency")));
        assertThat(client.getAccounts().isEmpty(), is(true));
    }

    @Test
    public void theCurrencyCanChangeOnlyBeforeTheAccountHasTransactions()
    {
        var idle = account("Idle");
        var used = new AccountBuilder("USD").deposit_("2026-01-02", 100).addTo(client);

        var refused = update(used, "\"currencyCode\":\"EUR\"");
        assertThat(failures(refused), is(Set.of("currencyCode:currency-immutable")));
        assertThat(((Outcome.Refused) refused).failures().get(0).message(),
                        is("The currency can't change because the account has transactions."));
        assertThat(applied(update(used, "\"currencyCode\":\"USD\",\"note\":\"kept\"")).after().keySet(),
                        is(Set.of("note")));

        applied(update(idle, "\"currencyCode\":\"CHF\""));
        assertThat(idle.getCurrencyCode(), is("CHF"));
    }

    @Test
    public void anUpdateNamesAnExistingCashAccountAndClaimsIt()
    {
        var account = account("Claimed");
        var added = account("Added in this batch");

        assertThat(failures(CASH_ACCOUNTS.writes(client, entity -> entity != added)
                        .update(json("{\"uuid\":\"" + added.getUUID() + "\"}"), UNCLAIMED)),
                        is(Set.of("uuid:reference-not-found")));
        assertThat(failures(CASH_ACCOUNTS.writes(client, ALL_EXISTED).update(
                        json("{\"uuid\":\"" + account.getUUID() + "\",\"note\":\"two\"}"),
                        uuid -> OptionalInt.of(0))), is(Set.of("uuid:duplicate-target")));
        assertThat(account.getNote(), is(nullValue()));
    }
}
