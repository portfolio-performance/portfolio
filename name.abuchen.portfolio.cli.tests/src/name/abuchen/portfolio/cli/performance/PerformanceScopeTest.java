package name.abuchen.portfolio.cli.performance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.cli.command.FacetHarness;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * {@link PerformanceScope#FACET} at its own interface: the scope options,
 * <code>--by</code>, the rules between them and <code>meta.scope</code>.
 */
public class PerformanceScopeTest
{
    private Client client;
    private Security alpha;
    private Account cash;
    private Portfolio depot;

    @Before
    public void setUp()
    {
        client = new Client();

        alpha = new SecurityBuilder().addTo(client);
        alpha.setName("Alpha");

        cash = new AccountBuilder().addTo(client);
        cash.setName("Cash");

        depot = new PortfolioBuilder(cash).addTo(client);
        depot.setName("Depot");
    }

    private FacetHarness.Result<PerformanceScope> run(String... args)
    {
        return FacetHarness.run(PerformanceScope.FACET, client, args);
    }

    @Test
    public void withoutAScopeOptionTheScopeIsTheWholeClient()
    {
        var result = run();

        assertThat(result.value().breakdown(), is(Optional.empty()));
        assertThat(result.meta().getAsJsonObject("scope").keySet(), is(Set.of("kind")));
        assertThat(result.meta().getAsJsonObject("scope").get("kind").getAsString(), is("client"));
    }

    @Test
    public void anInvestmentAccountIsResolvedByNameAndEchoedWithoutItsReferenceCashAccount()
    {
        var scope = run("--investment-account", "Depot").meta().getAsJsonObject("scope");

        assertThat(scope.get("kind").getAsString(), is("investment-account"));
        assertThat(scope.get("uuid").getAsString(), is(depot.getUUID()));
        assertThat(scope.get("name").getAsString(), is("Depot"));
        assertThat(scope.has("referenceCashAccount"), is(false));
    }

    @Test
    public void theReferenceCashAccountIsAddedOnRequest()
    {
        var scope = run("--investment-account", depot.getUUID(), "--with-reference-cash-account").meta()
                        .getAsJsonObject("scope");

        assertThat(scope.get("uuid").getAsString(), is(depot.getUUID()));
        assertThat(scope.getAsJsonObject("referenceCashAccount").get("uuid").getAsString(), is(cash.getUUID()));
        assertThat(scope.getAsJsonObject("referenceCashAccount").get("name").getAsString(), is("Cash"));
    }

    @Test
    public void aCashAccountAndAnInstrumentAreEchoed()
    {
        var account = run("--cash-account", "Cash").meta().getAsJsonObject("scope");
        assertThat(account.get("kind").getAsString(), is("cash-account"));
        assertThat(account.get("uuid").getAsString(), is(cash.getUUID()));

        var instrument = run("--instrument", "Alpha").meta().getAsJsonObject("scope");
        assertThat(instrument.get("kind").getAsString(), is("instrument"));
        assertThat(instrument.get("uuid").getAsString(), is(alpha.getUUID()));
        assertThat(instrument.get("name").getAsString(), is("Alpha"));
    }

    @Test
    public void theReferenceCashAccountFlagNeedsAnInvestmentAccount()
    {
        var result = run("--with-reference-cash-account");

        assertThat(result.code(), is("option-required"));
        assertThat(result.failure().toProblem().get("option"), is((Object) "investment-account"));
    }

    @Test
    public void anInvestmentAccountWithoutAReferenceCashAccountFailsWhenItIsAskedFor()
    {
        var orphan = new Portfolio();
        orphan.setName("Orphan");
        client.addPortfolio(orphan);

        var result = run("--investment-account", "Orphan", "--with-reference-cash-account");

        assertThat(result.code(), is("no-reference-cash-account"));
        assertThat(result.failure().toProblem().get("status"), is((Object) 422));
    }

    @Test
    public void atMostOneScopeOptionIsAllowed()
    {
        var options = List.of(List.of("--investment-account", "Depot"), List.of("--cash-account", "Cash"),
                        List.of("--instrument", "Alpha"));

        for (var first = 0; first < options.size(); first++)
        {
            for (var second = first + 1; second < options.size(); second++)
            {
                var args = new ArrayList<>(options.get(first));
                args.addAll(options.get(second));

                assertThat(args.toString(), run(args.toArray(String[]::new)).code(), is("conflicting-options"));
            }
        }
    }

    @Test
    public void byInstrumentBreaksDownTheWholeClientOrAnInvestmentAccount()
    {
        assertThat(run("--by", "instrument").value().breakdown(), is(Optional.of(client)));

        var narrowed = run("--investment-account", "Depot", "--by", "instrument").value().breakdown();
        assertThat(narrowed.isPresent(), is(true));
        assertThat(narrowed.get() == client, is(false));
    }

    @Test
    public void byWithACashAccountOrAnInstrumentConflicts()
    {
        assertThat(run("--cash-account", "Cash", "--by", "instrument").code(), is("conflicting-options"));
        assertThat(run("--instrument", "Alpha", "--by", "instrument").code(), is("conflicting-options"));
    }

    @Test
    public void byAnythingButInstrumentIsAnInvalidOptionValue()
    {
        assertThat(run("--by", "account").code(), is("invalid-option-value"));
    }
}
