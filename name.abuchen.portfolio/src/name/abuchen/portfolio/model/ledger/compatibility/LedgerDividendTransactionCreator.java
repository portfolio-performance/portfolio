package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.Money;

/**
 * Creates and updates ledger-backed dividend transactions.
 * This class is part of the Ledger compatibility layer for existing UI, import, and action
 * code. Contributor code should use it instead of mutating projected legacy transactions.
 */
public final class LedgerDividendTransactionCreator
{
    private final Client client;

    public LedgerDividendTransactionCreator(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public AccountTransaction create(Account account, LocalDateTime dateTime, long amount, String currencyCode,
                    Security security, long shares, LocalDateTime exDate, Money forexAmount, BigDecimal exchangeRate,
                    List<Transaction.Unit> units, String note, String source)
    {
        Objects.requireNonNull(account);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(security);
        Objects.requireNonNull(units);

        var metadata = LedgerTransactionMetadata.of(dateTime).withNote(note).withSource(source);
        var cashLeg = LedgerAccountCashLeg.of(account, Money.of(currencyCode, amount),
                        forexAmount != null ? LedgerForexAmount.of(forexAmount, Objects.requireNonNull(exchangeRate))
                                        : LedgerForexAmount.none());
        var creationUnits = creationUnits(units);
        var dividend = exDate != null
                        ? LedgerDividend.withExDate(cashLeg, LedgerOptionalSecurity.of(security), creationUnits,
                                        shares, exDate)
                        : LedgerDividend.withoutExDate(cashLeg, LedgerOptionalSecurity.of(security), creationUnits,
                                        shares);

        var created = new LedgerTransactionCreator(client).createDividend(metadata, dividend);

        return materializeAndFind(account, created);
    }

    public boolean canUpdate(AccountTransaction transaction)
    {
        return transaction instanceof LedgerBackedAccountTransaction
                        && transaction.getType() == AccountTransaction.Type.DIVIDENDS;
    }

    public AccountTransaction update(AccountTransaction transaction, Account account, AccountTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security, long shares,
                    LocalDateTime exDate, Money forexAmount, BigDecimal exchangeRate, List<Transaction.Unit> units,
                    String note, String source)
    {
        return update(transaction, account, type, dateTime, amount, currencyCode, security, shares, exDate,
                        forexAmount != null ? LedgerForexAmount.of(forexAmount, Objects.requireNonNull(exchangeRate))
                                        : LedgerForexAmount.none(),
                        unitPostingPatch(transaction, units), note, source);
    }

    public AccountTransaction update(AccountTransaction transaction, Account account, AccountTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security, long shares,
                    LocalDateTime exDate, LedgerForexAmount forex, LedgerUnitPostingPatch units, String note,
                    String source)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(account);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(security);
        Objects.requireNonNull(units);

        if (!(transaction instanceof LedgerBackedAccountTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_042
                            .message("Only ledger-backed dividend transactions can be updated")); //$NON-NLS-1$
        if (type != AccountTransaction.Type.DIVIDENDS || transaction.getType() != AccountTransaction.Type.DIVIDENDS)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_043.message("Unsupported dividend ledger production edit type: " + type)); //$NON-NLS-1$

        var editBuilder = LedgerAccountTransactionEdit.builder()
                        .metadata(LedgerEntryMetadataPatch.builder().dateTime(dateTime).note(note).source(source)
                        .build())
                        .amount(amount)
                        .currency(currencyCode)
                        .security(security)
                        .shares(shares)
                        .units(units);

        applyForex(editBuilder, forex);

        if (exDate != null)
            editBuilder.exDate(exDate);
        else
            editBuilder.clearExDate();

        var edit = editBuilder.build();
        var editor = new LedgerAccountTransactionEditor();

        if (ledgerTransaction.getLedgerProjectionRef().getAccount() != account)
        {
            var projectionUUID = ledgerTransaction.getLedgerProjectionRef().getUUID();

            editor.validate(ledgerTransaction, edit);
            new LedgerOwnerPatchHelper(client).moveAccountOnly(ledgerTransaction, account);
            ledgerTransaction = (LedgerBackedAccountTransaction) find(account, projectionUUID);
        }

        editor.apply(ledgerTransaction, edit);

        return ledgerTransaction;
    }

