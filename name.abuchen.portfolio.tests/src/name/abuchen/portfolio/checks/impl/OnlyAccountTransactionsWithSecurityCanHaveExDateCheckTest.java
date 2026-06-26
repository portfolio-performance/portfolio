package name.abuchen.portfolio.checks.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator.IssueCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests repair behavior for account transactions that carry an ex-date without a security.
 * These tests make sure legacy rows clear the field and ledger-backed rows remove only the ex-date fact.
 */
@SuppressWarnings("nls")
public class OnlyAccountTransactionsWithSecurityCanHaveExDateCheckTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2025, 12, 31, 0, 0);

    /**
     * Verifies that the legacy ex-date repair keeps its existing behavior.
     * An account transaction without security must simply lose the invalid ex-date.
     */
    @Test
    public void testLegacyAccountTransactionExDateWithoutSecurityIsCleared()
    {
        var client = new Client();
        var account = new Account();
        var transaction = new AccountTransaction(DATE_TIME, CurrencyUnit.EUR, Values.Amount.factorize(10), null,
                        AccountTransaction.Type.DIVIDENDS);

        client.addAccount(account);
        transaction.setExDate(EX_DATE);
        account.addTransaction(transaction);

        new OnlyAccountTransactionsWithSecurityCanHaveExDateCheck().execute(client);

        assertNull(transaction.getExDate());
    }

    /**
     * Verifies that an invalid ex-date on a ledger-backed booking is removed as a single fact.
     * The booking itself stays alive; no security or dividend structure is inferred.
     */
    @Test
    public void testLedgerBackedExDateWithoutSecurityClearsOnlyExDateFact()
    {
        var client = new Client();
        var account = new Account();
        var entry = new LedgerEntry("entry");
        var posting = new LedgerPosting("posting");
        var projection = new LedgerProjectionRef("projection");

        client.addAccount(account);

        entry.setType(LedgerEntryType.DIVIDENDS);
        entry.setDateTime(DATE_TIME);

        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(10));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE, EX_DATE));
        entry.addPosting(posting);

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(account);
        entry.addProjectionRef(projection);

        client.getLedger().addEntry(entry);
        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);

        assertTrue(transaction instanceof LedgerBackedTransaction);
        assertThat(transaction.getExDate(), is(EX_DATE));

        var entryUUID = entry.getUUID();
        var projectionUUID = projection.getUUID();
        var projectionRole = projection.getRole();

        new OnlyAccountTransactionsWithSecurityCanHaveExDateCheck().execute(client);

        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(client.getLedger().getEntries().get(0).getUUID(), is(entryUUID));
        assertThat(entry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(entry.getProjectionRefs().get(0).getRole(), is(projectionRole));
        assertFalse(posting.getParameters().stream()
                        .anyMatch(parameter -> parameter.getType() == LedgerParameterType.EX_DATE));
        assertThat(account.getTransactions().size(), is(1));

        var refreshed = account.getTransactions().get(0);

        assertTrue(refreshed instanceof LedgerBackedTransaction);
        assertThat(((LedgerBackedTransaction) refreshed).getLedgerProjectionRef().getUUID(), is(projectionUUID));
        assertNull(refreshed.getExDate());

        var validation = LedgerStructuralValidator.validate(client.getLedger());

        assertFalse(validation.hasIssue(IssueCode.EX_DATE_SECURITY_REQUIRED));
        assertTrue(validation.hasIssue(IssueCode.DIVIDEND_SECURITY_REQUIRED));
    }

    /**
     * Verifies that clearing an invalid ledger-backed ex-date is stable after XML save/load.
     * The ledger entry and projection remain present, but the ex-date fact does not come back.
     */
    @Test
    public void testLedgerBackedExDateClearSurvivesXmlReload() throws Exception
    {
        var client = new Client();
        var account = new Account();
        var entry = new LedgerEntry("entry");
        var posting = new LedgerPosting("posting");
        var projection = new LedgerProjectionRef("projection");

        client.addAccount(account);

        entry.setType(LedgerEntryType.FEES);
        entry.setDateTime(DATE_TIME);

        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(10));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE, EX_DATE));
        entry.addPosting(posting);

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(account);
        entry.addProjectionRef(projection);

        client.getLedger().addEntry(entry);
        LedgerProjectionService.materialize(client);

        new OnlyAccountTransactionsWithSecurityCanHaveExDateCheck().execute(client);

        assertFalse(LedgerStructuralValidator.validate(client.getLedger()).hasIssue(
                        IssueCode.EX_DATE_SECURITY_REQUIRED));

        var loaded = reloadXml(client);

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getLedger().getEntries().get(0).getUUID(), is(entry.getUUID()));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertNull(loaded.getAccounts().get(0).getTransactions().get(0).getExDate());
    }

    private Client reloadXml(Client client) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(saveXml(client).getBytes(StandardCharsets.UTF_8)));
    }

    private String saveXml(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-ex-date-repair-policy", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$

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
}
