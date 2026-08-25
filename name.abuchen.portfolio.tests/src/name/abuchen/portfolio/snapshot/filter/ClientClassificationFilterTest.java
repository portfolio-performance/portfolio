package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ClientClassificationFilterTest
{
    private Client client;
    private Account account;
    private Classification cashClassification;

    /**
     * Creates one account (categorized as cash) with a buy of a security that is
     * *not* part of the classification plus interest with taxes.
     */
    @Before
    public void setupClient()
    {
        client = new Client();

        var security = new SecurityBuilder().addTo(client);

        account = new AccountBuilder() //
                        .deposit_("2016-01-01", Values.Amount.factorize(100))
                        .interest("2016-04-01", Values.Amount.factorize(10), Values.Amount.factorize(2)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2016-02-01", Values.Share.factorize(1), Values.Amount.factorize(100))
                        .addTo(client);

        var taxonomy = new Taxonomy("Test Taxonomy");
        var root = new Classification("root", "Root");
        taxonomy.setRootNode(root);

        cashClassification = new Classification(root, "cash", "Cash");
        root.addChild(cashClassification);
        cashClassification.addAssignment(new Classification.Assignment(account));

        client.addTaxonomy(taxonomy);
    }

    @Test
    public void testThatNotesArePreservedWhenTransactionsAreRetyped()
    {
        account.getTransactions().forEach(t -> t.setNote("note " + t.getType().name()));

        Client result = new ClientClassificationFilter(cashClassification).filter(client);

        List<AccountTransaction> transactions = result.getAccounts().get(0).getTransactions();

        // the deposit and the interest are copied as-is, keeping their note
        assertThat(notesOf(transactions, AccountTransaction.Type.DEPOSIT), is(Arrays.asList("note DEPOSIT")));
        assertThat(notesOf(transactions, AccountTransaction.Type.INTEREST), is(Arrays.asList("note INTEREST")));

        // the buy is retyped to a removal because the security is not part of
        // the classification - it keeps the note of the original buy. The
        // second removal offsets the taxes of the interest; it is created by
        // the filter rather than copied and therefore must not carry a note.
        assertThat(notesOf(transactions, AccountTransaction.Type.REMOVAL), containsInAnyOrder("note BUY", null));
    }

    private List<String> notesOf(List<AccountTransaction> transactions, AccountTransaction.Type type)
    {
        return transactions.stream().filter(t -> t.getType() == type).map(Transaction::getNote)
                        .collect(Collectors.toList());
    }
}
