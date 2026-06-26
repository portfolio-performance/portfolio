package name.abuchen.portfolio.model.ledger.legacy;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LegacyTransactionToLedgerMigratorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2025, 12, 28, 0, 0);

    @Test
    public void testAccountOnlyFamiliesMigrateWithProjectionUUIDAndApiParity()
    {
        assertAccountOnlyMigration(AccountTransaction.Type.DEPOSIT, LedgerEntryType.DEPOSIT);
        assertAccountOnlyMigration(AccountTransaction.Type.REMOVAL, LedgerEntryType.REMOVAL);
        assertAccountOnlyMigration(AccountTransaction.Type.INTEREST, LedgerEntryType.INTEREST);
        assertAccountOnlyMigration(AccountTransaction.Type.INTEREST_CHARGE, LedgerEntryType.INTEREST_CHARGE);
        assertAccountOnlyMigration(AccountTransaction.Type.FEES, LedgerEntryType.FEES);
        assertAccountOnlyMigration(AccountTransaction.Type.FEES_REFUND, LedgerEntryType.FEES_REFUND);
        assertAccountOnlyMigration(AccountTransaction.Type.TAXES, LedgerEntryType.TAXES);
        assertAccountOnlyMigration(AccountTransaction.Type.TAX_REFUND, LedgerEntryType.TAX_REFUND);
    }

    @Test
    public void testDividendMigrationPreservesSecurityExDateUnitsAndForex()
    {
        var client = new Client();
        var account = register(client, account());
        var security = security();
        var dividend = accountTransaction(AccountTransaction.Type.DIVIDENDS, 120);

        dividend.setSecurity(security);
        dividend.setShares(Values.Share.factorize(3));
        dividend.setExDate(EX_DATE);
        dividend.addUnit(new Unit(Unit.Type.TAX, money(10)));
        dividend.addUnit(new Unit(Unit.Type.FEE, money(2)));
        dividend.addUnit(new Unit(Unit.Type.GROSS_VALUE, money(150),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(300)), BigDecimal.valueOf(0.5)));
        account.addTransaction(dividend);

        var result = migrate(client);
        var migrated = onlyAccountTransaction(account);
        var entry = onlyLedgerEntry(client);
        var posting = entry.getPostings().get(0);

        assertFalse(result.hasDiagnostics());
        assertThat(result.getMigratedTransactionCount(), is(1));
        assertThat(entry.getType(), is(LedgerEntryType.DIVIDENDS));
        assertThat(migrated.getUUID(), is(dividend.getUUID()));
        assertThat(migrated.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertSame(security, migrated.getSecurity());
        assertThat(migrated.getShares(), is(dividend.getShares()));
        assertThat(migrated.getExDate(), is(EX_DATE));
        assertThat(migrated.getUnits().toList().size(), is(3));
        assertThat(posting.getParameters().get(0).getType(), is(LedgerParameterType.EX_DATE));
        assertThat(posting.getParameters().get(0).getValueKind(), is(LedgerParameter.ValueKind.LOCAL_DATE_TIME));
        assertTrue(entry.getPostings().stream().anyMatch(p -> p.getType() == LedgerPostingType.GROSS_VALUE
                        && Long.valueOf(Values.Amount.factorize(300)).equals(p.getForexAmount())
                        && CurrencyUnit.USD.equals(p.getForexCurrency())
                        && BigDecimal.valueOf(0.5).compareTo(p.getExchangeRate()) == 0));
        assertValid(client);
    }

    @Test
    public void testBuyMigrationCreatesOneEntryTwoProjectionUUIDsAndCrossEntry()
    {
        assertBuySellMigration(PortfolioTransaction.Type.BUY, LedgerEntryType.BUY);
    }

    @Test
    public void testSellMigrationCreatesOneEntryTwoProjectionUUIDsAndCrossEntry()
    {
        assertBuySellMigration(PortfolioTransaction.Type.SELL, LedgerEntryType.SELL);
    }

    @Test
    public void testAccountTransferMigrationPreservesDirectionAndProjectionUUIDs()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var transfer = new AccountTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(55));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("transfer note");
        transfer.setSource("transfer source");
        transfer.insert();

        var sourceUUID = transfer.getSourceTransaction().getUUID();
        var targetUUID = transfer.getTargetTransaction().getUUID();

        migrate(client);

        var entry = onlyLedgerEntry(client);
        var sourceTransaction = onlyAccountTransaction(source);
        var targetTransaction = onlyAccountTransaction(target);

        assertThat(entry.getType(), is(LedgerEntryType.CASH_TRANSFER));
        assertThat(sourceTransaction.getUUID(), is(sourceUUID));
        assertThat(targetTransaction.getUUID(), is(targetUUID));
        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(sourceTransaction.getDateTime(), is(transfer.getSourceTransaction().getDateTime()));
        assertThat(targetTransaction.getDateTime(), is(transfer.getTargetTransaction().getDateTime()));
        assertThat(sourceTransaction.getAmount(), is(transfer.getSourceTransaction().getAmount()));
        assertThat(targetTransaction.getAmount(), is(transfer.getTargetTransaction().getAmount()));
        assertThat(sourceTransaction.getCurrencyCode(), is(transfer.getSourceTransaction().getCurrencyCode()));
        assertThat(targetTransaction.getCurrencyCode(), is(transfer.getTargetTransaction().getCurrencyCode()));
        assertThat(sourceTransaction.getNote(), is(transfer.getSourceTransaction().getNote()));
        assertThat(sourceTransaction.getSource(), is(transfer.getSourceTransaction().getSource()));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertValid(client);
    }

    @Test
    public void testPortfolioTransferMigrationPreservesDirectionAndProjectionUUIDs()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var security = security();
        var transfer = new name.abuchen.portfolio.model.PortfolioTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setSecurity(security);
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(400));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("portfolio transfer note");
        transfer.setSource("portfolio transfer source");
        transfer.insert();

        var sourceUUID = transfer.getSourceTransaction().getUUID();
        var targetUUID = transfer.getTargetTransaction().getUUID();

        migrate(client);

        var entry = onlyLedgerEntry(client);
        var sourceTransaction = onlyPortfolioTransaction(source);
        var targetTransaction = onlyPortfolioTransaction(target);

        assertThat(entry.getType(), is(LedgerEntryType.SECURITY_TRANSFER));
        assertThat(sourceTransaction.getUUID(), is(sourceUUID));
        assertThat(targetTransaction.getUUID(), is(targetUUID));
        assertThat(sourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(sourceTransaction.getDateTime(), is(transfer.getSourceTransaction().getDateTime()));
        assertThat(targetTransaction.getDateTime(), is(transfer.getTargetTransaction().getDateTime()));
        assertThat(sourceTransaction.getAmount(), is(transfer.getSourceTransaction().getAmount()));
        assertThat(targetTransaction.getAmount(), is(transfer.getTargetTransaction().getAmount()));
        assertThat(sourceTransaction.getCurrencyCode(), is(transfer.getSourceTransaction().getCurrencyCode()));
        assertThat(targetTransaction.getCurrencyCode(), is(transfer.getTargetTransaction().getCurrencyCode()));
        assertSame(security, sourceTransaction.getSecurity());
        assertSame(security, targetTransaction.getSecurity());
        assertThat(sourceTransaction.getShares(), is(Values.Share.factorize(7)));
        assertThat(targetTransaction.getShares(), is(Values.Share.factorize(7)));
        assertThat(sourceTransaction.getNote(), is(transfer.getSourceTransaction().getNote()));
        assertThat(sourceTransaction.getSource(), is(transfer.getSourceTransaction().getSource()));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertValid(client);
    }

    @Test
    public void testDeliveriesMigrateWithProjectionUUIDAndUnits()
    {
        assertDeliveryMigration(PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerEntryType.DELIVERY_INBOUND);
        assertDeliveryMigration(PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerEntryType.DELIVERY_OUTBOUND);
    }

    @Test
    public void testDuplicateMigrationPrevention()
    {
        var client = new Client();
        var account = register(client, account());
        var deposit = accountTransaction(AccountTransaction.Type.DEPOSIT, 100);

        account.addTransaction(deposit);

        var first = migrate(client);
        var second = migrate(client);

        assertThat(first.getMigratedTransactionCount(), is(1));
        assertThat(second.getMigratedTransactionCount(), is(0));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(deposit.getUUID()));
    }

    @Test
    public void testMixedClientMigratesSupportedAndLeavesUnsupportedLegacyUntouched()
    {
        var client = new Client();
        var account = register(client, account());
        var supported = accountTransaction(AccountTransaction.Type.DEPOSIT, 100);
        var unsupported = accountTransaction(AccountTransaction.Type.BUY, 200);

        account.addTransaction(supported);
        account.addTransaction(unsupported);

        var result = migrate(client);

        assertThat(result.getMigratedTransactionCount(), is(1));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertTrue(account.getTransactions().stream().anyMatch(transaction -> transaction == unsupported));
        assertTrue(account.getTransactions().stream().anyMatch(transaction -> transaction instanceof LedgerBackedTransaction
                        && transaction.getUUID().equals(supported.getUUID())));
    }

    @Test
    public void testMalformedCrossEntryIsDiagnosedAndDoesNotCreatePartialLedgerEntry()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = new BuySellEntry();

        account.setName("Migration Account");
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(DATE_TIME);
        entry.setSecurity(security());
        entry.setShares(Values.Share.factorize(1));
        entry.setAmount(Values.Amount.factorize(100));
        entry.setCurrencyCode(CurrencyUnit.EUR);
        account.addTransaction(entry.getAccountTransaction());

        var result = migrate(client);

        assertTrue(result.hasDiagnostics());
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_001.prefix(), "family=BUY_SELL",
                        "reason=MALFORMED_CROSS_ENTRY", "incomplete", entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID(),
                        Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":",
                        Messages.LedgerDiagnosticMessageFormatterAccount + ": Migration Account",
                        Messages.LedgerDiagnosticMessageFormatterDate + ": 2026-01-02T00:00",
                        Messages.LedgerDiagnosticMessageFormatterType + ":");
        assertTrue(client.getLedger().getEntries().isEmpty());
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
    }

    @Test
    public void testAccountTransferMalformedCrossEntryIncompleteHasImportCode()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var transfer = new AccountTransferEntry(source, target);

        source.setName("Source Account");
        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(55));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setTargetAccount(null);
        source.addTransaction(transfer.getSourceTransaction());

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_006.prefix(), "family=ACCOUNT_TRANSFER",
                        "reason=MALFORMED_CROSS_ENTRY", "incomplete", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID(),
                        Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":",
                        Messages.LedgerDiagnosticMessageFormatterAccount + ": Source Account",
                        Messages.LedgerDiagnosticMessageFormatterType + ":");
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertTrue(target.getTransactions().isEmpty());
    }

    @Test
    public void testPortfolioTransferMalformedCrossEntryIncompleteHasImportCode()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var transfer = new name.abuchen.portfolio.model.PortfolioTransferEntry(source, target);

        source.setName("Source Portfolio");
        transfer.setDate(DATE_TIME);
        transfer.setSecurity(security());
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(400));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setTargetPortfolio(null);
        source.addTransaction(transfer.getSourceTransaction());

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_011.prefix(), "family=PORTFOLIO_TRANSFER",
                        "reason=MALFORMED_CROSS_ENTRY", "incomplete", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID(),
                        Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":",
                        Messages.LedgerDiagnosticMessageFormatterPortfolio + ": Source Portfolio",
                        Messages.LedgerDiagnosticMessageFormatterSecurity + ": Security",
                        Messages.LedgerDiagnosticMessageFormatterType + ":");
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertTrue(target.getTransactions().isEmpty());
    }

    @Test
    public void testBuySellRejectsNonBuySellPortfolioType()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);

        entry.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_IN);
        entry.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_004.prefix(), "family=BUY_SELL",
                        "reason=TYPE_MISMATCH", entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
        assertSame(entry.getPortfolioTransaction(), portfolio.getTransactions().get(0));
    }

    @Test
    public void testBuySellRejectsNonBuySellAccountType()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);

        entry.getAccountTransaction().setType(AccountTransaction.Type.TRANSFER_IN);
        entry.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_004.prefix(), "family=BUY_SELL",
                        "reason=TYPE_MISMATCH", entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
        assertSame(entry.getPortfolioTransaction(), portfolio.getTransactions().get(0));
    }

    @Test
    public void testBuySellRejectsDirectionMismatch()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);

        entry.getPortfolioTransaction().setType(PortfolioTransaction.Type.SELL);
        entry.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_004.prefix(), "family=BUY_SELL",
                        "reason=TYPE_MISMATCH", entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
        assertSame(entry.getPortfolioTransaction(), portfolio.getTransactions().get(0));
    }

    @Test
    public void testBuySellMetadataMismatchesAreRejected()
    {
        assertBuySellMetadataMismatchIsRejected("dateTime");
        assertBuySellMetadataMismatchIsRejected("note");
        assertBuySellMetadataMismatchIsRejected("source");
    }

    @Test
    public void testAccountTransferMetadataMismatchIsRejected()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var transfer = new AccountTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(55));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("source note");
        transfer.setSource("source");
        transfer.getTargetTransaction().setNote("target note");
        transfer.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_019.prefix(), "family=ACCOUNT_TRANSFER",
                        "reason=METADATA_MISMATCH", "field=note", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    @Test
    public void testPortfolioTransferMetadataMismatchIsRejected()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var transfer = new name.abuchen.portfolio.model.PortfolioTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setSecurity(security());
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(400));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("source note");
        transfer.setSource("source");
        transfer.getTargetTransaction().setSource("target source");
        transfer.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_020.prefix(), "family=PORTFOLIO_TRANSFER",
                        "reason=METADATA_MISMATCH", "field=source", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    @Test
    public void testMigrationPlanIsAtomicWhenCandidateValidationFails()
    {
        var client = new Client();
        var account = register(client, account());
        var valid = accountTransaction(AccountTransaction.Type.DEPOSIT, 100);
        var invalid = accountTransaction(AccountTransaction.Type.REMOVAL, -10);

        account.addTransaction(valid);
        account.addTransaction(invalid);

        var result = migrate(client);

        assertThat(result.getMigratedTransactionCount(), is(0));
        assertDiagnostic(result, "family=MIGRATION", "reason=FAILED_VALIDATION", "[SIGNED_FACT_NOT_ALLOWED] ");
        assertTrue(client.getLedger().getEntries().isEmpty());
        assertThat(account.getTransactions().size(), is(2));
        assertSame(valid, account.getTransactions().get(0));
        assertSame(invalid, account.getTransactions().get(1));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    @Test
    public void testPartialExistingBuySellAccountProjectionDoesNotRemoveLegacyTransactions()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);

        entry.insert();
        client.getLedger().addEntry(existingAccountProjectionEntry(LedgerEntryType.BUY, account,
                        entry.getAccountTransaction().getUUID()));

        var result = migrate(client);

        assertPartialDuplicate(result, "BUY_SELL", entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
        assertSame(entry.getPortfolioTransaction(), portfolio.getTransactions().get(0));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
        assertFalse(portfolio.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    @Test
    public void testPartialExistingBuySellPortfolioProjectionDoesNotRemoveLegacyTransactions()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);

        entry.insert();
        client.getLedger().addEntry(existingPortfolioProjectionEntry(LedgerEntryType.BUY, portfolio,
                        entry.getPortfolioTransaction().getUUID(), LedgerProjectionRole.PORTFOLIO));

        var result = migrate(client);

        assertPartialDuplicate(result, "BUY_SELL", entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
        assertSame(entry.getPortfolioTransaction(), portfolio.getTransactions().get(0));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
        assertFalse(portfolio.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    @Test
    public void testPartialExistingAccountTransferProjectionDoesNotRemoveLegacyTransactions()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var transfer = new AccountTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(55));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();
        client.getLedger().addEntry(existingAccountProjectionEntry(LedgerEntryType.CASH_TRANSFER, source,
                        transfer.getSourceTransaction().getUUID()));

        var result = migrate(client);

        assertPartialDuplicate(result, "ACCOUNT_TRANSFER", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    @Test
    public void testPartialExistingPortfolioTransferProjectionDoesNotRemoveLegacyTransactions()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var transfer = new name.abuchen.portfolio.model.PortfolioTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setSecurity(security());
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(400));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();
        client.getLedger().addEntry(existingPortfolioProjectionEntry(LedgerEntryType.SECURITY_TRANSFER, target,
                        transfer.getTargetTransaction().getUUID(), LedgerProjectionRole.TARGET_PORTFOLIO));

        var result = migrate(client);

        assertPartialDuplicate(result, "PORTFOLIO_TRANSFER", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    @Test
    public void testCompleteExistingBuySellGroupRemovesLegacyRowsWithoutCreatingDuplicateEntry()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);

        entry.insert();
        client.getLedger().addEntry(existingBuySellEntry(account, portfolio, entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID()));

        var result = migrate(client);

        assertThat(result.getMigratedTransactionCount(), is(0));
        assertDiagnostic(result, "family=BUY_SELL", "reason=SKIPPED_ALREADY_MIGRATED",
                        entry.getAccountTransaction().getUUID(), entry.getPortfolioTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(1));
        assertTrue(account.getTransactions().get(0) instanceof LedgerBackedTransaction);
        assertTrue(portfolio.getTransactions().get(0) instanceof LedgerBackedTransaction);
    }

    @Test
    public void testExistingAccountProjectionMustMatchFamilyShape()
    {
        assertAccountDuplicateConflict(AccountTransaction.Type.DEPOSIT, LedgerEntryType.REMOVAL, false, "ENTRY_TYPE");
        assertAccountDuplicateConflict(AccountTransaction.Type.DEPOSIT, LedgerEntryType.DEPOSIT, true,
                        "PROJECTION_OWNER");
    }

    @Test
    public void testExistingDuplicateWithStructuralValidationFailureIsDiagnosed()
    {
        var client = new Client();
        var account = register(client, account());
        var transaction = accountTransaction(AccountTransaction.Type.DEPOSIT, 100);
        var existing = existingAccountProjectionEntry(LedgerEntryType.DEPOSIT, account, transaction.getUUID());

        existing.setDateTime(null);
        account.addTransaction(transaction);
        client.getLedger().addEntry(existing);

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "ACCOUNT", "STRUCTURAL_VALIDATION", transaction.getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(existing, client.getLedger().getEntries().get(0));
        assertThat(account.getTransactions().size(), is(1));
        assertSame(transaction, account.getTransactions().get(0));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    @Test
    public void testExistingDividendProjectionMustMatchFamilyShape()
    {
        assertDividendDuplicateConflict(LedgerEntryType.DEPOSIT, false, false, "ENTRY_TYPE");
        assertDividendDuplicateConflict(LedgerEntryType.DIVIDENDS, true, false, "PROJECTION_OWNER");
        assertDividendDuplicateConflict(LedgerEntryType.DIVIDENDS, false, true, "DIVIDEND_SECURITY");
    }

    @Test
    public void testExistingBuySellDuplicateMustMatchTypeRolesOwnersAndPostingOwners()
    {
        assertBuySellDuplicateConflict(existing -> existing.setType(LedgerEntryType.SELL), "ENTRY_TYPE");
        assertBuySellDuplicateConflict(this::swapBuySellProjectionSides, "PROJECTION_ROLE");
        assertBuySellDuplicateConflict((existing, account, portfolio, otherAccount, otherPortfolio) -> {
            var accountProjection = existing.getProjectionRefs().get(0);
            var cashPosting = postingByUUID(existing, accountProjection.getPrimaryPostingUUID());

            accountProjection.setAccount(otherAccount);
            cashPosting.setAccount(otherAccount);
        }, "PROJECTION_OWNER");
        assertBuySellDuplicateConflict((existing, account, portfolio, otherAccount, otherPortfolio) -> {
            var portfolioProjection = existing.getProjectionRefs().get(1);
            var securityPosting = postingByUUID(existing, portfolioProjection.getPrimaryPostingUUID());

            portfolioProjection.setPortfolio(otherPortfolio);
            securityPosting.setPortfolio(otherPortfolio);
        }, "PROJECTION_OWNER");
        assertBuySellDuplicateConflict((existing, account, portfolio, otherAccount, otherPortfolio) -> {
            var accountProjection = existing.getProjectionRefs().get(0);
            var cashPosting = postingByUUID(existing, accountProjection.getPrimaryPostingUUID());

            cashPosting.setAccount(otherAccount);
        }, "POSTING_OWNER");
    }

    @Test
    public void testExistingAccountTransferDuplicateMustPreserveSourceTargetShape()
    {
        assertAccountTransferDuplicateConflict(existing -> {
            existing.getProjectionRefs().get(0).setRole(LedgerProjectionRole.TARGET_ACCOUNT);
            existing.getProjectionRefs().get(1).setRole(LedgerProjectionRole.SOURCE_ACCOUNT);
        }, "PROJECTION_ROLE");
        assertAccountTransferDuplicateConflict((existing, source, target, other) -> {
            var sourceProjection = existing.getProjectionRefs().get(0);
            postingByUUID(existing, sourceProjection.getPrimaryPostingUUID()).setAccount(other);
        }, "POSTING_OWNER");
        assertAccountTransferDuplicateConflict((existing, source, target, other) -> {
            var targetProjection = existing.getProjectionRefs().get(1);
            postingByUUID(existing, targetProjection.getPrimaryPostingUUID()).setAccount(other);
        }, "POSTING_OWNER");
    }

    @Test
    public void testExistingPortfolioTransferDuplicateMustPreserveSourceTargetShape()
    {
        assertPortfolioTransferDuplicateConflict(existing -> {
            existing.getProjectionRefs().get(0).setRole(LedgerProjectionRole.TARGET_PORTFOLIO);
            existing.getProjectionRefs().get(1).setRole(LedgerProjectionRole.SOURCE_PORTFOLIO);
        }, "PROJECTION_ROLE");
        assertPortfolioTransferDuplicateConflict((existing, source, target, other) -> {
            var sourceProjection = existing.getProjectionRefs().get(0);
            postingByUUID(existing, sourceProjection.getPrimaryPostingUUID()).setPortfolio(other);
        }, "POSTING_OWNER");
        assertPortfolioTransferDuplicateConflict((existing, source, target, other) -> {
            var targetProjection = existing.getProjectionRefs().get(1);
            postingByUUID(existing, targetProjection.getPrimaryPostingUUID()).setPortfolio(other);
        }, "POSTING_OWNER");
    }

    @Test
    public void testExistingDeliveryDuplicateMustMatchDirectionAndOwner()
    {
        assertDeliveryDuplicateConflict(LedgerEntryType.DELIVERY_OUTBOUND, false, "ENTRY_TYPE");
        assertDeliveryDuplicateConflict(LedgerEntryType.DELIVERY_INBOUND, true, "PROJECTION_OWNER");
    }

    @Test
    public void testExistingDuplicateUnitPostingsMustMatchExactSemanticFacts()
    {
        assertAccountUnitDuplicateConflict(existing -> unitPosting(existing, LedgerPostingType.FEE).setAmount(
                        Values.Amount.factorize(8)));
        assertAccountUnitDuplicateConflict(existing -> unitPosting(existing, LedgerPostingType.TAX).setCurrency(
                        CurrencyUnit.USD));
        assertAccountUnitDuplicateConflict(existing -> {
            var posting = unitPosting(existing, LedgerPostingType.GROSS_VALUE);

            posting.setForexAmount(Values.Amount.factorize(301));
            posting.setForexCurrency(CurrencyUnit.EUR);
            posting.setExchangeRate(BigDecimal.valueOf(0.6));
        });
    }

    @Test
    public void testExistingDuplicateUnitPostingsAreComparedWithoutPostingUUIDOrListOrder()
    {
        var client = new Client();
        var account = register(client, account());
        var transaction = accountTransaction(AccountTransaction.Type.INTEREST, 100);
        var existing = existingAccountProjectionEntry(LedgerEntryType.INTEREST, account, transaction.getUUID());

        transaction.addUnit(new Unit(Unit.Type.FEE, money(4)));
        transaction.addUnit(new Unit(Unit.Type.FEE, money(4)));
        transaction.addUnit(new Unit(Unit.Type.TAX, money(3)));
        account.addTransaction(transaction);
        existing.addPosting(unitPosting(LedgerPostingType.TAX, 3));
        existing.addPosting(unitPosting(LedgerPostingType.FEE, 4));
        existing.addPosting(unitPosting(LedgerPostingType.FEE, 4));
        client.getLedger().addEntry(existing);

        var result = migrate(client);

        assertThat(result.getMigratedTransactionCount(), is(0));
        assertDiagnostic(result, "family=ACCOUNT", "reason=SKIPPED_ALREADY_MIGRATED", transaction.getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertTrue(account.getTransactions().get(0) instanceof LedgerBackedTransaction);
    }

    @Test
    public void testRerunAfterValidMultiProjectionAndDeliveryMigrationIsIdempotent()
    {
        var client = new Client();
        var sourceAccount = register(client, account());
        var targetAccount = register(client, account());
        var sourcePortfolio = register(client, portfolio());
        var targetPortfolio = register(client, portfolio());
        var deliveryPortfolio = register(client, portfolio());
        var accountTransfer = new AccountTransferEntry(sourceAccount, targetAccount);
        var portfolioTransfer = new name.abuchen.portfolio.model.PortfolioTransferEntry(sourcePortfolio,
                        targetPortfolio);
        var delivery = portfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND, security(), 100);

        accountTransfer.setDate(DATE_TIME);
        accountTransfer.setAmount(Values.Amount.factorize(55));
        accountTransfer.setCurrencyCode(CurrencyUnit.EUR);
        accountTransfer.insert();
        portfolioTransfer.setDate(DATE_TIME);
        portfolioTransfer.setSecurity(security());
        portfolioTransfer.setShares(Values.Share.factorize(7));
        portfolioTransfer.setAmount(Values.Amount.factorize(400));
        portfolioTransfer.setCurrencyCode(CurrencyUnit.EUR);
        portfolioTransfer.insert();
        deliveryPortfolio.addTransaction(delivery);

        var first = migrate(client);
        var second = migrate(client);

        assertThat(first.getMigratedTransactionCount(), is(5));
        assertThat(second.getMigratedTransactionCount(), is(0));
        assertThat(client.getLedger().getEntries().size(), is(3));
        assertThat(sourceAccount.getTransactions().size(), is(1));
        assertThat(targetAccount.getTransactions().size(), is(1));
        assertThat(sourcePortfolio.getTransactions().size(), is(1));
        assertThat(targetPortfolio.getTransactions().size(), is(1));
        assertThat(deliveryPortfolio.getTransactions().size(), is(1));
    }

    @Test
    public void testAccountTransferUnitsAreRejectedRatherThanDropped()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var transfer = new AccountTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(55));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.getSourceTransaction().addUnit(new Unit(Unit.Type.FEE, money(1)));
        transfer.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_010.prefix(), "family=ACCOUNT_TRANSFER",
                        "reason=UNSUPPORTED_UNITS", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    @Test
    public void testPortfolioTransferUnitsAreRejectedRatherThanDropped()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var transfer = new name.abuchen.portfolio.model.PortfolioTransferEntry(source, target);

        transfer.setDate(DATE_TIME);
        transfer.setSecurity(security());
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(400));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.getTargetTransaction().addUnit(new Unit(Unit.Type.TAX, money(1)));
        transfer.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, LedgerDiagnosticCode.LEDGER_IMPORT_015.prefix(), "family=PORTFOLIO_TRANSFER",
                        "reason=UNSUPPORTED_UNITS", transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    @Test
    public void testUnsupportedUnitPostingMembershipHasImportCode() throws ReflectiveOperationException
    {
        var projection = new LedgerProjectionRef();
        var posting = new LedgerPosting();
        var builder = Class.forName(
                        "name.abuchen.portfolio.model.ledger.legacy.LegacyTransactionToLedgerMigrator$MigrationGraphBuilder");
        var method = builder.getDeclaredMethod("addUnitMemberships", LedgerProjectionRef.class, List.class);

        posting.setType(LedgerPostingType.CASH);
        method.setAccessible(true);

        var exception = assertThrows(InvocationTargetException.class,
                        () -> method.invoke(null, projection, List.of(posting)));

        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        assertTrue(exception.getCause().getMessage().contains(LedgerDiagnosticCode.LEDGER_IMPORT_021.prefix()));
        assertTrue(exception.getCause().getMessage().contains("Unsupported unit posting type: CASH"));
    }

    @Test
    public void testClientAllTransactionsUsesDeduplicatedMigratedShape()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);

        entry.insert();

        migrate(client);

        assertThat(client.getAllTransactions().size(), is(1));
        assertSame(portfolio, client.getAllTransactions().get(0).getOwner());
        assertThat(client.getAllTransactions().get(0).getTransaction().getUUID(),
                        is(entry.getPortfolioTransaction().getUUID()));
    }

    @Test
    public void testInvestmentPlanLegacyTransactionRefsBecomeStableLedgerRefs()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);
        var plan = new InvestmentPlan();

        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, money(5)));
        entry.insert();
        plan.getTransactions().add(entry.getPortfolioTransaction());
        client.addPlan(plan);

        var accountUUID = entry.getAccountTransaction().getUUID();
        var portfolioUUID = entry.getPortfolioTransaction().getUUID();

        migrate(client);

        var ledgerEntry = onlyLedgerEntry(client);
        var cashPosting = ledgerEntry.getPostings().stream()
                        .filter(posting -> posting.getType() == LedgerPostingType.CASH).findFirst().orElseThrow();
        var securityPosting = ledgerEntry.getPostings().stream()
                        .filter(posting -> posting.getType() == LedgerPostingType.SECURITY).findFirst().orElseThrow();
        var feePosting = ledgerEntry.getPostings().stream()
                        .filter(posting -> posting.getType() == LedgerPostingType.FEE).findFirst().orElseThrow();
        var ref = plan.getLedgerExecutionRefs().get(0);

        assertTrue(plan.getTransactions().isEmpty());
        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(ref.getLedgerEntryUUID(), is(migratedEntryUUID(LedgerEntryType.BUY, portfolioUUID)));
        assertThat(ref.getProjectionUUID(), is(portfolioUUID));
        assertThat(ref.getProjectionRole(), is(LedgerProjectionRole.PORTFOLIO));
        assertThat(ledgerEntry.getUUID(), is(ref.getLedgerEntryUUID()));
        assertThat(cashPosting.getUUID(), is(migratedPostingUUID(accountUUID, LedgerPostingType.CASH, "primary")));
        assertThat(securityPosting.getUUID(),
                        is(migratedPostingUUID(portfolioUUID, LedgerPostingType.SECURITY, "primary")));
        assertThat(feePosting.getUUID(), is(migratedPostingUUID(portfolioUUID, LedgerPostingType.FEE, "unit-0")));
        assertValid(client);
    }

    private void assertAccountOnlyMigration(AccountTransaction.Type type, LedgerEntryType entryType)
    {
        var client = new Client();
        var account = register(client, account());
        var security = security();
        var transaction = accountTransaction(type, 100);

        transaction.setSecurity(security);
        transaction.setShares(Values.Share.factorize(2));
        transaction.addUnit(new Unit(Unit.Type.TAX, money(3)));
        transaction.addUnit(new Unit(Unit.Type.FEE, money(4)));
        account.addTransaction(transaction);

        var result = migrate(client);
        var migrated = onlyAccountTransaction(account);
        var entry = onlyLedgerEntry(client);
        var projection = entry.getProjectionRefs().get(0);

        assertFalse(result.hasDiagnostics());
        assertThat(result.getMigratedTransactionCount(), is(1));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(entry.getType(), is(entryType));
        assertThat(projection.getPrimaryMembership().orElseThrow().getPostingUUID(),
                        is(projection.getPrimaryPostingUUID()));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.FEE_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.TAX_UNIT).size(), is(1));
        assertThat(migrated, instanceOf(LedgerBackedAccountTransaction.class));
        assertThat(migrated.getUUID(), is(transaction.getUUID()));
        assertThat(migrated.getType(), is(type));
        assertThat(migrated.getDateTime(), is(transaction.getDateTime()));
        assertThat(migrated.getAmount(), is(transaction.getAmount()));
        assertThat(migrated.getCurrencyCode(), is(transaction.getCurrencyCode()));
        assertSame(security, migrated.getSecurity());
        assertThat(migrated.getShares(), is(transaction.getShares()));
        assertThat(migrated.getNote(), is(transaction.getNote()));
        assertThat(migrated.getSource(), is(transaction.getSource()));
        assertThat(migrated.getUnits().toList().size(), is(2));
        assertValid(client);
    }

    private void assertBuySellMigration(PortfolioTransaction.Type type, LedgerEntryType entryType)
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, type);

        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, money(5)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, money(6)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, money(150),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(300)), BigDecimal.valueOf(0.5)));
        entry.insert();

        var accountUUID = entry.getAccountTransaction().getUUID();
        var portfolioUUID = entry.getPortfolioTransaction().getUUID();

        var result = migrate(client);
        var ledgerEntry = onlyLedgerEntry(client);
        var accountTransaction = onlyAccountTransaction(account);
        var portfolioTransaction = onlyPortfolioTransaction(portfolio);
        var accountProjection = ledgerEntry.getProjectionRefs().get(0);
        var portfolioProjection = ledgerEntry.getProjectionRefs().get(1);

        assertFalse(result.hasDiagnostics());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(ledgerEntry.getType(), is(entryType));
        assertThat(ledgerEntry.getProjectionRefs().size(), is(2));
        assertThat(accountTransaction.getUUID(), is(accountUUID));
        assertThat(portfolioTransaction.getUUID(), is(portfolioUUID));
        assertThat(accountProjection.getPrimaryMembership().orElseThrow().getPostingUUID(),
                        is(accountProjection.getPrimaryPostingUUID()));
        assertThat(portfolioProjection.getPrimaryMembership().orElseThrow().getPostingUUID(),
                        is(portfolioProjection.getPrimaryPostingUUID()));
        assertThat(accountProjection.getMembershipsByRole(ProjectionMembershipRole.FEE_UNIT).size(), is(1));
        assertThat(accountProjection.getMembershipsByRole(ProjectionMembershipRole.TAX_UNIT).size(), is(1));
        assertThat(accountProjection.getMembershipsByRole(ProjectionMembershipRole.GROSS_VALUE_UNIT).size(), is(1));
        assertThat(portfolioProjection.getMembershipsByRole(ProjectionMembershipRole.FEE_UNIT).size(), is(1));
        assertThat(portfolioProjection.getMembershipsByRole(ProjectionMembershipRole.TAX_UNIT).size(), is(1));
        assertThat(portfolioProjection.getMembershipsByRole(ProjectionMembershipRole.GROSS_VALUE_UNIT).size(), is(1));
        assertThat(accountTransaction.getType().name(), is(type.name()));
        assertThat(portfolioTransaction.getType(), is(type));
        assertThat(accountTransaction.getDateTime(), is(entry.getAccountTransaction().getDateTime()));
        assertThat(accountTransaction.getAmount(), is(entry.getAccountTransaction().getAmount()));
        assertThat(accountTransaction.getCurrencyCode(), is(entry.getAccountTransaction().getCurrencyCode()));
        assertSame(entry.getAccountTransaction().getSecurity(), accountTransaction.getSecurity());
        assertThat(accountTransaction.getNote(), is(entry.getAccountTransaction().getNote()));
        assertThat(accountTransaction.getSource(), is(entry.getAccountTransaction().getSource()));
        assertThat(portfolioTransaction.getAmount(), is(entry.getPortfolioTransaction().getAmount()));
        assertThat(portfolioTransaction.getCurrencyCode(), is(entry.getPortfolioTransaction().getCurrencyCode()));
        assertSame(entry.getPortfolioTransaction().getSecurity(), portfolioTransaction.getSecurity());
        assertThat(portfolioTransaction.getShares(), is(entry.getPortfolioTransaction().getShares()));
        assertThat(portfolioTransaction.getNote(), is(entry.getPortfolioTransaction().getNote()));
        assertThat(portfolioTransaction.getSource(), is(entry.getPortfolioTransaction().getSource()));
        assertSame(portfolio, accountTransaction.getCrossEntry().getCrossOwner(accountTransaction));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertThat(portfolioTransaction.getUnits().toList().size(), is(3));
        assertTrue(portfolioTransaction.getUnits().anyMatch(unit -> unit.getType() == Unit.Type.GROSS_VALUE
                        && CurrencyUnit.USD.equals(unit.getForex().getCurrencyCode())
                        && unit.getForex().getAmount() == Values.Amount.factorize(300)
                        && BigDecimal.valueOf(0.5).compareTo(unit.getExchangeRate()) == 0));
        assertValid(client);
    }

    private void assertDeliveryMigration(PortfolioTransaction.Type type, LedgerEntryType entryType)
    {
        var client = new Client();
        var portfolio = register(client, portfolio());
        var security = security();
        var transaction = portfolioTransaction(type, security, 100);

        transaction.addUnit(new Unit(Unit.Type.FEE, money(4)));
        portfolio.addTransaction(transaction);

        var result = migrate(client);
        var migrated = onlyPortfolioTransaction(portfolio);

        assertFalse(result.hasDiagnostics());
        assertThat(onlyLedgerEntry(client).getType(), is(entryType));
        assertThat(migrated.getUUID(), is(transaction.getUUID()));
        assertThat(migrated.getType(), is(type));
        assertSame(security, migrated.getSecurity());
        assertThat(migrated.getShares(), is(transaction.getShares()));
        assertThat(migrated.getAmount(), is(transaction.getAmount()));
        assertThat(migrated.getCurrencyCode(), is(transaction.getCurrencyCode()));
        assertThat(migrated.getDateTime(), is(transaction.getDateTime()));
        assertThat(migrated.getNote(), is(transaction.getNote()));
        assertThat(migrated.getSource(), is(transaction.getSource()));
        assertThat(migrated.getUnits().toList().size(), is(1));
        assertValid(client);
    }

    private void assertAccountDuplicateConflict(AccountTransaction.Type type, LedgerEntryType existingEntryType,
                    boolean wrongOwner, String expectedMismatch)
    {
        var client = new Client();
        var account = register(client, account());
        var existingAccount = wrongOwner ? register(client, account()) : account;
        var transaction = accountTransaction(type, 100);

        account.addTransaction(transaction);
        client.getLedger().addEntry(existingAccountProjectionEntry(existingEntryType, existingAccount,
                        transaction.getUUID()));

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "ACCOUNT", expectedMismatch, transaction.getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(transaction, account.getTransactions().get(0));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    private void assertDividendDuplicateConflict(LedgerEntryType existingEntryType, boolean wrongOwner,
                    boolean wrongSecurity, String expectedMismatch)
    {
        var client = new Client();
        var account = register(client, account());
        var existingAccount = wrongOwner ? register(client, account()) : account;
        var security = security();
        var dividend = accountTransaction(AccountTransaction.Type.DIVIDENDS, 120);
        var existing = existingAccountProjectionEntry(existingEntryType, existingAccount, dividend.getUUID());

        dividend.setSecurity(security);
        account.addTransaction(dividend);
        existing.getPostings().get(0).setSecurity(wrongSecurity ? new Security("Other", CurrencyUnit.EUR) : security);
        client.getLedger().addEntry(existing);

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "ACCOUNT", expectedMismatch, dividend.getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(dividend, account.getTransactions().get(0));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    private void assertBuySellDuplicateConflict(Consumer<LedgerEntry> mutator, String expectedMismatch)
    {
        assertBuySellDuplicateConflict((existing, account, portfolio, otherAccount, otherPortfolio) -> mutator.accept(
                        existing), expectedMismatch);
    }

    private void assertBuySellDuplicateConflict(BuySellDuplicateMutator mutator, String expectedMismatch)
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var otherAccount = register(client, account());
        var otherPortfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);
        var existing = existingBuySellEntry(account, portfolio, entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());

        entry.insert();
        mutator.accept(existing, account, portfolio, otherAccount, otherPortfolio);
        client.getLedger().addEntry(existing);

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "BUY_SELL", expectedMismatch, entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
        assertSame(entry.getPortfolioTransaction(), portfolio.getTransactions().get(0));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
        assertFalse(portfolio.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    private void assertAccountTransferDuplicateConflict(Consumer<LedgerEntry> mutator, String expectedMismatch)
    {
        assertAccountTransferDuplicateConflict((existing, source, target, other) -> mutator.accept(existing),
                        expectedMismatch);
    }

    private void assertAccountTransferDuplicateConflict(AccountTransferDuplicateMutator mutator,
                    String expectedMismatch)
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var other = register(client, account());
        var transfer = new AccountTransferEntry(source, target);
        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(55));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();
        var existing = existingAccountTransferEntry(source, target, transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());

        mutator.accept(existing, source, target, other);
        client.getLedger().addEntry(existing);

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "ACCOUNT_TRANSFER", expectedMismatch,
                        transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    private void assertPortfolioTransferDuplicateConflict(Consumer<LedgerEntry> mutator, String expectedMismatch)
    {
        assertPortfolioTransferDuplicateConflict((existing, source, target, other) -> mutator.accept(existing),
                        expectedMismatch);
    }

    private void assertPortfolioTransferDuplicateConflict(PortfolioTransferDuplicateMutator mutator,
                    String expectedMismatch)
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var other = register(client, portfolio());
        var transfer = new name.abuchen.portfolio.model.PortfolioTransferEntry(source, target);
        transfer.setDate(DATE_TIME);
        transfer.setSecurity(security());
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(400));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();
        var existing = existingPortfolioTransferEntry(source, target, transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());

        mutator.accept(existing, source, target, other);
        client.getLedger().addEntry(existing);

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "PORTFOLIO_TRANSFER", expectedMismatch,
                        transfer.getSourceTransaction().getUUID(),
                        transfer.getTargetTransaction().getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(transfer.getSourceTransaction(), source.getTransactions().get(0));
        assertSame(transfer.getTargetTransaction(), target.getTransactions().get(0));
    }

    private void assertDeliveryDuplicateConflict(LedgerEntryType existingEntryType, boolean wrongOwner,
                    String expectedMismatch)
    {
        var client = new Client();
        var portfolio = register(client, portfolio());
        var existingPortfolio = wrongOwner ? register(client, portfolio()) : portfolio;
        var transaction = portfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND, security(), 100);

        portfolio.addTransaction(transaction);
        var role = existingEntryType == LedgerEntryType.DELIVERY_OUTBOUND ? LedgerProjectionRole.DELIVERY_OUTBOUND
                        : LedgerProjectionRole.DELIVERY_INBOUND;
        client.getLedger().addEntry(existingDeliveryEntry(existingEntryType, existingPortfolio, transaction.getUUID(),
                        role));

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "DELIVERY", expectedMismatch, transaction.getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(transaction, portfolio.getTransactions().get(0));
        assertFalse(portfolio.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    private void assertAccountUnitDuplicateConflict(Consumer<LedgerEntry> mutator)
    {
        var client = new Client();
        var account = register(client, account());
        var transaction = accountTransaction(AccountTransaction.Type.INTEREST, 100);
        var existing = existingAccountProjectionEntry(LedgerEntryType.INTEREST, account, transaction.getUUID());

        transaction.addUnit(new Unit(Unit.Type.FEE, money(4)));
        transaction.addUnit(new Unit(Unit.Type.TAX, money(3)));
        transaction.addUnit(new Unit(Unit.Type.GROSS_VALUE, money(150),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(300)), BigDecimal.valueOf(0.5)));
        account.addTransaction(transaction);
        existing.addPosting(unitPosting(LedgerPostingType.FEE, 4));
        existing.addPosting(unitPosting(LedgerPostingType.TAX, 3));
        existing.addPosting(grossValuePosting(150, CurrencyUnit.USD, 300, BigDecimal.valueOf(0.5)));
        mutator.accept(existing);
        client.getLedger().addEntry(existing);

        var result = migrate(client);

        assertDuplicateConflictWithMismatch(result, "ACCOUNT", "UNIT_POSTINGS", transaction.getUUID());
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(transaction, account.getTransactions().get(0));
        assertFalse(account.getTransactions().stream().anyMatch(LedgerBackedTransaction.class::isInstance));
    }

    private void assertBuySellMetadataMismatchIsRejected(String field)
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = buySellEntry(portfolio, account, PortfolioTransaction.Type.BUY);
        var expectedCode = switch (field)
        {
            case "dateTime" -> LedgerDiagnosticCode.LEDGER_IMPORT_018;
            case "note" -> LedgerDiagnosticCode.LEDGER_IMPORT_019;
            case "source" -> LedgerDiagnosticCode.LEDGER_IMPORT_020;
            default -> throw new IllegalArgumentException(field);
        };

        switch (field)
        {
            case "dateTime" -> entry.getAccountTransaction().setDateTime(DATE_TIME.plusDays(1));
            case "note" -> entry.getAccountTransaction().setNote("different note");
            case "source" -> entry.getAccountTransaction().setSource("different source");
            default -> throw new IllegalArgumentException(field);
        }

        entry.insert();

        var result = migrate(client);

        assertNoMigration(result, client);
        assertDiagnostic(result, expectedCode.prefix(), "family=BUY_SELL", "reason=METADATA_MISMATCH",
                        "field=" + field, entry.getAccountTransaction().getUUID(),
                        entry.getPortfolioTransaction().getUUID());
        assertSame(entry.getAccountTransaction(), account.getTransactions().get(0));
        assertSame(entry.getPortfolioTransaction(), portfolio.getTransactions().get(0));
    }

    private void assertNoMigration(LegacyTransactionToLedgerMigrator.MigrationResult result, Client client)
    {
        assertTrue(result.hasDiagnostics());
        assertThat(result.getMigratedTransactionCount(), is(0));
        assertTrue(client.getLedger().getEntries().isEmpty());
    }

    private void assertPartialDuplicate(LegacyTransactionToLedgerMigrator.MigrationResult result, String family,
                    String... uuids)
    {
        assertDuplicateConflict(result, family, uuids);
    }

    private void assertDuplicateConflict(LegacyTransactionToLedgerMigrator.MigrationResult result, String family,
                    String... uuids)
    {
        assertThat(result.getMigratedTransactionCount(), is(0));
        assertDiagnostic(result, "family=" + family, "reason=DUPLICATE_CONFLICT");

        for (var uuid : uuids)
            assertDiagnostic(result, uuid);
    }

    private void assertDuplicateConflictWithMismatch(LegacyTransactionToLedgerMigrator.MigrationResult result,
                    String family,
                    String expectedMismatch, String... uuids)
    {
        assertDuplicateConflict(result, family, uuids);
        assertDiagnostic(result, "mismatch=" + expectedMismatch);
    }

    private void assertDiagnostic(LegacyTransactionToLedgerMigrator.MigrationResult result, String... fragments)
    {
        assertTrue(result.getDiagnostics().stream().anyMatch(diagnostic -> List.of(fragments).stream()
                        .allMatch(diagnostic::contains)));
    }

    private LedgerEntry existingAccountProjectionEntry(LedgerEntryType entryType, Account account, String projectionUUID)
    {
        var entry = new LedgerEntry();
        var posting = new LedgerPosting();
        var projection = new LedgerProjectionRef(projectionUUID);

        entry.setType(entryType);
        entry.setDateTime(DATE_TIME);
        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(account);
        projection.setPrimaryPostingUUID(posting.getUUID());
        entry.addPosting(posting);
        entry.addProjectionRef(projection);

        return entry;
    }

    private LedgerEntry existingPortfolioProjectionEntry(LedgerEntryType entryType, Portfolio portfolio,
                    String projectionUUID, LedgerProjectionRole role)
    {
        var entry = new LedgerEntry();
        var posting = new LedgerPosting();
        var projection = new LedgerProjectionRef(projectionUUID);

        entry.setType(entryType);
        entry.setDateTime(DATE_TIME);
        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(portfolio);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSecurity(security());
        posting.setShares(Values.Share.factorize(1));
        projection.setRole(role);
        projection.setPortfolio(portfolio);
        projection.setPrimaryPostingUUID(posting.getUUID());
        entry.addPosting(posting);
        entry.addProjectionRef(projection);

        return entry;
    }

    private LedgerEntry existingBuySellEntry(Account account, Portfolio portfolio, String accountProjectionUUID,
                    String portfolioProjectionUUID)
    {
        var entry = new LedgerEntry();
        var cashPosting = new LedgerPosting();
        var securityPosting = new LedgerPosting();
        var accountProjection = new LedgerProjectionRef(accountProjectionUUID);
        var portfolioProjection = new LedgerProjectionRef(portfolioProjectionUUID);

        entry.setType(LedgerEntryType.BUY);
        entry.setDateTime(DATE_TIME);
        cashPosting.setType(LedgerPostingType.CASH);
        cashPosting.setAccount(account);
        cashPosting.setAmount(Values.Amount.factorize(100));
        cashPosting.setCurrency(CurrencyUnit.EUR);
        securityPosting.setType(LedgerPostingType.SECURITY);
        securityPosting.setPortfolio(portfolio);
        securityPosting.setAmount(Values.Amount.factorize(100));
        securityPosting.setCurrency(CurrencyUnit.EUR);
        securityPosting.setSecurity(security());
        securityPosting.setShares(Values.Share.factorize(5));
        accountProjection.setRole(LedgerProjectionRole.ACCOUNT);
        accountProjection.setAccount(account);
        accountProjection.setPrimaryPostingUUID(cashPosting.getUUID());
        portfolioProjection.setRole(LedgerProjectionRole.PORTFOLIO);
        portfolioProjection.setPortfolio(portfolio);
        portfolioProjection.setPrimaryPostingUUID(securityPosting.getUUID());
        entry.addPosting(cashPosting);
        entry.addPosting(securityPosting);
        entry.addProjectionRef(accountProjection);
        entry.addProjectionRef(portfolioProjection);

        return entry;
    }

    private LedgerEntry existingAccountTransferEntry(Account source, Account target, String sourceProjectionUUID,
                    String targetProjectionUUID)
    {
        var entry = new LedgerEntry();
        var sourcePosting = new LedgerPosting();
        var targetPosting = new LedgerPosting();
        var sourceProjection = new LedgerProjectionRef(sourceProjectionUUID);
        var targetProjection = new LedgerProjectionRef(targetProjectionUUID);

        entry.setType(LedgerEntryType.CASH_TRANSFER);
        entry.setDateTime(DATE_TIME);
        sourcePosting.setType(LedgerPostingType.CASH);
        sourcePosting.setAccount(source);
        sourcePosting.setAmount(Values.Amount.factorize(55));
        sourcePosting.setCurrency(CurrencyUnit.EUR);
        targetPosting.setType(LedgerPostingType.CASH);
        targetPosting.setAccount(target);
        targetPosting.setAmount(Values.Amount.factorize(55));
        targetPosting.setCurrency(CurrencyUnit.EUR);
        sourceProjection.setRole(LedgerProjectionRole.SOURCE_ACCOUNT);
        sourceProjection.setAccount(source);
        sourceProjection.setPrimaryPostingUUID(sourcePosting.getUUID());
        targetProjection.setRole(LedgerProjectionRole.TARGET_ACCOUNT);
        targetProjection.setAccount(target);
        targetProjection.setPrimaryPostingUUID(targetPosting.getUUID());
        entry.addPosting(sourcePosting);
        entry.addPosting(targetPosting);
        entry.addProjectionRef(sourceProjection);
        entry.addProjectionRef(targetProjection);

        return entry;
    }

    private LedgerEntry existingPortfolioTransferEntry(Portfolio source, Portfolio target, String sourceProjectionUUID,
                    String targetProjectionUUID)
    {
        var entry = new LedgerEntry();
        var sourcePosting = new LedgerPosting();
        var targetPosting = new LedgerPosting();
        var sourceProjection = new LedgerProjectionRef(sourceProjectionUUID);
        var targetProjection = new LedgerProjectionRef(targetProjectionUUID);
        var security = security();

        entry.setType(LedgerEntryType.SECURITY_TRANSFER);
        entry.setDateTime(DATE_TIME);
        sourcePosting.setType(LedgerPostingType.SECURITY);
        sourcePosting.setPortfolio(source);
        sourcePosting.setAmount(Values.Amount.factorize(400));
        sourcePosting.setCurrency(CurrencyUnit.EUR);
        sourcePosting.setSecurity(security);
        sourcePosting.setShares(Values.Share.factorize(7));
        targetPosting.setType(LedgerPostingType.SECURITY);
        targetPosting.setPortfolio(target);
        targetPosting.setAmount(Values.Amount.factorize(400));
        targetPosting.setCurrency(CurrencyUnit.EUR);
        targetPosting.setSecurity(security);
        targetPosting.setShares(Values.Share.factorize(7));
        sourceProjection.setRole(LedgerProjectionRole.SOURCE_PORTFOLIO);
        sourceProjection.setPortfolio(source);
        sourceProjection.setPrimaryPostingUUID(sourcePosting.getUUID());
        targetProjection.setRole(LedgerProjectionRole.TARGET_PORTFOLIO);
        targetProjection.setPortfolio(target);
        targetProjection.setPrimaryPostingUUID(targetPosting.getUUID());
        entry.addPosting(sourcePosting);
        entry.addPosting(targetPosting);
        entry.addProjectionRef(sourceProjection);
        entry.addProjectionRef(targetProjection);

        return entry;
    }

    private LedgerEntry existingDeliveryEntry(LedgerEntryType entryType, Portfolio portfolio, String projectionUUID,
                    LedgerProjectionRole role)
    {
        var entry = new LedgerEntry();
        var posting = new LedgerPosting();
        var projection = new LedgerProjectionRef(projectionUUID);

        entry.setType(entryType);
        entry.setDateTime(DATE_TIME);
        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(portfolio);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSecurity(security());
        posting.setShares(Values.Share.factorize(5));
        projection.setRole(role);
        projection.setPortfolio(portfolio);
        projection.setPrimaryPostingUUID(posting.getUUID());
        entry.addPosting(posting);
        entry.addProjectionRef(projection);

        return entry;
    }

    private void swapBuySellProjectionSides(LedgerEntry entry)
    {
        var accountProjection = entry.getProjectionRefs().get(0);
        var portfolioProjection = entry.getProjectionRefs().get(1);
        var account = accountProjection.getAccount();
        var portfolio = portfolioProjection.getPortfolio();
        var cashPostingUUID = accountProjection.getPrimaryPostingUUID();
        var securityPostingUUID = portfolioProjection.getPrimaryPostingUUID();

        accountProjection.setRole(LedgerProjectionRole.PORTFOLIO);
        accountProjection.setAccount(null);
        accountProjection.setPortfolio(portfolio);
        accountProjection.setPrimaryPostingUUID(securityPostingUUID);
        portfolioProjection.setRole(LedgerProjectionRole.ACCOUNT);
        portfolioProjection.setPortfolio(null);
        portfolioProjection.setAccount(account);
        portfolioProjection.setPrimaryPostingUUID(cashPostingUUID);
    }

    private LedgerPosting postingByUUID(LedgerEntry entry, String uuid)
    {
        return entry.getPostings().stream().filter(posting -> posting.getUUID().equals(uuid)).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting unitPosting(LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type).findFirst().orElseThrow();
    }

    private LedgerPosting unitPosting(LedgerPostingType type, int amount)
    {
        var posting = new LedgerPosting();

        posting.setType(type);
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);

        return posting;
    }

    private LedgerPosting grossValuePosting(int amount, String forexCurrency, int forexAmount,
                    BigDecimal exchangeRate)
    {
        var posting = unitPosting(LedgerPostingType.GROSS_VALUE, amount);

        posting.setForexAmount(Values.Amount.factorize(forexAmount));
        posting.setForexCurrency(forexCurrency);
        posting.setExchangeRate(exchangeRate);

        return posting;
    }

    @FunctionalInterface
    private interface BuySellDuplicateMutator
    {
        void accept(LedgerEntry entry, Account account, Portfolio portfolio, Account otherAccount,
                        Portfolio otherPortfolio);
    }

    @FunctionalInterface
    private interface AccountTransferDuplicateMutator
    {
        void accept(LedgerEntry entry, Account source, Account target, Account other);
    }

    @FunctionalInterface
    private interface PortfolioTransferDuplicateMutator
    {
        void accept(LedgerEntry entry, Portfolio source, Portfolio target, Portfolio other);
    }

    private LegacyTransactionToLedgerMigrator.MigrationResult migrate(Client client)
    {
        return new LegacyTransactionToLedgerMigrator().migrate(client);
    }

    private void assertValid(Client client)
    {
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());
    }

    private Account register(Client client, Account account)
    {
        client.addAccount(account);
        return account;
    }

    private Portfolio register(Client client, Portfolio portfolio)
    {
        client.addPortfolio(portfolio);
        return portfolio;
    }

    private Account account()
    {
        var account = new Account();

        account.setCurrencyCode(CurrencyUnit.EUR);

        return account;
    }

    private Portfolio portfolio()
    {
        return new Portfolio();
    }

    private Security security()
    {
        return new Security("Security", CurrencyUnit.EUR);
    }

    private AccountTransaction accountTransaction(AccountTransaction.Type type, int amount)
    {
        var transaction = new AccountTransaction(type);

        transaction.setDateTime(DATE_TIME);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(amount));
        transaction.setNote("note");
        transaction.setSource("source");

        return transaction;
    }

    private PortfolioTransaction portfolioTransaction(PortfolioTransaction.Type type, Security security, int amount)
    {
        var transaction = new PortfolioTransaction(type);

        transaction.setDateTime(DATE_TIME);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(amount));
        transaction.setSecurity(security);
        transaction.setShares(Values.Share.factorize(5));
        transaction.setNote("note");
        transaction.setSource("source");

        return transaction;
    }

    private BuySellEntry buySellEntry(Portfolio portfolio, Account account, PortfolioTransaction.Type type)
    {
        var entry = new BuySellEntry(portfolio, account);

        entry.setType(type);
        entry.setDate(DATE_TIME);
        entry.setSecurity(security());
        entry.setShares(Values.Share.factorize(5));
        entry.setAmount(Values.Amount.factorize(100));
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setNote("note");
        entry.setSource("source");

        return entry;
    }

    private AccountTransaction onlyAccountTransaction(Account account)
    {
        assertThat(account.getTransactions().size(), is(1));
        return account.getTransactions().get(0);
    }

    private PortfolioTransaction onlyPortfolioTransaction(Portfolio portfolio)
    {
        assertThat(portfolio.getTransactions().size(), is(1));
        return portfolio.getTransactions().get(0);
    }

    private LedgerEntry onlyLedgerEntry(Client client)
    {
        assertThat(client.getLedger().getEntries().size(), is(1));
        return client.getLedger().getEntries().get(0);
    }

    private Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private String migratedEntryUUID(LedgerEntryType type, String primaryProjectionUUID)
    {
        var key = "ledger-v6:migrated-entry:" + type + ":" + primaryProjectionUUID;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String migratedPostingUUID(String projectionUUID, LedgerPostingType type, String discriminator)
    {
        var key = "ledger-v6:migrated-posting:" + projectionUUID + ":" + type + ":" + discriminator;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
