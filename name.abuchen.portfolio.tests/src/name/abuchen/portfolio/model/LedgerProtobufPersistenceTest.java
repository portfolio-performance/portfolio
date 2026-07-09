package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.model.proto.v1.PTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerProtobufPersistenceTest
{
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testLegacyTransactionsRemainLegacyAndDoNotPopulateLedger() throws Exception
    {
        var client = fixture();
        var account = client.getAccounts().get(0);
        var portfolio = client.getPortfolios().get(0);
        var accountTransaction = legacyDeposit();
        var portfolioTransaction = legacyDelivery(client.getSecurities().get(0));

        account.addTransaction(accountTransaction);
        portfolio.addTransaction(portfolioTransaction);

        var bytes = ProtobufTestUtilities.save(client);
        var persisted = persistedClient(bytes);

        assertThat(persisted.getTransactionsCount(), is(2));
        assertFalse(persisted.getTransactions(0).getLedgerProjection());
        assertFalse(persisted.getTransactions(1).getLedgerProjection());

        var loaded = ProtobufTestUtilities.load(bytes);

        assertTrue(loaded.getLedger().getEntries().isEmpty());
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(AccountTransaction.class));
        assertFalse(loaded.getAccounts().get(0).getTransactions().get(0) instanceof LedgerBackedTransaction);
        assertThat(loaded.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getPortfolios().get(0).getTransactions().get(0), instanceOf(PortfolioTransaction.class));
        assertFalse(loaded.getPortfolios().get(0).getTransactions().get(0) instanceof LedgerBackedTransaction);
    }

    @Test
    public void testCorporateActionLedgerProtobufRoundtripPersistsFlaggedRuntimeProjections() throws Exception
    {
        var client = fixture();
        var account = client.getAccounts().get(0);
        var portfolio = client.getPortfolios().get(0);
        var security = client.getSecurities().get(0);

        LedgerNativeEntryAssembler.corporateAction(client) //
                        .kind(CorporateActionKind.CASH_DISTRIBUTION) //
                        .date(DATE) //
                        .securityContext("context-1", "cash-1", portfolio, security) //
                        .cash("cash-1", "cash-1", account, money(10)) //
                        .buildAndAdd();
        LedgerProjectionService.materialize(client);

        var bytes = ProtobufTestUtilities.save(client);
        var persisted = persistedClient(bytes);

        assertThat(persisted.getLedger().getEntriesCount(), is(1));
        assertThat(persisted.getTransactionsCount(), is(1));
        assertThat(persisted.getTransactions(0).getType(), is(PTransaction.Type.DIVIDEND));
        assertTrue(persisted.getTransactions(0).getLedgerProjection());

        var loaded = ProtobufTestUtilities.load(bytes);

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.CORPORATE_ACTION));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertSame(loaded.getLedger().getEntries().get(0),
                        ((LedgerBackedTransaction) loaded.getAccounts().get(0).getTransactions().get(0))
                                        .getLedgerEntry());
        assertTrue(loaded.getPortfolios().get(0).getTransactions().isEmpty());

        var secondPersisted = persistedClient(ProtobufTestUtilities.save(loaded));
        assertThat(secondPersisted.getTransactionsCount(), is(1));
        assertTrue(secondPersisted.getTransactions(0).getLedgerProjection());
    }

    @Test
    public void testLedgerProjectionTransactionWithoutLedgerTruthFails() throws Exception
    {
        var transaction = PTransaction.newBuilder() //
                        .setType(PTransaction.Type.DEPOSIT) //
                        .setLedgerProjection(true) //
                        .build();
        var client = PClient.newBuilder().addTransactions(transaction).build();

        var exception = assertThrows(UnsupportedOperationException.class,
                        () -> ProtobufTestUtilities.load(wire(client)));

        assertThat(exception.getMessage(), is("Ledger projection transaction exists but no PLedger source exists"));
    }

    private static Client fixture()
    {
        var client = new Client();
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setName("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(UPDATED_AT);
        portfolio.setName("Portfolio");
        portfolio.setReferenceAccount(account);
        portfolio.setUpdatedAt(UPDATED_AT);
        security.setUpdatedAt(UPDATED_AT);
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return client;
    }

    private static AccountTransaction legacyDeposit()
    {
        var transaction = new AccountTransaction(AccountTransaction.Type.DEPOSIT);

        transaction.setDateTime(DATE);
        transaction.setUpdatedAt(UPDATED_AT);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(10));

        return transaction;
    }

    private static PortfolioTransaction legacyDelivery(Security security)
    {
        var transaction = new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND);

        transaction.setDateTime(DATE);
        transaction.setUpdatedAt(UPDATED_AT);
        transaction.setSecurity(security);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(10));
        transaction.setShares(Values.Share.factorize(1));

        return transaction;
    }

    private static Money money(long amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static PClient persistedClient(byte[] bytes) throws Exception
    {
        return PClient.parseFrom(Arrays.copyOfRange(bytes, 6, bytes.length));
    }

    private static byte[] wire(PClient client) throws Exception
    {
        var stream = new ByteArrayOutputStream();
        stream.write(new byte[] { 'P', 'P', 'P', 'B', 'V', '1' });
        client.writeTo(stream);
        return stream.toByteArray();
    }
}
