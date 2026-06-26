package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests rollback behavior for failed ledger owner changes.
 * These tests make sure owner lists are restored when a later validation step rejects the change.
 */
@SuppressWarnings("nls")
public class LedgerOwnerChangeFailurePathTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);

    /**
     * Verifies that an account-only owner move rolls back when the later edit is invalid.
     * The booking must stay on the original account and no partial owner-list move may remain.
     */
    @Test
    public void testAccountOnlyOwnerMoveRollsBackWhenLaterEditRejects()
    {
        var client = new Client();
        var source = account("Source");
        var target = account("Target");
        client.addAccount(source);
        client.addAccount(target);

        var creator = new LedgerAccountOnlyTransactionCreator(client);
        var transaction = creator.create(source, AccountTransaction.Type.DEPOSIT, DATE_TIME,
                        Values.Amount.factorize(100), CurrencyUnit.EUR, null, List.of(), "note", "source");
        var before = ClientSnapshot.capture(client);

        assertThrows(IllegalArgumentException.class,
                        () -> creator.update(transaction, target, AccountTransaction.Type.DEPOSIT, DATE_TIME,
                                        -Values.Amount.factorize(1), CurrencyUnit.EUR, null, List.of(), "bad",
                                        "bad source"));

        assertThat(ClientSnapshot.capture(client), is(before));
        assertThat(source.getTransactions(), is(List.of(transaction)));
        assertTrue(target.getTransactions().isEmpty());
    }

    /**
     * Verifies that a delivery owner move rolls back when the later edit is invalid.
     * The booking must stay on the original portfolio and no partial owner-list move may remain.
     */
    @Test
    public void testDeliveryOwnerMoveRollsBackWhenLaterEditRejects()
    {
        var fixture = portfolioFixture();
        var target = portfolio("Target", fixture.account());
        fixture.client().addPortfolio(target);

        var creator = new LedgerDeliveryTransactionCreator(fixture.client());
        var transaction = creator.create(fixture.portfolio(), PortfolioTransaction.Type.DELIVERY_INBOUND, DATE_TIME,
                        Values.Amount.factorize(100), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(10), null, null, List.of(), "note", "source");
        var before = ClientSnapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class,
                        () -> creator.update(transaction, target, PortfolioTransaction.Type.DELIVERY_INBOUND,
                                        DATE_TIME, -Values.Amount.factorize(1), CurrencyUnit.EUR,
                                        fixture.security(), Values.Share.factorize(10), null, null, List.of(), "bad",
                                        "bad source"));

        assertThat(ClientSnapshot.capture(fixture.client()), is(before));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(transaction)));
        assertTrue(target.getTransactions().isEmpty());
    }

    /**
     * Verifies that a buy/sell owner move rolls back when the later edit is invalid.
     * Both account and portfolio projections must remain on their original owners.
     */
    @Test
    public void testBuySellOwnerMoveRollsBackWhenLaterEditRejects()
    {
        var fixture = portfolioFixture();
        var targetPortfolio = portfolio("Target", fixture.account());
        fixture.client().addPortfolio(targetPortfolio);

        var creator = new LedgerBuySellTransactionCreator(fixture.client());
        var entry = creator.create(fixture.portfolio(), fixture.account(), PortfolioTransaction.Type.BUY, DATE_TIME,
                        Values.Amount.factorize(100), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(10), List.of(), "note", "source");
        var before = ClientSnapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class,
                        () -> creator.update(entry, targetPortfolio, fixture.account(), PortfolioTransaction.Type.BUY,
                                        DATE_TIME, -Values.Amount.factorize(1), CurrencyUnit.EUR, fixture.security(),
                                        Values.Share.factorize(10), List.of(), "bad", "bad source"));

        assertThat(ClientSnapshot.capture(fixture.client()), is(before));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(entry.getPortfolioTransaction())));
        assertTrue(targetPortfolio.getTransactions().isEmpty());
        assertThat(fixture.account().getTransactions(), is(List.of(entry.getAccountTransaction())));
    }

    /**
     * Verifies that an account-transfer owner move rolls back when the later edit is invalid.
     * Source and target account lists must stay unchanged after the failed update.
     */
    @Test
    public void testAccountTransferOwnerMoveRollsBackWhenLaterEditRejects()
    {
        var client = new Client();
        var source = account("Source");
        var target = account("Target");
        var newSource = account("New Source");
        client.addAccount(source);
        client.addAccount(target);
        client.addAccount(newSource);

        var creator = new LedgerAccountTransferTransactionCreator(client);
        var transfer = creator.create(source, target, DATE_TIME, Values.Amount.factorize(100), CurrencyUnit.EUR,
                        Values.Amount.factorize(100), CurrencyUnit.EUR, null, null, "note", "source");
        var before = ClientSnapshot.capture(client);

        assertThrows(IllegalArgumentException.class,
                        () -> creator.update(transfer, newSource, target, DATE_TIME, -Values.Amount.factorize(1),
                                        CurrencyUnit.EUR, Values.Amount.factorize(100), CurrencyUnit.EUR, null, null,
                                        "bad", "bad source"));

        assertThat(ClientSnapshot.capture(client), is(before));
        assertThat(source.getTransactions(), is(List.of(transfer.getSourceTransaction())));
        assertThat(target.getTransactions(), is(List.of(transfer.getTargetTransaction())));
        assertTrue(newSource.getTransactions().isEmpty());
    }

    /**
     * Verifies that a portfolio-transfer owner move rolls back when the later edit is invalid.
     * Source and target portfolio lists must stay unchanged after the failed update.
     */
    @Test
    public void testPortfolioTransferOwnerMoveRollsBackWhenLaterEditRejects()
    {
        var fixture = portfolioTransferFixture();
        var newSource = portfolio("New Source", fixture.account());
        fixture.client().addPortfolio(newSource);

        var creator = new LedgerPortfolioTransferTransactionCreator(fixture.client());
        var transfer = creator.create(fixture.source(), fixture.target(), fixture.security(), DATE_TIME,
                        Values.Share.factorize(10), Values.Amount.factorize(100), CurrencyUnit.EUR, "note",
                        "source");
        var before = ClientSnapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class,
                        () -> creator.update(transfer, newSource, fixture.target(), fixture.security(), DATE_TIME,
                                        Values.Share.factorize(10), -Values.Amount.factorize(1), CurrencyUnit.EUR,
                                        "bad", "bad source"));

        assertThat(ClientSnapshot.capture(fixture.client()), is(before));
        assertThat(fixture.source().getTransactions(), is(List.of(transfer.getSourceTransaction())));
        assertThat(fixture.target().getTransactions(), is(List.of(transfer.getTargetTransaction())));
        assertTrue(newSource.getTransactions().isEmpty());
    }

    private static Account account(String name)
    {
        var account = new Account(name);

        account.setCurrencyCode(CurrencyUnit.EUR);

        return account;
    }

    private static Portfolio portfolio(String name, Account account)
    {
        var portfolio = new Portfolio(name);

        portfolio.setReferenceAccount(account);

        return portfolio;
    }

    private static PortfolioFixture portfolioFixture()
    {
        var client = new Client();
        var account = account("Account");
        var portfolio = portfolio("Portfolio", account);
        var security = new Security("Security", CurrencyUnit.EUR);

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new PortfolioFixture(client, account, portfolio, security);
    }

    private static PortfolioTransferFixture portfolioTransferFixture()
    {
        var client = new Client();
        var account = account("Account");
        var source = portfolio("Source", account);
        var target = portfolio("Target", account);
        var security = new Security("Security", CurrencyUnit.EUR);

        client.addAccount(account);
        client.addPortfolio(source);
        client.addPortfolio(target);
        client.addSecurity(security);

        return new PortfolioTransferFixture(client, account, source, target, security);
    }

    private record PortfolioFixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }

    private record PortfolioTransferFixture(Client client, Account account, Portfolio source, Portfolio target,
                    Security security)
    {
    }

    private record ClientSnapshot(List<EntrySnapshot> entries, List<List<String>> accountTransactions,
                    List<List<String>> portfolioTransactions, List<String> allTransactions)
    {
        static ClientSnapshot capture(Client client)
        {
            return new ClientSnapshot(client.getLedger().getEntries().stream().map(EntrySnapshot::capture).toList(),
                            client.getAccounts().stream().map(account -> transactionUUIDs(account.getTransactions()))
                                            .toList(),
                            client.getPortfolios().stream()
                                            .map(portfolio -> transactionUUIDs(portfolio.getTransactions())).toList(),
                            client.getAllTransactions().stream().map(pair -> pair.getTransaction().getUUID()).toList());
        }

        private static List<String> transactionUUIDs(List<? extends Transaction> transactions)
        {
            return transactions.stream().map(Transaction::getUUID).toList();
        }
    }

    private record EntrySnapshot(String uuid, Object type, Object dateTime, String note, String source,
                    List<PostingSnapshot> postings, List<ProjectionSnapshot> projections)
    {
        static EntrySnapshot capture(LedgerEntry entry)
        {
            return new EntrySnapshot(entry.getUUID(), entry.getType(), entry.getDateTime(), entry.getNote(),
                            entry.getSource(), entry.getPostings().stream().map(PostingSnapshot::capture).toList(),
                            entry.getProjectionRefs().stream().map(ProjectionSnapshot::capture).toList());
        }
    }

    private record PostingSnapshot(String uuid, Object type, long amount, String currency, Long forexAmount,
                    String forexCurrency, Object exchangeRate, Security security, long shares, Account account,
                    Portfolio portfolio, List<Object> parameters)
    {
        static PostingSnapshot capture(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount(), posting.getPortfolio(),
                            posting.getParameters().stream().map(parameter -> List.of(parameter.getType(),
                                            parameter.getValueKind(), parameter.getValue())).map(Object.class::cast)
                                            .toList());
        }
    }

    private record ProjectionSnapshot(String uuid, Object role, Account account, Portfolio portfolio,
                    String primaryPostingUUID, String postingGroupUUID)
    {
        static ProjectionSnapshot capture(LedgerProjectionRef projection)
        {
            return new ProjectionSnapshot(projection.getUUID(), projection.getRole(), projection.getAccount(),
                            projection.getPortfolio(), projection.getPrimaryPostingUUID(),
                            projection.getPostingGroupUUID());
        }
    }
}
