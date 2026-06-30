package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
final class LedgerGuardrailTestSupport
{
    static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);

    private LedgerGuardrailTestSupport()
    {
    }

    static void assertMetadataSetterUpdatesLedgerTruthOnly(Transaction transaction, int index)
    {
        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var entry = ledgerBacked.getLedgerEntry();
        var snapshot = Snapshot.of(entry);
        var dateTime = DATE_TIME.plusDays(10 + index);
        var note = "guard note " + index;
        var source = "guard source " + index;

        transaction.setDateTime(dateTime);
        transaction.setNote(note);
        transaction.setSource(source);

        snapshot.assertOnlyMetadataChanged(entry, dateTime, note, source);
        assertThat(transaction.getDateTime(), is(dateTime));
        assertThat(transaction.getNote(), is(note));
        assertThat(transaction.getSource(), is(source));
    }

    static <T extends Transaction> void assertRejectedWithoutChangingState(T transaction, Consumer<T> mutation)
    {
        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var entry = ledgerBacked.getLedgerEntry();
        var snapshot = Snapshot.of(entry);

        assertThrows(UnsupportedOperationException.class, () -> mutation.accept(transaction));

        snapshot.assertUnchanged(entry);
    }

    static void assertRejectedWithoutChangingState(LedgerBackedTransaction ledgerBacked, Runnable mutation)
    {
        var entry = ledgerBacked.getLedgerEntry();
        var snapshot = Snapshot.of(entry);

        assertThrows(UnsupportedOperationException.class, mutation::run);

        snapshot.assertUnchanged(entry);
    }

    static void assertAcceptedWithoutChangingState(LedgerBackedTransaction ledgerBacked, Runnable mutation)
    {
        var entry = ledgerBacked.getLedgerEntry();
        var snapshot = Snapshot.of(entry);

        mutation.run();

        snapshot.assertUnchanged(entry);
    }

    static LedgerTransactionCreator creator(Client client)
    {
        return new LedgerTransactionCreator(client);
    }

    static LedgerTransactionMetadata metadata()
    {
        return LedgerTransactionMetadata.of(DATE_TIME).withNote("note").withSource("source");
    }

    static Account account()
    {
        return new Account();
    }

    static Portfolio portfolio()
    {
        return new Portfolio();
    }

    static Security security()
    {
        return new Security("Security", CurrencyUnit.EUR);
    }

    static LedgerAccountCashLeg cashLeg(Account account, int amount)
    {
        return LedgerAccountCashLeg.of(account, money(amount));
    }

    static LedgerDeliveryLeg deliveryLeg(Portfolio portfolio)
    {
        return LedgerDeliveryLeg.of(portfolio, LedgerSecurityQuantity.of(security(), Values.Share.factorize(5)),
                        money(100));
    }

    static LedgerPortfolioSecurityLeg portfolioLeg(Portfolio portfolio, int amount)
    {
        return LedgerPortfolioSecurityLeg.of(portfolio,
                        LedgerSecurityQuantity.of(security(), Values.Share.factorize(5)), money(amount));
    }

    static Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    static LedgerPosting posting(LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type).findFirst().orElseThrow();
    }

    static LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private record Snapshot(LocalDateTime dateTime, String note, String source, List<PostingSnapshot> postings,
                    List<ProjectionSnapshot> projections, List<OwnerListSnapshot> accountProjectionUUIDs,
                    List<OwnerListSnapshot> portfolioProjectionUUIDs)
    {
        static Snapshot of(LedgerEntry entry)
        {
            return new Snapshot(entry.getDateTime(), entry.getNote(), entry.getSource(),
                            entry.getPostings().stream().map(PostingSnapshot::of).toList(),
                            entry.getProjectionRefs().stream().map(ProjectionSnapshot::of).toList(),
                            accountOwners(entry).stream().map(OwnerListSnapshot::of).toList(),
                            portfolioOwners(entry).stream().map(OwnerListSnapshot::of).toList());
        }

        void assertUnchanged(LedgerEntry entry)
        {
            assertThat(entry.getDateTime(), is(dateTime));
            assertThat(entry.getNote(), is(note));
            assertThat(entry.getSource(), is(source));
            assertThat(entry.getPostings().stream().map(PostingSnapshot::of).toList(), is(postings));
            assertThat(entry.getProjectionRefs().stream().map(ProjectionSnapshot::of).toList(), is(projections));
            assertThat(accountOwners(entry).stream().map(OwnerListSnapshot::of).toList(), is(accountProjectionUUIDs));
            assertThat(portfolioOwners(entry).stream().map(OwnerListSnapshot::of).toList(), is(portfolioProjectionUUIDs));
        }

        void assertOnlyMetadataChanged(LedgerEntry entry, LocalDateTime newDateTime, String newNote, String newSource)
        {
            assertThat(entry.getDateTime(), is(newDateTime));
            assertThat(entry.getNote(), is(newNote));
            assertThat(entry.getSource(), is(newSource));
            assertThat(entry.getPostings().stream().map(PostingSnapshot::of).toList(), is(postings));
            assertThat(entry.getProjectionRefs().stream().map(ProjectionSnapshot::of).toList(), is(projections));
            assertThat(accountOwners(entry).stream().map(OwnerListSnapshot::of).toList(), is(accountProjectionUUIDs));
            assertThat(portfolioOwners(entry).stream().map(OwnerListSnapshot::of).toList(), is(portfolioProjectionUUIDs));
        }

        private static List<Account> accountOwners(LedgerEntry entry)
        {
            return entry.getProjectionRefs().stream().map(LedgerProjectionRef::getAccount)
                            .filter(account -> account != null).distinct().toList();
        }

        private static List<Portfolio> portfolioOwners(LedgerEntry entry)
        {
            return entry.getProjectionRefs().stream().map(LedgerProjectionRef::getPortfolio)
                            .filter(portfolio -> portfolio != null).distinct().toList();
        }
    }

    private record OwnerListSnapshot(String ownerUUID, List<String> transactionUUIDs)
    {
        static OwnerListSnapshot of(Account account)
        {
            return new OwnerListSnapshot(account.getUUID(),
                            account.getTransactions().stream().map(Transaction::getUUID).toList());
        }

        static OwnerListSnapshot of(Portfolio portfolio)
        {
            return new OwnerListSnapshot(portfolio.getUUID(),
                            portfolio.getTransactions().stream().map(Transaction::getUUID).toList());
        }
    }

    private record PostingSnapshot(String uuid, LedgerPostingType type, long amount, String currency, Long forexAmount,
                    String forexCurrency, BigDecimal exchangeRate, Security security, long shares, Account account,
                    Portfolio portfolio, List<LedgerParameter<?>> parameters)
    {
        static PostingSnapshot of(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount(), posting.getPortfolio(),
                            List.copyOf(posting.getParameters()));
        }
    }

    private record ProjectionSnapshot(String uuid, LedgerProjectionRole role, Account account, Portfolio portfolio,
                    String primaryPostingUUID, String postingGroupUUID)
    {
        static ProjectionSnapshot of(LedgerProjectionRef projection)
        {
            return new ProjectionSnapshot(projection.getUUID(), projection.getRole(), projection.getAccount(),
                            projection.getPortfolio(), projection.getPrimaryPostingUUID(),
                            projection.getPostingGroupUUID());
        }
    }
}
