package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerXmlPersistenceTest
{
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

        var loaded = load(save(client));

        assertTrue(loaded.getLedger().getEntries().isEmpty());
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(AccountTransaction.class));
        assertFalse(loaded.getAccounts().get(0).getTransactions().get(0) instanceof LedgerBackedTransaction);
        assertThat(loaded.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getPortfolios().get(0).getTransactions().get(0), instanceOf(PortfolioTransaction.class));
        assertFalse(loaded.getPortfolios().get(0).getTransactions().get(0) instanceof LedgerBackedTransaction);
    }

    @Test
    public void testCorporateActionLedgerXmlRoundtripDoesNotPersistRuntimeProjections() throws Exception
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

        var xml = save(client);

        assertTrue(xml.contains("<ledger>"));
        assertTrue(xml.contains("type=\"CORPORATE_ACTION\"") || xml.contains("<type>CORPORATE_ACTION</type>"));
        assertFalse(xml.contains("<account-transaction"));
        assertFalse(xml.contains("LedgerBacked"));
        assertNoLedgerUuidTruth(xml);

        var loaded = load(xml);

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.CORPORATE_ACTION));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertSame(loaded.getLedger().getEntries().get(0),
                        ((LedgerBackedTransaction) loaded.getAccounts().get(0).getTransactions().get(0))
                                        .getLedgerEntry());
    }

    @Test
    public void testSiemensEnergySpinOffExampleIsCorporateActionOnly() throws Exception
    {
        var xml = read("ledger-v6-spin-off-siemens-energy-example.xml");
        var loaded = load(xml);

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.CORPORATE_ACTION));
        assertThat(CorporateActionKind.fromEntry(loaded.getLedger().getEntries().get(0)).orElseThrow(),
                        is(CorporateActionKind.SPIN_OFF));

        assertThat(loaded.getAccounts().get(0).getTransactions().stream()
                        .filter(transaction -> transaction.getType() == AccountTransaction.Type.DEPOSIT)
                        .filter(transaction -> !(transaction instanceof LedgerBackedTransaction)).count(), is(1L));
        assertTrue(loaded.getAccounts().get(0).getTransactions().stream()
                        .anyMatch(LedgerBackedTransaction.class::isInstance));
        assertThat(loaded.getPortfolios().get(0).getTransactions().stream()
                        .filter(transaction -> transaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                        .filter(transaction -> !(transaction instanceof LedgerBackedTransaction)).count(), is(1L));
        assertTrue(loaded.getPortfolios().get(0).getTransactions().stream()
                        .anyMatch(LedgerBackedTransaction.class::isInstance));

        var roundtripXml = save(loaded);
        var roundtripLedgerXml = ledgerSection(roundtripXml);

        assertTrue(roundtripLedgerXml.contains("type=\"CORPORATE_ACTION\"") //$NON-NLS-1$
                        || roundtripLedgerXml.contains("<type>CORPORATE_ACTION</type>")); //$NON-NLS-1$
        assertFalse(roundtripLedgerXml.contains("type=\"BUY\"")); //$NON-NLS-1$
        assertFalse(roundtripLedgerXml.contains("<type>BUY</type>")); //$NON-NLS-1$
        assertFalse(roundtripLedgerXml.contains("type=\"DEPOSIT\"")); //$NON-NLS-1$
        assertFalse(roundtripLedgerXml.contains("<type>DEPOSIT</type>")); //$NON-NLS-1$
        assertNoLedgerUuidTruth(roundtripXml);

        var roundtrip = load(roundtripXml);

        assertThat(roundtrip.getLedger().getEntries().size(), is(1));
        assertThat(roundtrip.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.CORPORATE_ACTION));
        assertThat(CorporateActionKind.fromEntry(roundtrip.getLedger().getEntries().get(0)).orElseThrow(),
                        is(CorporateActionKind.SPIN_OFF));
    }

    private static String save(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-corporate-action", ".xml");

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

    private static String read(String name) throws IOException
    {
        for (var base = Path.of("").toAbsolutePath(); base != null; base = base.getParent()) //$NON-NLS-1$
        {
            var path = base.resolve(Path.of("name.abuchen.portfolio.tests", "src", "name", "abuchen", "portfolio", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                            "model", "ledger", name)); //$NON-NLS-1$ //$NON-NLS-2$

            if (Files.exists(path))
                return Files.readString(path, StandardCharsets.UTF_8);
        }

        throw new IOException("Missing test fixture: " + name); //$NON-NLS-1$
    }

    private static Client load(String xml) throws IOException
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Client fixture()
    {
        var client = new Client();
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return client;
    }

    private static AccountTransaction legacyDeposit()
    {
        var transaction = new AccountTransaction(AccountTransaction.Type.DEPOSIT);

        transaction.setDateTime(DATE);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(10));

        return transaction;
    }

    private static PortfolioTransaction legacyDelivery(Security security)
    {
        var transaction = new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND);

        transaction.setDateTime(DATE);
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

    private static void assertNoLedgerUuidTruth(String xml)
    {
        var ledgerXml = ledgerSection(xml);

        assertFalse(ledgerXml.matches("(?s).*<ledger-entry[^>]*\\buuid=.*"));
        assertFalse(ledgerXml.matches("(?s).*<ledger-posting[^>]*\\buuid=.*"));
        assertFalse(ledgerXml.contains("ProjectionMembership"));
        assertFalse(ledgerXml.contains("primaryPostingUUID"));
        assertFalse(ledgerXml.contains("postingGroupUUID"));
    }

    private static String ledgerSection(String xml)
    {
        var start = xml.indexOf("<ledger>");
        var end = xml.indexOf("</ledger>");

        assertTrue(start >= 0);
        assertTrue(end > start);

        return xml.substring(start, end);
    }
}
