package name.abuchen.portfolio.checks.impl;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

/**
 * Tests check and repair choices for broken cross-entry and transaction facts.
 * These tests make sure damaged business facts are diagnosed without reconstructing currency, security, or transfer structure by guessing.
 */
public class CrossEntryCheckTest
{
    private Client client;
    private Account account;
    private Portfolio portfolio;
    private Security security;

    @Before
    public void setupClient()
    {
        client = new Client();
        account = new Account();
        client.addAccount(account);
        portfolio = new Portfolio();
        client.addPortfolio(portfolio);
        security = new Security();
        client.addSecurity(security);
    }

    /**
     * Verifies that an empty client has no cross-entry or repair issues.
     * The check must not report synthetic ledger or legacy problems.
     */
    @Test
    public void testEmptyClient()
    {
        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
    }

    /**
     * Verifies that a sell missing its account-side booking is diagnosed.
     * The issue represents a broken business booking and must not be silently ignored.
     */
    @Test
    public void testMissingSellInAccountIssue()
    {
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 1, security, 1,
                        PortfolioTransaction.Type.BUY, 1, 0));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingBuySellAccountIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));
        assertOnlyDeleteFix(issues.get(0));

        applyFixes(client, issues);
    }

    /**
     * Verifies that a buy missing its account-side booking is diagnosed.
     * The check must catch broken cross-entry structure for legacy buy/sell rows.
     */
    @Test
    public void testMissingBuyInAccountIssue()
    {
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 1, security, 1,
                        PortfolioTransaction.Type.SELL, 1, 0));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingBuySellAccountIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));
        assertOnlyDeleteFix(issues.get(0));

        applyFixes(client, issues);
    }

    /**
     * Verifies that complete buy/sell entries are not reported as damaged.
     * A valid cross entry must not receive any repair or delete suggestion.
     */
    @Test
    public void testThatCorrectBuySellEntriesAreNotReported()
    {
        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.now());
        entry.setSecurity(security);
        entry.setShares(1);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(100);
        entry.insert();

        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
    }

    /**
     * Verifies that valid ledger-backed cross-entry families are not reported as broken.
     * Runtime projections derived from the ledger must be accepted when the ledger entry is structurally valid.
     */
    @Test
    public void testThatLedgerBackedCrossEntryFamiliesAreNotReported()
    {
        Account secondAccount = new Account();
        client.addAccount(secondAccount);
        Portfolio secondPortfolio = new Portfolio();
        client.addPortfolio(secondPortfolio);

        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        new LedgerBuySellTransactionCreator(client).create(portfolio, account, PortfolioTransaction.Type.BUY, date,
                        Values.Amount.factorize(100), CurrencyUnit.EUR, security, Values.Share.factorize(5),
                        List.of(), "buy note", "buy source");
        new LedgerBuySellTransactionCreator(client).create(portfolio, account, PortfolioTransaction.Type.SELL,
                        date.plusDays(1), Values.Amount.factorize(110), CurrencyUnit.EUR, security,
                        Values.Share.factorize(5), List.of(), "sell note", "sell source");
        new LedgerAccountTransferTransactionCreator(client).create(account, secondAccount, date.plusDays(2),
                        Values.Amount.factorize(50), CurrencyUnit.EUR, Values.Amount.factorize(50), CurrencyUnit.EUR,
                        null, null, "transfer note", "transfer source");
        new LedgerPortfolioTransferTransactionCreator(client).create(portfolio, secondPortfolio, security,
                        date.plusDays(3), Values.Share.factorize(2), Values.Amount.factorize(40), CurrencyUnit.EUR,
                        "portfolio transfer note", "portfolio transfer source");

        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
        assertThat(client.getAllTransactions().size(), is(4));
    }

    /**
     * Verifies that damaged ledger-backed cross-entry rows offer only a ledger-aware delete fix.
     * The check must not rebuild cross entries from runtime projections.
     */
    @Test
    public void testLedgerBackedMissingCrossEntryIssuesOfferOnlyLedgerAwareDeleteFix()
    {
        Account secondAccount = new Account();
        client.addAccount(secondAccount);
        Portfolio secondPortfolio = new Portfolio();
        client.addPortfolio(secondPortfolio);

        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        BuySellEntry buy = new LedgerBuySellTransactionCreator(client).create(portfolio, account,
                        PortfolioTransaction.Type.BUY, date, Values.Amount.factorize(100), CurrencyUnit.EUR, security,
                        Values.Share.factorize(5), List.of(), "note", "source");
        var accountTransfer = new LedgerAccountTransferTransactionCreator(client).create(account, secondAccount,
                        date.plusDays(1), Values.Amount.factorize(50), CurrencyUnit.EUR,
                        Values.Amount.factorize(50), CurrencyUnit.EUR, null, null, "note", "source");
        var portfolioTransfer = new LedgerPortfolioTransferTransactionCreator(client).create(portfolio,
                        secondPortfolio, security, date.plusDays(2), Values.Share.factorize(2),
                        Values.Amount.factorize(40), CurrencyUnit.EUR, "note", "source");

        assertOnlyDeleteFix(new MissingBuySellAccountIssue(client, portfolio, buy.getPortfolioTransaction()));
        assertOnlyDeleteFix(new MissingBuySellPortfolioIssue(client, account, buy.getAccountTransaction()));
        assertOnlyDeleteFix(new MissingAccountTransferIssue(client, account, accountTransfer.getSourceTransaction()));
        assertOnlyDeleteFix(new MissingPortfolioTransferIssue(client, portfolio,
                        portfolioTransfer.getSourceTransaction()));
    }

    /**
     * Verifies that deleting a damaged ledger-backed cross entry removes the whole ledger entry.
     * All derived account and portfolio projections must disappear together.
     */
    @Test
    public void testLedgerBackedDeleteFixDeletesWholeLedgerEntryAndAllProjections()
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        BuySellEntry buy = new LedgerBuySellTransactionCreator(client).create(portfolio, account,
                        PortfolioTransaction.Type.BUY, date, Values.Amount.factorize(100), CurrencyUnit.EUR, security,
                        Values.Share.factorize(5), List.of(), "note", "source");

        new DeleteTransactionFix<AccountTransaction>(client, account, buy.getAccountTransaction()).execute();

        assertThat(client.getLedger().getEntries().size(), is(0));
        assertThat(account.getTransactions().size(), is(0));
        assertThat(portfolio.getTransactions().size(), is(0));
        assertThat(client.getAllTransactions().size(), is(0));
    }

    /**
     * Verifies that a delete fix also cleans plan execution refs for the deleted ledger entry.
     * Save/load must not bring back a generated booking through a stale plan reference.
     */
    @Test
    public void testLedgerBackedDeleteFixRemovesInvestmentPlanRefs() throws IOException
    {
        security.addPrice(new SecurityPrice(LocalDate.now().minusMonths(2), Values.Quote.factorize(10)));
        InvestmentPlan plan = createBuyPlan("deleted plan");
        InvestmentPlan unrelatedPlan = createBuyPlan("unrelated plan");
        client.addPlan(plan);
        client.addPlan(unrelatedPlan);

        var generated = plan.generateTransactions(client, new TestCurrencyConverter());
        var unrelatedGenerated = unrelatedPlan.generateTransactions(client, new TestCurrencyConverter());
        var deletedPortfolioProjection = (PortfolioTransaction) generated.get(0).getTransaction();
        var deletedEntryUUID = ((LedgerBackedTransaction) deletedPortfolioProjection).getLedgerEntry().getUUID();
        var accountProjection = (AccountTransaction) deletedPortfolioProjection.getCrossEntry()
                        .getCrossTransaction(deletedPortfolioProjection);
        var unrelatedEntryUUID = ((LedgerBackedTransaction) unrelatedGenerated.get(0).getTransaction()).getLedgerEntry()
                        .getUUID();

        AccountTransaction legacyTransaction = new AccountTransaction(LocalDateTime.of(2026, 6, 15, 11, 0),
                        CurrencyUnit.EUR, Values.Amount.factorize(1), null, AccountTransaction.Type.DEPOSIT);
        account.addTransaction(legacyTransaction);

        new DeleteTransactionFix<AccountTransaction>(client, account, accountProjection).execute();

        assertFalse(client.getLedger().getEntries().stream().anyMatch(entry -> entry.getUUID().equals(deletedEntryUUID)));
        assertThat(client.getLedger().getEntries().stream().filter(entry -> entry.getUUID().equals(unrelatedEntryUUID))
                        .count(), is(1L));
        assertThat(plan.getLedgerExecutionRefs(), is(List.of()));
        assertThat(plan.getTransactions(client), is(List.of()));
        assertThat(unrelatedPlan.getLedgerExecutionRefs().size(), is(1));
        assertThat(unrelatedPlan.getTransactions(client).size(), is(1));
        assertThat(account.getTransactions(), hasItem(legacyTransaction));
        assertFalse(account.getTransactions().contains(accountProjection));
        assertFalse(portfolio.getTransactions().contains(deletedPortfolioProjection));
    }

    /**
     * Verifies that missing currency facts on ledger-backed bookings are delete-only.
     * The fix must delete the ledger entry instead of guessing or setting a transaction currency.
     */
    @Test
    public void testLedgerBackedMissingCurrencyIssueOffersDeleteOnlyAndDeleteSurvivesXmlReload() throws Exception
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        AccountTransaction transaction = new LedgerAccountOnlyTransactionCreator(client).create(account,
                        AccountTransaction.Type.FEES, date, Values.Amount.factorize(10), CurrencyUnit.EUR, security,
                        List.of(), "fee note", "fee source");
        String entryUUID = ledgerEntry(transaction).getUUID();

        primaryPosting(transaction).setCurrency(null);

        List<Issue> issues = new TransactionCurrencyCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertOnlyDeleteFix(issues.get(0)).execute();

        assertEntryDeleted(entryUUID);
        assertThat(account.getTransactions().size(), is(0));

        Client loaded = reloadXml(client);
        assertThat(loaded.getLedger().getEntries().size(), is(0));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(0));
    }

    /**
     * Verifies that missing security facts on ledger-backed portfolio rows are delete-only.
     * The fix must not infer a security from the projected transaction or owner list.
     */
    @Test
    public void testLedgerBackedMissingSecurityIssueOffersDeleteOnlyAndDeletesLedgerEntry()
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        PortfolioTransaction transaction = new LedgerDeliveryTransactionCreator(client).create(portfolio,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, date, Values.Amount.factorize(20),
                        CurrencyUnit.EUR, security, Values.Share.factorize(2), null, null, List.of(), "delivery note",
                        "delivery source");
        String entryUUID = ledgerEntry(transaction).getUUID();

        primaryPosting(transaction).setSecurity(null);

        List<Issue> issues = new PortfolioTransactionWithoutSecurityCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertOnlyDeleteFix(issues.get(0)).execute();

        assertEntryDeleted(entryUUID);
        assertThat(portfolio.getTransactions().size(), is(0));
    }

    /**
     * Verifies that security-backed account bookings with missing security facts are delete-only.
     * Dividend-like facts must not be reconstructed automatically.
     */
    @Test
    public void testLedgerBackedDividendSecurityIssueOffersDeleteOnly()
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        AccountTransaction transaction = new LedgerAccountOnlyTransactionCreator(client).create(account,
                        AccountTransaction.Type.INTEREST, date, Values.Amount.factorize(30), CurrencyUnit.EUR,
                        security, List.of(), "interest note", "interest source");
        String entryUUID = ledgerEntry(transaction).getUUID();

        ((LedgerBackedTransaction) transaction).getLedgerEntry().setType(LedgerEntryType.DIVIDENDS);
        primaryPosting(transaction).setSecurity(null);

        List<Issue> issues = new DividendsAndInterestCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertOnlyDeleteFix(issues.get(0)).execute();

        assertEntryDeleted(entryUUID);
        assertThat(account.getTransactions().size(), is(0));
    }

    /**
     * Verifies that missing buy/sell projections in a ledger entry are delete-only.
     * The repair path must not recreate the missing projection as a second source of truth.
     */
    @Test
    public void testLedgerBackedMissingBuySellProjectionIssueOffersDeleteOnlyAndDeletesLedgerEntry() throws Exception
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        BuySellEntry buy = new LedgerBuySellTransactionCreator(client).create(portfolio, account,
                        PortfolioTransaction.Type.BUY, date, Values.Amount.factorize(100), CurrencyUnit.EUR, security,
                        Values.Share.factorize(5), List.of(), "note", "source");
        LedgerEntry entry = ledgerEntry(buy.getAccountTransaction());
        String entryUUID = entry.getUUID();

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.PORTFOLIO));
        rematerializeLedgerProjections();

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingBuySellPortfolioIssue.class)));
        assertOnlyDeleteFix(issues.get(0)).execute();

        assertEntryDeleted(entryUUID);
        assertThat(account.getTransactions().size(), is(0));
        assertThat(portfolio.getTransactions().size(), is(0));
        assertThat(reloadXml(client).getLedger().getEntries().size(), is(0));
    }

    /**
     * Verifies that missing transfer-side projections in ledger-backed transfers are delete-only.
     * Transfer source and target structure must not be inferred from the remaining runtime side.
     */
    @Test
    public void testLedgerBackedMissingTransferProjectionIssuesOfferDeleteOnlyAndDeleteLedgerEntry()
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        Account targetAccount = new Account();
        client.addAccount(targetAccount);

        var accountTransfer = new LedgerAccountTransferTransactionCreator(client).create(account, targetAccount, date,
                        Values.Amount.factorize(50), CurrencyUnit.EUR, Values.Amount.factorize(50), CurrencyUnit.EUR,
                        null, null, "account transfer note", "account transfer source");
        LedgerEntry accountEntry = ledgerEntry(accountTransfer.getSourceTransaction());

        accountEntry.removeProjectionRef(projection(accountEntry, LedgerProjectionRole.TARGET_ACCOUNT));
        rematerializeLedgerProjections();

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingAccountTransferIssue.class)));
        assertOnlyDeleteFix(issues.get(0)).execute();

        assertEntryDeleted(accountEntry.getUUID());
        assertThat(account.getTransactions().size(), is(0));
        assertThat(targetAccount.getTransactions().size(), is(0));

        Portfolio targetPortfolio = new Portfolio();
        client.addPortfolio(targetPortfolio);
        var portfolioTransfer = new LedgerPortfolioTransferTransactionCreator(client).create(portfolio,
                        targetPortfolio, security, date.plusDays(1), Values.Share.factorize(2),
                        Values.Amount.factorize(40), CurrencyUnit.EUR, "portfolio transfer note",
                        "portfolio transfer source");
        LedgerEntry portfolioEntry = ledgerEntry(portfolioTransfer.getSourceTransaction());

        portfolioEntry.removeProjectionRef(projection(portfolioEntry, LedgerProjectionRole.TARGET_PORTFOLIO));
        rematerializeLedgerProjections();

        issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingPortfolioTransferIssue.class)));
        assertOnlyDeleteFix(issues.get(0)).execute();

        assertEntryDeleted(portfolioEntry.getUUID());
        assertThat(portfolio.getTransactions().size(), is(0));
        assertThat(targetPortfolio.getTransactions().size(), is(0));
    }

    /**
     * Verifies that legacy damaged business facts now follow the same delete-only policy.
     * Currency, security, and transfer facts are not repaired by guessing replacement values.
     */
    @Test
    public void testLegacyFactRepairIssuesOfferOnlyDeleteFixes()
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        AccountTransaction missingCurrency = new AccountTransaction(date, CurrencyUnit.EUR,
                        Values.Amount.factorize(10), null, AccountTransaction.Type.FEES);
        account.addTransaction(missingCurrency);
        missingCurrency.setCurrencyCode(null);

        PortfolioTransaction missingSecurity = new PortfolioTransaction(date.plusDays(1), CurrencyUnit.EUR,
                        Values.Amount.factorize(20), security, Values.Share.factorize(2),
                        PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0);
        portfolio.addTransaction(missingSecurity);
        missingSecurity.setSecurity(null);

        AccountTransaction dividendWithoutSecurity = new AccountTransaction(date.plusDays(2), CurrencyUnit.EUR,
                        Values.Amount.factorize(30), null, AccountTransaction.Type.DIVIDENDS);
        account.addTransaction(dividendWithoutSecurity);

        List<Issue> currencyIssues = new TransactionCurrencyCheck().execute(client);
        List<Issue> securityIssues = new PortfolioTransactionWithoutSecurityCheck().execute(client);
        List<Issue> dividendIssues = new DividendsAndInterestCheck().execute(client);

        assertThat(currencyIssues.size(), is(1));
        assertOnlyDeleteFix(currencyIssues.get(0));
        assertThat(securityIssues.size(), is(1));
        assertOnlyDeleteFix(securityIssues.get(0));
        assertThat(dividendIssues.size(), is(1));
        assertOnlyDeleteFix(dividendIssues.get(0));
    }

    /**
     * Verifies that malformed ledger-backed exchange-rate facts offer only deletion.
     * Because amount, currency, and rate belong together, no automatic forex repair is attempted.
     */
    @Test
    public void testLedgerBackedExchangeRateMalformedFactsOfferDeleteOnlyAndDeleteLedgerEntry() throws Exception
    {
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);
        AccountTransaction negativeExchangeRate = new LedgerAccountOnlyTransactionCreator(client).create(account,
                        AccountTransaction.Type.FEES, date, Values.Amount.factorize(40), CurrencyUnit.EUR,
                        security,
                        List.of(new Transaction.Unit(Transaction.Unit.Type.FEE,
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1)), BigDecimal.ONE)),
                        "negative rate note", "negative rate source");

        ((LedgerBackedTransaction) negativeExchangeRate).getLedgerEntry().getPostings().stream()
                        .filter(posting -> posting.getType() == LedgerPostingType.FEE).findFirst().orElseThrow()
                        .setExchangeRate(BigDecimal.valueOf(-1));
        String entryUUID = ledgerEntry(negativeExchangeRate).getUUID();

        assertThat(new TransactionCurrencyCheck().execute(client).size(), is(0));
        assertThat(new PortfolioTransactionWithoutSecurityCheck().execute(client).size(), is(0));
        assertThat(new DividendsAndInterestCheck().execute(client).size(), is(0));

        List<Issue> issues = new NegativeExchangeRateCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertOnlyDeleteFix(issues.get(0)).execute();

        assertEntryDeleted(entryUUID);
        assertThat(account.getTransactions().size(), is(0));
        assertThat(reloadXml(client).getLedger().getEntries().size(), is(0));
    }

    private LedgerPosting primaryPosting(AccountTransaction transaction)
    {
        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var primaryPostingUUID = ledgerBacked.getLedgerProjectionRef().getPrimaryPostingUUID();

        return ledgerBacked.getLedgerEntry().getPostings().stream()
                        .filter(posting -> posting.getUUID().equals(primaryPostingUUID)).findFirst().orElseThrow();
    }

    private LedgerPosting primaryPosting(PortfolioTransaction transaction)
    {
        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var primaryPostingUUID = ledgerBacked.getLedgerProjectionRef().getPrimaryPostingUUID();

        return ledgerBacked.getLedgerEntry().getPostings().stream()
                        .filter(posting -> posting.getUUID().equals(primaryPostingUUID)).findFirst().orElseThrow();
    }

    private LedgerEntry ledgerEntry(Transaction transaction)
    {
        return ((LedgerBackedTransaction) transaction).getLedgerEntry();
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow();
    }

    private void rematerializeLedgerProjections()
    {
        client.getAccounts().forEach(owner -> owner.getTransactions().removeIf(LedgerCheckSupport::isLedgerBacked));
        client.getPortfolios().forEach(owner -> owner.getTransactions().removeIf(LedgerCheckSupport::isLedgerBacked));

        LedgerProjectionService.materialize(client);
    }

    private void assertEntryDeleted(String entryUUID)
    {
        assertFalse(client.getLedger().getEntries().stream().anyMatch(entry -> entry.getUUID().equals(entryUUID)));
    }

    private Client reloadXml(Client client) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(saveXml(client).getBytes(StandardCharsets.UTF_8)));
    }

    private String saveXml(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-delete-only-repair-policy", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$

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

    /**
     * Verifies that matching buy/sell rows are reported as one damaged legacy booking.
     * The available fix is deletion, not reconstruction of the missing side.
     */
    @Test
    public void testThatMatchingBuySellEntriesAreReportedDeleteOnly()
    {
        LocalDateTime date = LocalDateTime.now();
        portfolio.addTransaction(new PortfolioTransaction(date, CurrencyUnit.EUR, 1, security, 1,
                        PortfolioTransaction.Type.SELL, 1, 0));

        account.addTransaction(new AccountTransaction(date, CurrencyUnit.EUR, 1, security, //
                        AccountTransaction.Type.SELL));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(2));
        List<Object> objects = new ArrayList<Object>(issues);
        assertThat(objects, hasItem(instanceOf(MissingBuySellAccountIssue.class)));
        assertThat(objects, hasItem(instanceOf(MissingBuySellPortfolioIssue.class)));
        issues.forEach(this::assertOnlyDeleteFix);

        applyFixes(client, issues);
    }

    /**
     * Verifies that almost matching buy/sell rows are not paired by the check.
     * The repair code must not guess cross-entry relationships from similar values.
     */
    @Test
    public void testThatAlmostMatchingBuySellEntriesAreNotMatched()
    {
        LocalDateTime date = LocalDateTime.now();
        portfolio.addTransaction(new PortfolioTransaction(date, CurrencyUnit.EUR, 1, security, 1,
                        PortfolioTransaction.Type.SELL, 1, 0));

        account.addTransaction(new AccountTransaction(date, CurrencyUnit.EUR, 2, security,
                        AccountTransaction.Type.SELL));

        List<Issue> issues = new CrossEntryCheck().execute(client);
        assertThat(issues.size(), is(2));
        List<Object> objects = new ArrayList<Object>(issues);
        assertThat(objects, hasItem(instanceOf(MissingBuySellAccountIssue.class)));
        assertThat(objects, hasItem(instanceOf(MissingBuySellPortfolioIssue.class)));
        issues.forEach(this::assertOnlyDeleteFix);

        applyFixes(client, issues);
    }

    /**
     * Verifies that a missing transfer-out side offers only deletion.
     * The check must not create a replacement transfer side from inferred owner structure.
     */
    @Test
    public void testMissingAccountTransferOutIssueOffersOnlyDeleteFix()
    {
        account.addTransaction(new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 1, security,
                        AccountTransaction.Type.TRANSFER_IN));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingAccountTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) account));
        assertOnlyDeleteFix(issues.get(0));

        applyFixes(client, issues);
    }

    /**
     * Verifies that a missing transfer-in side offers only deletion.
     * The check must not create a replacement transfer side from inferred owner structure.
     */
    @Test
    public void testMissingAccountTransferInIssueOffersOnlyDeleteFix()
    {
        account.addTransaction(new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 1, security,
                        AccountTransaction.Type.TRANSFER_OUT));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingAccountTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) account));
        assertOnlyDeleteFix(issues.get(0));

        applyFixes(client, issues);
    }

    /**
     * Verifies that account transfers are no longer reconstructed by repair fixes.
     * Broken source or target sides must be diagnosed and deleted instead of rebuilt.
     */
    @Test
    public void testThatAccountTransfersAreNotReconstructedAndOfferOnlyDeleteFixes()
    {
        Account second = new Account();
        client.addAccount(second);

        LocalDateTime date = LocalDateTime.now();
        account.addTransaction(new AccountTransaction(date, CurrencyUnit.EUR, 2, security,
                        AccountTransaction.Type.TRANSFER_IN));

        AccountTransaction umatched = new AccountTransaction(date, CurrencyUnit.EUR, 2, security,
                        AccountTransaction.Type.TRANSFER_OUT);
        account.addTransaction(umatched);

        second.addTransaction(new AccountTransaction(date, CurrencyUnit.EUR, 2, security,
                        AccountTransaction.Type.TRANSFER_OUT));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(3));
        issues.forEach(issue -> assertThat(issue, is(instanceOf(MissingAccountTransferIssue.class))));
        issues.forEach(this::assertOnlyDeleteFix);
        assertThat(account.getTransactions(), hasItem(umatched));

        applyFixes(client, issues);
    }

    /**
     * Verifies that a missing portfolio transfer-out side offers only deletion.
     * The check must not infer a replacement depot-side transfer structure.
     */
    @Test
    public void testMissingPortfolioTransferOutIssueOffersOnlyDeleteFix()
    {
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 1, security, 1,
                        PortfolioTransaction.Type.TRANSFER_IN, 1, 0));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingPortfolioTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));
        assertOnlyDeleteFix(issues.get(0));

        applyFixes(client, issues);
    }

    /**
     * Verifies that a missing portfolio transfer-in side offers only deletion.
     * The check must not infer a replacement depot-side transfer structure.
     */
    @Test
    public void testMissingPortfolioTransferInIssueOffersOnlyDeleteFix()
    {
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 1, security, 1,
                        PortfolioTransaction.Type.TRANSFER_OUT, 1, 0));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingPortfolioTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));
        assertOnlyDeleteFix(issues.get(0));

        applyFixes(client, issues);
    }

    /**
     * Verifies that portfolio transfers are no longer reconstructed by repair fixes.
     * Broken source or target sides must be diagnosed and deleted instead of rebuilt.
     */
    @Test
    public void testThatPortfolioTransfersAreNotReconstructedAndOfferOnlyDeleteFixes()
    {
        Portfolio second = new Portfolio();
        client.addPortfolio(second);

        LocalDateTime date = LocalDateTime.now();
        portfolio.addTransaction(new PortfolioTransaction(date, CurrencyUnit.EUR, 3, security, 1,
                        PortfolioTransaction.Type.TRANSFER_IN, 1, 0));

        PortfolioTransaction umatched = new PortfolioTransaction(date, CurrencyUnit.EUR, 3, security, 1,
                        PortfolioTransaction.Type.TRANSFER_OUT, 1, 0);
        portfolio.addTransaction(umatched);

        second.addTransaction(new PortfolioTransaction(date, CurrencyUnit.EUR, 3, security, 1,
                        PortfolioTransaction.Type.TRANSFER_OUT, 1, 0));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(3));
        issues.forEach(issue -> assertThat(issue, is(instanceOf(MissingPortfolioTransferIssue.class))));
        issues.forEach(this::assertOnlyDeleteFix);
        assertThat(portfolio.getTransactions(), hasItem(umatched));

        applyFixes(client, issues);
    }

    /**
     * Verifies that account transactions carrying impossible security facts offer only deletion.
     * The repair path must not clean up or reinterpret the transaction facts automatically.
     */
    @Test
    public void testThatAccountTransactionsWithoutSecurityOfferOnlyDeleteFix()
    {
        Portfolio second = new Portfolio();
        client.addPortfolio(second);

        account.addTransaction(new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 1, null,
                        AccountTransaction.Type.BUY));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0).getAvailableFixes().get(0), is(instanceOf(DeleteTransactionFix.class)));

        applyFixes(client, issues);

        ClientSnapshot.create(client, new TestCurrencyConverter(), LocalDate.now());
    }

    private void applyFixes(Client client, List<Issue> issues)
    {
        for (Issue issue : issues)
        {
            List<QuickFix> fixes = issue.getAvailableFixes();
            assertThat(fixes.isEmpty(), is(false));
            fixes.get(0).execute();
        }
        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
    }

    private QuickFix assertOnlyDeleteFix(Issue issue)
    {
        List<QuickFix> fixes = issue.getAvailableFixes();
        assertThat(fixes.size(), is(1));
        assertThat(fixes.get(0), is(instanceOf(DeleteTransactionFix.class)));
        return fixes.get(0);
    }

    private InvestmentPlan createBuyPlan(String name)
    {
        InvestmentPlan plan = new InvestmentPlan();
        plan.setName(name);
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setAccount(account);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setStart(LocalDate.now().minusMonths(1));
        plan.setInterval(12);
        plan.setAmount(Values.Amount.factorize(100));
        return plan;
    }

}
