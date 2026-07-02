package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-aware reversal between inbound and outbound deliveries.
 * These tests make sure a delivery changes direction without creating duplicate transaction truth.
 */
@SuppressWarnings("nls")
public class LedgerDeliveryDirectionConverterTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 8, 9, 10);
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("0.5000");

    /**
     * Verifies that an inbound delivery can be reversed into an outbound delivery.
     * The same ledger entry and projection must continue to represent the booking after save/load.
     */
    @Test
    public void testReversesLedgerBackedInboundDeliveryToOutboundPreservingIdentityAndTruth() throws Exception
    {
        assertReversesDelivery(PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerEntryType.DELIVERY_OUTBOUND,
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerProjectionRole.DELIVERY_INBOUND,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, Values.Amount.factorize(113));
    }

    /**
     * Verifies that an outbound delivery can be reversed into an inbound delivery.
     * The same ledger entry and projection must continue to represent the booking after save/load.
     */
    @Test
    public void testReversesLedgerBackedOutboundDeliveryToInboundPreservingIdentityAndTruth() throws Exception
    {
        assertReversesDelivery(PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerEntryType.DELIVERY_INBOUND,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerProjectionRole.DELIVERY_OUTBOUND,
                        LedgerProjectionRole.DELIVERY_INBOUND, Values.Amount.factorize(127));
    }

    /**
     * Verifies that delivery reversal rejects a missing portfolio projection before mutation.
     * The converter must not rebuild the runtime view as a second booking truth.
     */
    @Test
    public void testMalformedDeliveryMissingProjectionRejectsBeforeMutation()
    {
        var fixture = fixture();
        var delivery = create(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        var entry = fixture.client().getLedger().getEntries().get(0);

        projection(entry, LedgerProjectionRole.DELIVERY_INBOUND).getPrimaryPosting().setSemanticRole(null);

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(pair(fixture, delivery)),
                        IllegalArgumentException.class);
    }

    /**
     * Verifies that delivery reversal rejects a missing security posting before mutation.
     * Missing security and share facts must not be inferred from the projection.
     */
    @Test
    public void testMalformedDeliveryMissingSecurityPostingRejectsBeforeMutation()
    {
        var fixture = fixture();
        var delivery = create(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removePosting(posting(entry, LedgerPostingType.SECURITY));

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(pair(fixture, delivery)),
                        IllegalArgumentException.class);
    }

    /**
     * Verifies that delivery reversal does not reinterpret unsupported forex posting facts.
     * The converter must reject before mutation when those facts cannot be preserved safely.
     */
    @Test
    public void testUnsupportedDeliveryPostingForexRejectsBeforeMutation()
    {
        var fixture = fixture();
        var delivery = create(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND,
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(246)), EXCHANGE_RATE);

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(pair(fixture, delivery)),
                        UnsupportedOperationException.class);
    }

    /**
     * Verifies that a plan-generated delivery can be reversed safely.
     * The plan reference must still resolve to the same generated booking after save/load.
     */
    @Test
    public void testInvestmentPlanReferencedDeliveryReversesAndUpdatesPlanReference() throws Exception
    {
        var fixture = fixture();
        var delivery = create(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = new InvestmentPlan("Plan");

        entry.setGeneratedByPlanKey(plan.getPlanKey());
        entry.setPlanExecutionDate(DATE_TIME.toLocalDate());
        entry.setPreferredViewKind(InvestmentPlan.LedgerExecutionViewKind.PORTFOLIO.name());
        fixture.client().addPlan(plan);

        var reversed = converter(fixture).reverse(pair(fixture, delivery));

        assertThat(plan.getLedgerExecutionRefs(), is(List.of()));
        assertThat(entry.getGeneratedByPlanKey(), is(plan.getPlanKey()));
        assertThat(entry.getPlanExecutionDate(), is(DATE_TIME.toLocalDate()));
        assertThat(entry.getPreferredViewKind(), is(InvestmentPlan.LedgerExecutionViewKind.PORTFOLIO.name()));
        assertSame(reversed, plan.getTransactions(fixture.client()).get(0).getTransaction());

        var loaded = loadXml(saveXml(fixture.client()));
        assertThat(((PortfolioTransaction) loaded.getPlans().get(0).getTransactions(loaded).get(0).getTransaction())
                        .getType(),
                        is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
    }

    private void assertReversesDelivery(PortfolioTransaction.Type sourceType, LedgerEntryType targetEntryType,
                    PortfolioTransaction.Type targetTransactionType, LedgerProjectionRole sourceProjectionRole,
                    LedgerProjectionRole targetProjectionRole, long expectedAmount) throws Exception
    {
        var fixture = fixture();
        var delivery = create(fixture, sourceType);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var securityPostingUUID = posting(entry, LedgerPostingType.SECURITY).getUUID();
        var unitSnapshots = unitSnapshots(entry);

        var reversed = converter(fixture).reverse(pair(fixture, delivery));
        var targetProjectionUUID = projection(entry, targetProjectionRole).getRuntimeProjectionId();

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(targetEntryType));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getAmount(), is(expectedAmount));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getCurrency(), is(CurrencyUnit.EUR));
        assertThat(unitSnapshots(entry), is(unitSnapshots));
        assertSame(fixture.portfolio(), projection(entry, targetProjectionRole).getPortfolio());

        assertThat(fixture.portfolio().getTransactions(), is(List.of(reversed)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(reversed, fixture.client().getAllTransactions().get(0).getTransaction());
        assertThat(reversed.getUUID(), is(targetProjectionUUID));
        assertThat(reversed, instanceOf(LedgerBackedTransaction.class));
        assertThat(reversed.getType(), is(targetTransactionType));
        assertThat(reversed.getCrossEntry(), is(nullValue()));
        assertThat(reversed.getDateTime(), is(DATE_TIME));
        assertThat(reversed.getNote(), is("note"));
        assertThat(reversed.getSource(), is("source"));
        assertSame(fixture.security(), reversed.getSecurity());
        assertThat(reversed.getShares(), is(Values.Share.factorize(5)));
        assertThat(reversed.getAmount(), is(expectedAmount));
        assertThat(reversed.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(reversed.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120))));
        assertThat(reversed.getUnit(Unit.Type.GROSS_VALUE).orElseThrow().getAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120))));
        assertThat(reversed.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR,
                        Values.Amount.factorize(3))));
        assertThat(reversed.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR,
                        Values.Amount.factorize(4))));
        assertValid(fixture.client());

        assertRoundtrip(loadXml(saveXml(fixture.client())), entryUUID, securityPostingUUID, targetProjectionUUID,
                        targetEntryType, targetTransactionType, targetProjectionRole, expectedAmount);
        assertRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), entryUUID, securityPostingUUID, targetProjectionUUID,
                        targetEntryType, targetTransactionType, targetProjectionRole, expectedAmount);
    }

    private void assertRoundtrip(Client client, String entryUUID, String securityPostingUUID, String projectionUUID,
                    LedgerEntryType entryType, PortfolioTransaction.Type transactionType,
                    LedgerProjectionRole projectionRole, long expectedAmount)
    {
        assertThat(client.getAccounts().get(0).getTransactions().isEmpty(), is(true));
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));

        var entry = client.getLedger().getEntries().get(0);
        var transaction = client.getPortfolios().get(0).getTransactions().get(0);

        assertThat(entry.getType(), is(entryType));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getAmount(), is(expectedAmount));
        assertThat(projection(entry, projectionRole).getRole(), is(projectionRole));
        assertThat(((LedgerBackedTransaction) transaction).getLedgerProjectionRole(), is(projectionRole));
        assertThat(transaction.getType(), is(transactionType));
        assertThat(transaction.getCrossEntry(), is(nullValue()));
        assertThat(transaction.getAmount(), is(expectedAmount));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120))));
        assertValid(client);
    }

    private <T extends Throwable> void assertRejectsWithoutMutation(Fixture fixture, ThrowingRunnable runnable,
                    Class<T> expectedType)
    {
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(expectedType, runnable::run);
        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    private PortfolioTransaction create(Fixture fixture, PortfolioTransaction.Type type)
    {
        return create(fixture, type, null, null);
    }

    private PortfolioTransaction create(Fixture fixture, PortfolioTransaction.Type type, Money forexAmount,
                    BigDecimal exchangeRate)
    {
        return new LedgerDeliveryTransactionCreator(fixture.client()).create(fixture.portfolio(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(), Values.Share.factorize(5),
                        forexAmount, exchangeRate, units(), "note", "source");
    }

    private List<Unit> units()
    {
        return List.of(
                        new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)), EXCHANGE_RATE),
                        new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4))),
                        new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(240)), EXCHANGE_RATE));
    }

    private LedgerDeliveryDirectionConverter converter(Fixture fixture)
    {
        return new LedgerDeliveryDirectionConverter(fixture.client());
    }

    private TransactionPair<PortfolioTransaction> pair(Fixture fixture, PortfolioTransaction transaction)
    {
        return new TransactionPair<>(fixture.portfolio(), transaction);
    }

    private name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type).findFirst().orElseThrow();
    }

    private List<PostingSnapshot> unitSnapshots(LedgerEntry entry)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.FEE
                        || posting.getType() == LedgerPostingType.TAX
                        || posting.getType() == LedgerPostingType.GROSS_VALUE).map(PostingSnapshot::capture).toList();
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-delivery-direction-converter", ".xml");
        try
        {
            ClientFactory.save(client, file);
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client loadXml(String xml) throws IOException
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] saveProtobuf(Client client) throws IOException
    {
        return ProtobufTestUtilities.save(client);
    }

    private Client loadProtobuf(byte[] bytes) throws IOException
    {
        return ProtobufTestUtilities.load(bytes);
    }

    private void assertValid(Client client)
    {
        var result = LedgerStructuralValidator.validate(client.getLedger());

        assertThat(result.format(), result.isOK(), is(true));
    }

    private Fixture fixture()
    {
        var client = new Client();
        var account = new Account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);
        account.setUpdatedAt(Instant.now());
        portfolio.setUpdatedAt(Instant.now());
        security.setUpdatedAt(Instant.now());
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security);
    }

    @FunctionalInterface
    private interface ThrowingRunnable
    {
        void run() throws Exception;
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }

    private record Snapshot(List<EntrySnapshot> entries, List<String> accountTransactions,
                    List<String> portfolioTransactions, List<String> allTransactions)
    {
        static Snapshot capture(Client client)
        {
            return new Snapshot(client.getLedger().getEntries().stream().map(EntrySnapshot::capture).toList(),
                            client.getAccounts().stream().flatMap(account -> account.getTransactions().stream())
                                            .map(Transaction::getUUID).toList(),
                            client.getPortfolios().stream().flatMap(portfolio -> portfolio.getTransactions().stream())
                                            .map(Transaction::getUUID).toList(),
                            client.getAllTransactions().stream().map(pair -> pair.getTransaction().getUUID())
                                            .toList());
        }
    }

    private record EntrySnapshot(String uuid, LedgerEntryType type, List<PostingSnapshot> postings,
                    List<ProjectionSnapshot> projections)
    {
        static EntrySnapshot capture(LedgerEntry entry)
        {
            List<ProjectionSnapshot> projections;
            try
            {
                projections = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry)
                                .stream().map(ProjectionSnapshot::capture).toList();
            }
            catch (IllegalArgumentException e)
            {
                projections = List.of();
            }

            return new EntrySnapshot(entry.getUUID(), entry.getType(),
                            entry.getPostings().stream().map(PostingSnapshot::capture).toList(),
                            projections);
        }
    }

    private record PostingSnapshot(String uuid, LedgerPostingType type, Long amount, String currency, Long forexAmount,
                    String forexCurrency, BigDecimal exchangeRate, Security security, Long shares, Account account,
                    Portfolio portfolio)
    {
        static PostingSnapshot capture(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount(), posting.getPortfolio());
        }
    }

    private record ProjectionSnapshot(String uuid, LedgerProjectionRole role, Account account, Portfolio portfolio,
                    String primaryPostingId, String groupKey)
    {
        static ProjectionSnapshot capture(name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor projection)
        {
            return new ProjectionSnapshot(projection.getRuntimeProjectionId(), projection.getRole(), projection.getAccount(),
                            projection.getPortfolio(), projection.getPrimaryPosting().getUUID(),
                            projection.getPrimaryPosting().getGroupKey());
        }
    }
}

