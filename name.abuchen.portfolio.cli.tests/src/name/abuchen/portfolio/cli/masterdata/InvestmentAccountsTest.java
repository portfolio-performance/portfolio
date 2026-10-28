package name.abuchen.portfolio.cli.masterdata;

import static name.abuchen.portfolio.cli.masterdata.InvestmentAccounts.INVESTMENT_ACCOUNTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

public class InvestmentAccountsTest
{
    private final Client client = new Client();

    private Portfolio resolve(String value) throws CliException
    {
        return INVESTMENT_ACCOUNTS.reference().resolve(client, "investment-account", value);
    }

    @Test
    public void rendersEveryMemberInDeclarationOrder()
    {
        var cash = new AccountBuilder().addTo(client);
        var portfolio = new PortfolioBuilder(cash).addTo(client);
        portfolio.setName("Depot");
        portfolio.setNote("long term");
        portfolio.setRetired(true);
        portfolio.setUpdatedAt(Instant.parse("2026-10-24T15:04:57Z"));

        var item = INVESTMENT_ACCOUNTS.renderer(client).apply(portfolio);

        assertThat(item.keySet().toString(), is("[uuid, name, referenceCashAccount, note, retired, updatedAt]"));
        assertThat(item.get("uuid"), is(portfolio.getUUID()));
        assertThat(item.get("name"), is("Depot"));
        assertThat(item.get("referenceCashAccount"), is(cash.getUUID()));
        assertThat(item.get("note"), is("long term"));
        assertThat(item.get("retired"), is(true));
        assertThat(item.get("updatedAt"), is("2026-10-24T15:04:57Z"));
    }

    @Test
    public void unsetMembersAreOmitted()
    {
        var portfolio = new PortfolioBuilder().addTo(client);
        portfolio.setNote("");
        portfolio.setReferenceAccount(null);
        portfolio.setUpdatedAt(null);

        var item = INVESTMENT_ACCOUNTS.renderer(client).apply(portfolio);

        assertThat(item.keySet().toString(), is("[uuid, name, retired]"));
        assertThat(item.get("retired"), is(false));
    }

    @Test
    public void resolvesByUuidAndNameSearchingOnlyInvestmentAccounts() throws CliException
    {
        var cash = new AccountBuilder().addTo(client);
        cash.setName("Main");
        var depot = new PortfolioBuilder(cash).addTo(client);
        depot.setName("Broker");

        assertThat(resolve(depot.getUUID()), is(sameInstance(depot)));
        assertThat(resolve("BROKER"), is(sameInstance(depot)));

        assertThat(assertThrows(CliException.class, () -> resolve("Main")).getCode(), is("reference-not-found"));
        assertThat(assertThrows(CliException.class, () -> resolve(cash.getUUID())).getCode(),
                        is("reference-not-found"));
    }

    @Test
    public void ambiguousInvestmentAccountsCarryNoDetails()
    {
        var first = new PortfolioBuilder().addTo(client);
        first.setName("Depot");
        new PortfolioBuilder().addTo(client).setName("depot");

        var e = assertThrows(CliException.class, () -> resolve("DEPOT"));

        assertThat(e.getCode(), is("reference-ambiguous"));
        @SuppressWarnings("unchecked")
        var candidates = (List<Map<String, Object>>) e.toProblem().get("candidates");
        var candidate = candidates.stream().filter(c -> c.get("uuid").equals(first.getUUID())).findFirst()
                        .orElseThrow();
        assertThat(candidate.keySet().toString(), is("[uuid, name, retired, matchedBy]"));
    }
}