    private void applyForex(LedgerAccountTransactionEdit.Builder builder, LedgerForexAmount forex)
    {
        if (forex == null)
            return;

        if (forex.isPresent())
            builder.forexAmount(forex.getForexAmount().getAmount())
                            .forexCurrency(forex.getForexAmount().getCurrencyCode())
                            .exchangeRate(forex.getExchangeRate());
        else
            builder.forexAmount(null).forexCurrency(null).exchangeRate(null);
    }

    private AccountTransaction find(Account account, String projectionUUID)
    {
        return account.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(item -> projectionUUID.equals(item.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger dividend projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }

    private LedgerCreationUnits creationUnits(List<Transaction.Unit> units)
    {
        if (units.isEmpty())
            return LedgerCreationUnits.none();

        var ledgerUnits = units.stream().map(this::creationUnit).toList();
        return LedgerCreationUnits.of(ledgerUnits.get(0),
                        ledgerUnits.subList(1, ledgerUnits.size()).toArray(LedgerCreationUnit[]::new));
    }

    private LedgerCreationUnit creationUnit(Transaction.Unit unit)
    {
        var forex = unit.getForex() != null ? LedgerForexAmount.of(unit.getForex(), unit.getExchangeRate())
                        : LedgerForexAmount.none();

        return switch (unit.getType())
        {
            case FEE -> forex.isPresent() ? LedgerCreationUnit.fee(unit.getAmount(), forex)
                            : LedgerCreationUnit.fee(unit.getAmount());
            case TAX -> forex.isPresent() ? LedgerCreationUnit.tax(unit.getAmount(), forex)
                            : LedgerCreationUnit.tax(unit.getAmount());
            case GROSS_VALUE -> LedgerCreationUnit.grossValue(unit.getAmount(), forex);
        };
    }

    private LedgerUnitPostingPatch unitPostingPatch(AccountTransaction transaction, List<Transaction.Unit> units)
    {
        Objects.requireNonNull(units);

        if (!(transaction instanceof LedgerBackedAccountTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_044
                            .message("Only ledger-backed dividend transactions can be updated")); //$NON-NLS-1$

        var edits = new java.util.ArrayList<LedgerUnitPostingEdit>();

        ledgerTransaction.getLedgerEntry().getPostings().stream()
                        .filter(posting -> isUnitPosting(posting.getType()))
                        .map(LedgerPosting::getUUID)
                        .map(LedgerUnitPostingEdit::remove)
                        .forEach(edits::add);

        units.stream().map(this::unitPostingEdit).forEach(edits::add);

        if (edits.isEmpty())
            return LedgerUnitPostingPatch.none();

        return LedgerUnitPostingPatch.of(edits.get(0),
                        edits.subList(1, edits.size()).toArray(LedgerUnitPostingEdit[]::new));
    }

    private LedgerUnitPostingEdit unitPostingEdit(Transaction.Unit unit)
    {
        var ledgerUnit = creationUnit(unit);
        var forex = ledgerUnit.getForex();

        return forex.isPresent()
                        ? LedgerUnitPostingEdit.add(ledgerUnit.getPostingType(), ledgerUnit.getAmount(), forex)
                        : LedgerUnitPostingEdit.add(ledgerUnit.getPostingType(), ledgerUnit.getAmount());
    }

    private boolean isUnitPosting(LedgerPostingType type)
    {
        return type == LedgerPostingType.FEE || type == LedgerPostingType.TAX || type == LedgerPostingType.GROSS_VALUE;
    }

    private AccountTransaction materializeAndFind(Account account, LedgerTransactionCreator.CreatedTransaction created)
    {
        var projectionUUID = created.getProjectionRefs().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        return account.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger dividend projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }
}
