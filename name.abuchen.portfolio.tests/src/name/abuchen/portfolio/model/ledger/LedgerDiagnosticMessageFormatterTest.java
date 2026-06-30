package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerDiagnosticMessageFormatterTest
{
    @Test
    public void testDiagnosticNlsTextDoesNotContainLedgerCodes()
    {
        assertFalse(Messages.LedgerDiagnosticMessageFormatterTransactionContext.isBlank());
        assertThat(Messages.LedgerStructuralValidatorPostingCurrencyRequired, containsString("{0}"));
        assertThat(Messages.LedgerRuntimeProjectionRestorerInvalidLedger, containsString("{0}"));
        assertFalse(Messages.LedgerDiagnosticMessageFormatterTransactionContext.contains("LEDGER-"));
        assertFalse(Messages.LedgerStructuralValidatorPostingCurrencyRequired.contains("LEDGER-"));
        assertFalse(Messages.LedgerRuntimeProjectionRestorerInvalidLedger.contains("LEDGER-"));
    }

    @Test
    public void testValidationFormattingAddsFullEntryContext()
    {
        var ledger = new Ledger();
        var account = new Account();
        var security = new Security("Siemens AG", CurrencyUnit.EUR);
        var entry = new LedgerEntry("entry-1");
        var posting = new LedgerPosting("posting-1");

        account.setName("Cash Account");
        security.setIsin("DE0007236101");
        entry.setType(LedgerEntryType.DIVIDENDS);
        entry.setDateTime(LocalDateTime.of(2026, 1, 2, 0, 0));
        entry.setSource("import");
        entry.setNote("note");
        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(account);
        posting.setSecurity(security);
        posting.setAmount(Values.Amount.factorize(12));
        posting.setCurrency(null);
        entry.addPosting(posting);
        ledger.addEntry(entry);

        var result = LedgerStructuralValidator.validate(ledger);
        var message = LedgerDiagnosticMessageFormatter.formatValidationResult(ledger, result);

        assertFalse(result.isOK());
        assertThat(message, containsString("[POSTING_CURRENCY_REQUIRED] "));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterDate + ": 2026-01-02T00:00"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterType + ": DIVIDENDS"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterAccount + ": Cash Account"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterSecurity
                        + ": Siemens AG (" + Messages.LedgerDiagnosticMessageFormatterIsin + "=DE0007236101)"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterSource + ": import"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterNote + ": note"));
        assertThat(message, containsString("UUID: posting-1"));
    }

    @Test
    public void testValidationFormattingReportsUnavailableContext()
    {
        var result = LedgerStructuralValidator.validate(null);
        var message = LedgerDiagnosticMessageFormatter.formatValidationResult(null, result);

        assertFalse(result.isOK());
        assertThat(message, containsString("[LEDGER_REQUIRED] "));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":\n  "
                        + Messages.LedgerDiagnosticMessageFormatterContextUnavailable));
    }

    @Test
    public void testMigrationFormattingAddsPartialLegacyContext()
    {
        var client = new Client();
        var account = new Account();
        var transaction = new name.abuchen.portfolio.model.AccountTransaction(
                        name.abuchen.portfolio.model.AccountTransaction.Type.DEPOSIT);

        account.setName("Cash Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        transaction.setDateTime(LocalDateTime.of(2026, 1, 3, 0, 0));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(25));
        transaction.setSource("bank import");
        account.addTransaction(transaction);

        var message = LedgerDiagnosticMessageFormatter.formatMigrationDiagnostic(client,
                        "[LEDGER-IMPORT-001] family=ACCOUNT reason=TEST uuids=[" + transaction.getUUID() + "]",
                        transaction);

        assertThat(message, containsString("[LEDGER-IMPORT-001] family=ACCOUNT reason=TEST"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterDate + ": 2026-01-03T00:00"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterType + ":"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterAccount + ": Cash Account"));
        assertThat(message, containsString(Messages.LedgerDiagnosticMessageFormatterSource + ": bank import"));
    }
}
