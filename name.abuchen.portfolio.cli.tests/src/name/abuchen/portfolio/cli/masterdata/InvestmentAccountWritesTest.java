package name.abuchen.portfolio.cli.masterdata;

import static name.abuchen.portfolio.cli.masterdata.InvestmentAccounts.INVESTMENT_ACCOUNTS;
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
import static org.hamcrest.Matchers.sameInstance;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

public class InvestmentAccountWritesTest
{
    private final Client client = new Client();
    private final Account cash = new AccountBuilder().addTo(client);

    private Portfolio depot(String name)
    {
        var portfolio = new PortfolioBuilder(cash).addTo(client);
        portfolio.setName(name);
        return portfolio;
    }

    private Outcome add(String input)
    {
        return INVESTMENT_ACCOUNTS.writes(client, ALL_EXISTED).add(json(input));
    }

    private Outcome update(Portfolio portfolio, String fields)
    {
        return INVESTMENT_ACCOUNTS.writes(client, ALL_EXISTED)
                        .update(json("{\"uuid\":\"" + portfolio.getUUID() + "\"," + fields + "}"), UNCLAIMED);
    }

    @Test
    public void renderAddRenderRoundTripsAndUpdatingWithTheRenderChangesNothing()
    {
        var original = depot("Depot");
        original.setNote("long term");
        original.setRetired(true);
        var rendered = INVESTMENT_ACCOUNTS.renderer(client).apply(original);

        var added = applied(add(input(rendered, "uuid", "updatedAt").toString()));

        assertThat(added.kind(), is(Outcome.Entity.INVESTMENT_ACCOUNT));
        assertThat(added.after(), is(Map.of("name", "Depot", "referenceCashAccount", cash.getUUID(), "note",
                        "long term", "retired", true)));
        var copy = client.getPortfolios().stream().filter(p -> p.getUUID().equals(added.recordId())).findFirst()
                        .orElseThrow();
        assertThat(copy.getReferenceAccount(), is(sameInstance(cash)));
        var reRendered = INVESTMENT_ACCOUNTS.renderer(client).apply(copy);
        assertThat(input(reRendered, "uuid", "updatedAt"), is(input(rendered, "uuid", "updatedAt")));

        assertThat(INVESTMENT_ACCOUNTS.writes(client, ALL_EXISTED).update(asJson(rendered), UNCLAIMED),
                        is(new Outcome.Unchanged(original.getUUID())));
    }

    @Test
    public void anUpdateChangesOnlyThePresentMembersAndMayMoveTheReferenceCashAccount()
    {
        var depot = depot("Old");
        depot.setNote("first");
        var second = new AccountBuilder().addTo(client);

        var updated = applied(update(depot, "\"name\":\"Renamed\",\"note\":null,\"referenceCashAccount\":\""
                        + second.getUUID() + "\""));

        assertThat(updated.before(), is(Map.of("name", "Old", "referenceCashAccount", cash.getUUID(), "note",
                        "first")));
        assertThat(depot.getReferenceAccount(), is(sameInstance(second)));
        assertThat(depot.getNote(), is(nullValue()));

        // repeating the reference cash account is no change
        assertThat(applied(update(depot, "\"referenceCashAccount\":\"" + second.getUUID() + "\",\"note\":\"n\""))
                        .after().keySet(), is(Set.of("note")));
    }

    @Test
    public void theNameAndTheReferenceCashAccountAreRequiredAndCannotBeCleared()
    {
        var depot = depot("Kept");

        assertThat(failures(add("{\"note\":\"bare\"}")),
                        is(Set.of("name:required", "referenceCashAccount:required")));
        assertThat(failures(update(depot, "\"name\":null,\"referenceCashAccount\":null")),
                        is(Set.of("name:required", "referenceCashAccount:required")));
        assertThat(depot.getReferenceAccount(), is(sameInstance(cash)));
    }

    @Test
    public void refusesMistypedMembers()
    {
        assertThat(failures(add("{\"name\":\"Typed\",\"referenceCashAccount\":{},\"note\":true}")),
                        is(Set.of("referenceCashAccount:invalid-type", "note:invalid-type")));
    }

    @Test
    public void theReferenceCashAccountMustBeAnExistingCashAccount()
    {
        var other = depot("Not cash");
        var addedInBatch = new AccountBuilder().addTo(client);
        var writes = INVESTMENT_ACCOUNTS.writes(client, entity -> entity != addedInBatch);

        for (var uuid : new String[] { "00000000-0000-0000-0000-000000000000", other.getUUID(),
                        addedInBatch.getUUID() })
        {
            var outcome = writes.add(json("{\"name\":\"Depot\",\"referenceCashAccount\":\"" + uuid + "\"}"));
            assertThat(uuid, failures(outcome), is(Set.of("referenceCashAccount:reference-not-found")));
        }
        assertThat(client.getPortfolios().size(), is(1));
    }

    @Test
    public void anUpdateNamesAnExistingInvestmentAccountAndClaimsIt()
    {
        var depot = depot("Claimed");

        assertThat(failures(INVESTMENT_ACCOUNTS.writes(client, ALL_EXISTED)
                        .update(json("{\"uuid\":\"" + cash.getUUID() + "\",\"note\":\"x\"}"), UNCLAIMED)),
                        is(Set.of("uuid:reference-not-found")));
        var outcome = INVESTMENT_ACCOUNTS.writes(client, ALL_EXISTED).update(
                        json("{\"uuid\":\"" + depot.getUUID() + "\",\"referenceCashAccount\":\"nope\"}"),
                        uuid -> OptionalInt.of(0));
        assertThat(failures(outcome),
                        is(Set.of("uuid:duplicate-target", "referenceCashAccount:reference-not-found")));
    }
}
