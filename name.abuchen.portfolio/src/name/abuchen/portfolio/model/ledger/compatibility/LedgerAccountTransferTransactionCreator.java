package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;

/**
 * Creates and updates ledger-backed account transfer transactions.
 * This class is part of the Ledger compatibility layer for existing UI, import, and action
 * code. Contributor code should use it instead of mutating projected legacy transactions.
 */
public final class LedgerAccountTransferTransactionCreator
{
    private final Client client;

    public LedgerAccountTransferTransactionCreator(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public AccountTransferEntry create(Account sourceAccount, Account targetAccount, LocalDateTime dateTime,
                    long sourceAmount, String sourceCurrencyCode, long targetAmount, String targetCurrencyCode,
                    Money sourceForexAmount, BigDecimal sourceExchangeRate, String note, String source)
    {
        return create(sourceAccount, targetAccount, dateTime, sourceAmount, sourceCurrencyCode, targetAmount,
                        targetCurrencyCode, forex(sourceForexAmount, sourceExchangeRate), LedgerForexAmount.none(),
                        LedgerUnitPostingPatch.none(), note, source);
    }

    public AccountTransferEntry create(Account sourceAccount, Account targetAccount, LocalDateTime dateTime,
                    long sourceAmount, String sourceCurrencyCode, long targetAmount, String targetCurrencyCode,
                    LedgerForexAmount sourceForex, LedgerForexAmount targetForex, LedgerUnitPostingPatch units,
                    String note, String source)
    {
        Objects.requireNonNull(sourceAccount);
        Objects.requireNonNull(targetAccount);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(sourceCurrencyCode);
        Objects.requireNonNull(targetCurrencyCode);
        Objects.requireNonNull(sourceForex);
        Objects.requireNonNull(targetForex);
        Objects.requireNonNull(units);

        var metadata = LedgerTransactionMetadata.of(dateTime).withNote(note).withSource(source);
        var creator = new LedgerTransactionCreator(client);
        var entry = creator.createAccountTransferEntry(metadata,
                        LedgerCashTransferLeg.of(sourceAccount, Money.of(sourceCurrencyCode, sourceAmount), sourceForex),
                        LedgerCashTransferLeg.of(targetAccount, Money.of(targetCurrencyCode, targetAmount), targetForex));

        new LedgerUnitPostingUpdater().apply(entry, units);
        var created = creator.add(entry);

        return materializeAndWrap(sourceAccount, targetAccount, created);
    }

    public boolean isLedgerBacked(AccountTransferEntry entry)
    {
        return entry.getSourceTransaction() instanceof LedgerBackedTransaction
                        || entry.getTargetTransaction() instanceof LedgerBackedTransaction;
    }

    public boolean canUpdate(AccountTransferEntry entry)
    {
        return entry.getSourceTransaction() instanceof LedgerBackedAccountTransaction
                        && entry.getTargetTransaction() instanceof LedgerBackedAccountTransaction
                        && entry.getSourceTransaction().getType() == AccountTransaction.Type.TRANSFER_OUT
                        && entry.getTargetTransaction().getType() == AccountTransaction.Type.TRANSFER_IN;
    }

    public Optional<BigDecimal> getSourceExchangeRate(AccountTransferEntry entry)
    {
        Objects.requireNonNull(entry);

        if (!(entry.getSourceTransaction() instanceof LedgerBackedAccountTransaction ledgerTransaction))
            return Optional.empty();

        var sourceAccount = entry.getSourceAccount();
        var targetAccount = entry.getTargetAccount();
        var projectionRef = ledgerTransaction.getLedgerProjectionRef();
        var postings = ledgerTransaction.getLedgerEntry().getPostings().stream()
                        .filter(posting -> posting.getAccount() == projectionRef.getAccount()).toList();

        if (postings.isEmpty())
            return Optional.empty();

        var posting = projectionRef.getRole() == LedgerProjectionRole.TARGET_ACCOUNT ? postings.get(postings.size() - 1)
                        : postings.get(0);

        if (posting.getForexAmount() == null || posting.getForexCurrency() == null || posting.getExchangeRate() == null
                        || !posting.getCurrency().equals(sourceAccount.getCurrencyCode())
                        || !posting.getForexCurrency().equals(targetAccount.getCurrencyCode()))
            return Optional.empty();

        return Optional.of(ExchangeRate.inverse(posting.getExchangeRate()));
    }

    public AccountTransferEntry update(AccountTransferEntry entry, Account sourceAccount, Account targetAccount,
                    LocalDateTime dateTime, long sourceAmount, String sourceCurrencyCode, long targetAmount,
                    String targetCurrencyCode, Money sourceForexAmount, BigDecimal sourceExchangeRate, String note,
                    String source)
    {
        return update(entry, sourceAccount, targetAccount, dateTime, sourceAmount, sourceCurrencyCode, targetAmount,
                        targetCurrencyCode, sourceForexAmount != null ? forex(sourceForexAmount, sourceExchangeRate)
                                        : null,
                        null, LedgerUnitPostingPatch.none(), note, source);
    }

    public AccountTransferEntry update(AccountTransferEntry entry, Account sourceAccount, Account targetAccount,
                    LocalDateTime dateTime, long sourceAmount, String sourceCurrencyCode, long targetAmount,
                    String targetCurrencyCode, LedgerForexAmount sourceForex, LedgerForexAmount targetForex,
                    LedgerUnitPostingPatch units, String note, String source)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(sourceAccount);
        Objects.requireNonNull(targetAccount);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(sourceCurrencyCode);
        Objects.requireNonNull(targetCurrencyCode);
        Objects.requireNonNull(units);

        if (!canUpdate(entry))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_018.message("Only ledger-backed account transfers can be updated")); //$NON-NLS-1$

