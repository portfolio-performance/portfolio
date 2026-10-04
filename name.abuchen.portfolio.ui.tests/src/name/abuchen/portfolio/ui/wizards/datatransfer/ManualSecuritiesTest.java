package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ManualSecuritiesTest
{
    private static final ImportAction.Context EMPTY_CONTEXT = new ImportAction.Context()
    {
        @Override
        public Account getAccount(String currencyCode)
        {
            return null;
        }

        @Override
        public Account getSecondaryAccount(String currencyCode)
        {
            return null;
        }

        @Override
        public Portfolio getPortfolio()
        {
            return null;
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            return null;
        }
    };

    private Client client;
    private Account account;
    private Portfolio portfolio;
    private Security newSecurity;

    /** shared by all pages of the wizard */
    private List<Security> additionalSecurities;

    /** two manual entry pages */
    private List<ExtractedEntry> entriesA;
    private List<ExtractedEntry> entriesB;
    private ManualSecurities pageA;
    private ManualSecurities pageB;

    @Before
    public void setup()
    {
        client = new Client();
        account = new AccountBuilder(CurrencyUnit.EUR).addTo(client);
        portfolio = new PortfolioBuilder(account).addTo(client);

        // not added to the client: created on the manual entry page
        newSecurity = new SecurityBuilder(CurrencyUnit.EUR).get();

        additionalSecurities = new ArrayList<>();
        entriesA = new ArrayList<>();
        entriesB = new ArrayList<>();
        pageA = new ManualSecurities(additionalSecurities, entriesA, this::allEntries);
        pageB = new ManualSecurities(additionalSecurities, entriesB, this::allEntries);
    }

    private Stream<ExtractedEntry> allEntries()
    {
        return Stream.concat(entriesA.stream(), entriesB.stream());
    }

    /** books a purchase in the shadow client as the transaction dialog does */
    private void buy(ManualSecurities page, List<ExtractedEntry> entries, Security security, int shares)
    {
        var session = ShadowSession.create(client, additionalSecurities);

        var entry = new BuySellEntry(session.toShadow(portfolio), session.toShadow(account));
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setSecurity(security);
        entry.setDate(LocalDateTime.parse("2024-01-02T10:00"));
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(1000));
        entry.setShares(Values.Share.factorize(shares));
        entry.insert();

        entries.addAll(page.withSecurityDependencies(session.harvest()));
    }

    /** imports the items of all pages as the wizard does */
    private void importAll()
    {
        var action = new InsertAction(client);
        for (var entries : List.of(entriesA, entriesB))
            for (var entry : entries)
                if (entry.isImported())
                    entry.getItem().apply(action, EMPTY_CONTEXT);
    }

    private long shares()
    {
        return portfolio.getTransactions().stream().mapToLong(PortfolioTransaction::getShares).sum();
    }

    private boolean isOffered()
    {
        return ShadowSession.create(client, additionalSecurities).getClient().getSecurities().contains(newSecurity);
    }

    @Test
    public void testNewSecurityIsListedAndOfferedButNotCreatedYet()
    {
        pageA.add(newSecurity);

        assertThat(entriesA, hasSize(1));
        assertThat(entriesA.get(0).getItem(), instanceOf(Extractor.SecurityItem.class));
        assertThat(isOffered(), is(true));
        assertThat(client.getSecurities().contains(newSecurity), is(false));
    }

    @Test
    public void testTransactionsOfAllPagesDependOnTheSecurityEntry()
    {
        pageA.add(newSecurity);
        buy(pageA, entriesA, newSecurity, 10);
        buy(pageB, entriesB, newSecurity, 5);

        assertThat(entriesA.get(1).getSecurityDependency(), is(entriesA.get(0)));
        assertThat(entriesB.get(0).getSecurityDependency(), is(entriesA.get(0)));
    }

    @Test
    public void testImportCreatesSecurityAndTransactions()
    {
        pageA.add(newSecurity);
        buy(pageA, entriesA, newSecurity, 10);
        buy(pageB, entriesB, newSecurity, 5);

        importAll();

        assertThat(client.getSecurities().contains(newSecurity), is(true));
        assertThat(shares(), is(Values.Share.factorize(15)));
    }

    @Test
    public void testExcludedSecurityExcludesItsTransactions()
    {
        pageA.add(newSecurity);
        buy(pageA, entriesA, newSecurity, 10);
        buy(pageB, entriesB, newSecurity, 5);

        entriesA.get(0).setImported(false);
        importAll();

        assertThat(client.getSecurities().contains(newSecurity), is(false));
        assertThat(shares(), is(0L));
    }

    @Test
    public void testRemovedSecurityRemovesItsTransactions()
    {
        pageA.add(newSecurity);
        buy(pageA, entriesA, newSecurity, 10);
        buy(pageB, entriesB, newSecurity, 5);

        pageA.remove(List.of(entriesA.get(0)), false);

        assertThat(entriesA, is(empty()));
        assertThat(entriesB.get(0).isImported(), is(false));
        assertThat(isOffered(), is(false));

        importAll();
        assertThat(client.getSecurities().contains(newSecurity), is(false));
        assertThat(shares(), is(0L));
    }

    @Test
    public void testSecurityIsNotRemovedWhileAnEditorIsOpen()
    {
        pageA.add(newSecurity);
        buy(pageA, entriesA, newSecurity, 10);
        var securityEntry = entriesA.get(0);
        var transaction = entriesA.get(1);

        pageA.remove(List.of(securityEntry, transaction), true);

        // only the transaction is removed
        assertThat(entriesA, contains(securityEntry));
        assertThat(isOffered(), is(true));

        pageA.remove(List.of(securityEntry), false);
        assertThat(entriesA, is(empty()));
    }

    @Test
    public void testTransactionWithExistingSecurityHasNoDependency()
    {
        var existing = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        pageA.add(newSecurity);
        buy(pageA, entriesA, existing, 3);
        pageA.remove(List.of(entriesA.get(0)), false);

        assertThat(entriesA, hasSize(1));
        assertThat(entriesA.get(0).getSecurityDependency(), is(nullValue()));
    }
}
