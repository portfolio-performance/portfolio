package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Values;

/**
 * Test case to verify that ClientClassificationFilter properly handles
 * ReadOnlyAccount instances when the client is already filtered by some other
 * filter. This reproduces the bug described in PR #4838 where account
 * categorization checks fail when dealing with filtered clients that already
 * contain ReadOnlyAccount instances instead of original Account instances.
 */
@SuppressWarnings("nls")
public class ClientClassificationFilterWithPortfolioFilterTest
{
    private Client client;
    private Classification cashClassification;

    @Before
    public void prepare()
    {
        client = new Client();

        var security = new SecurityBuilder().addTo(client);

        Arrays.asList("A", "B", "C").forEach(index -> {
            Account a = new AccountBuilder() //
                            .deposit_("2016-01-01", Values.Amount.factorize(100))
                            .dividend("2016-03-01", Values.Amount.factorize(10), security) //
                            .addTo(client);
            a.setName("Account " + index);

            Portfolio p = new PortfolioBuilder(a) //
                            .buy(security, "2016-02-01", Values.Share.factorize(1), Values.Amount.factorize(100))
                            .addTo(client);
            p.setName("Portfolio " + index);
        });

        // Create a simple taxonomy with a cash classification
        var taxonomy = new Taxonomy("Test Taxonomy");
        var root = new Classification("root", "Root");
        taxonomy.setRootNode(root);

        // Create a cash classification that includes all accounts
        cashClassification = new Classification(root, "cash", "Cash");
        root.addChild(cashClassification);

        // Add all accounts to the cash classification with 100% weight
        for (Account account : client.getAccounts())
        {
            cashClassification.addAssignment(new Classification.Assignment(account));
        }

        client.addTaxonomy(taxonomy);
    }

    @Test
    public void testClassificationFilterAfterPortfolioFilter()
    {
        // Create a PortfolioClientFilter that includes all portfolios and
        // accounts, This will wrap the original accounts in ReadOnlyAccount
        // instances
        var portfolioFilter = new PortfolioClientFilter(client.getPortfolios(), client.getAccounts());

        var filteredByPortfolio = portfolioFilter.filter(client);

        // Verify that the portfolio filter worked
        assertThat("Portfolio filter should preserve all portfolios", filteredByPortfolio.getPortfolios().size(),
                        is(client.getPortfolios().size()));
        assertThat("Portfolio filter should preserve all accounts", filteredByPortfolio.getAccounts().size(),
                        is(client.getAccounts().size()));

        // Now apply the classification filter on the already-filtered client
        var classificationFilter = new ClientClassificationFilter(cashClassification);
        var filteredByClassification = classificationFilter.filter(filteredByPortfolio);

        // The classification filter should work correctly even on a
        // pre-filtered client
        assertThat("Classification filter should work on pre-filtered client",
                        filteredByClassification.getAccounts().size() > 0, is(true));

        // Verify that the accounts in the classification-filtered client have
        // transactions
        var hasTransactions = filteredByClassification.getAccounts().stream()
                        .anyMatch(account -> !account.getTransactions().isEmpty());

        assertThat("Accounts should have transactions after classification filter", hasTransactions, is(true));
    }
}