        var sourceTransaction = (LedgerBackedAccountTransaction) entry.getSourceTransaction();
        var targetTransaction = (LedgerBackedAccountTransaction) entry.getTargetTransaction();

        var ledgerEntry = sourceTransaction.getLedgerEntry();
        var sourceProjectionUUID = sourceTransaction.getLedgerProjectionRef().getUUID();
        var targetProjectionUUID = targetTransaction.getLedgerProjectionRef().getUUID();
        var ownerPatchHelper = new LedgerOwnerPatchHelper(client);

        var editBuilder = LedgerAccountTransferEdit.builder()
                        .metadata(LedgerEntryMetadataPatch.builder().dateTime(dateTime).note(note).source(source)
                                        .build())
                        .sourceAmount(sourceAmount)
                        .sourceCurrency(sourceCurrencyCode)
                        .targetAmount(targetAmount)
                        .targetCurrency(targetCurrencyCode)
                        .units(units);

        applySourceForex(editBuilder, sourceForex);
        applyTargetForex(editBuilder, targetForex);

        var edit = editBuilder.build();
        var editor = new LedgerAccountTransferEditor();

        if (sourceTransaction.getLedgerProjectionRef().getAccount() != sourceAccount
                        || targetTransaction.getLedgerProjectionRef().getAccount() != targetAccount)
            editor.validate(ledgerEntry, edit);

        if (sourceTransaction.getLedgerProjectionRef().getAccount() != sourceAccount)
            ownerPatchHelper.moveAccountTransferSource(ledgerEntry, sourceAccount);

        if (targetTransaction.getLedgerProjectionRef().getAccount() != targetAccount)
            ownerPatchHelper.moveAccountTransferTarget(ledgerEntry, targetAccount);

        sourceTransaction = (LedgerBackedAccountTransaction) find(sourceAccount, sourceProjectionUUID);
        targetTransaction = (LedgerBackedAccountTransaction) find(targetAccount, targetProjectionUUID);
        entry = (AccountTransferEntry) sourceTransaction.getCrossEntry();

        editor.apply(sourceTransaction, edit);

        return entry;
    }

    private void applySourceForex(LedgerAccountTransferEdit.Builder builder, LedgerForexAmount forex)
    {
        if (forex == null)
            return;

        if (forex.isPresent())
            builder.sourceForexAmount(forex.getForexAmount().getAmount())
                            .sourceForexCurrency(forex.getForexAmount().getCurrencyCode())
                            .sourceExchangeRate(forex.getExchangeRate());
        else
            builder.sourceForexAmount(null).sourceForexCurrency(null).sourceExchangeRate(null);
    }

    private void applyTargetForex(LedgerAccountTransferEdit.Builder builder, LedgerForexAmount forex)
    {
        if (forex == null)
            return;

        if (forex.isPresent())
            builder.targetForexAmount(forex.getForexAmount().getAmount())
                            .targetForexCurrency(forex.getForexAmount().getCurrencyCode())
                            .targetExchangeRate(forex.getExchangeRate());
        else
            builder.targetForexAmount(null).targetForexCurrency(null).targetExchangeRate(null);
    }

    private LedgerForexAmount forex(Money forexAmount, BigDecimal exchangeRate)
    {
        return forexAmount != null ? LedgerForexAmount.of(forexAmount, Objects.requireNonNull(exchangeRate))
                        : LedgerForexAmount.none();
    }

    private AccountTransferEntry materializeAndWrap(Account sourceAccount, Account targetAccount,
                    LedgerTransactionCreator.CreatedTransaction created)
    {
        var sourceProjectionUUID = created.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.SOURCE_ACCOUNT)
                        .findFirst().orElseThrow().getUUID();
        var targetProjectionUUID = created.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.TARGET_ACCOUNT)
                        .findFirst().orElseThrow().getUUID();

        LedgerProjectionService.materialize(client);

        return wrap(sourceAccount, find(sourceAccount, sourceProjectionUUID), targetAccount,
                        find(targetAccount, targetProjectionUUID));
    }

    private AccountTransaction find(Account account, String projectionUUID)
    {
        return account.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger account transfer projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }

    private AccountTransferEntry wrap(Account sourceAccount, AccountTransaction sourceTransaction, Account targetAccount,
                    AccountTransaction targetTransaction)
    {
        return AccountTransferEntry.readOnly(sourceAccount, sourceTransaction, targetAccount, targetTransaction);
    }
}
