package name.abuchen.portfolio.model.ledger.compatibility;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * Creates and updates ledger-backed account-only transactions.
 * This class is part of the Ledger compatibility layer for existing UI, import, and action
 * code. Contributor code should use it instead of mutating projected legacy transactions.
 */
public final class LedgerAccountOnlyTransactionCreator
{
    private final Client client;

    public LedgerAccountOnlyTransactionCreator(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public AccountTransaction create(Account account, AccountTransaction.Type type, LocalDateTime dateTime, long amount,
                    String currencyCode, Security security, List<Transaction.Unit> units, String note, String source)
    {
        return create(account, type, dateTime, amount, currencyCode, security, LedgerForexAmount.none(), units, note,
                        source);
    }

    public AccountTransaction create(Account account, AccountTransaction.Type type, LocalDateTime dateTime, long amount,
                    String currencyCode, Security security, LedgerForexAmount forex, List<Transaction.Unit> units,
                    String note, String source)
    {
        Objects.requireNonNull(account);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(forex);
        Objects.requireNonNull(units);

        var facts = normalizePrimaryUnit(type, amount, currencyCode, units);
        var metadata = LedgerTransactionMetadata.of(dateTime).withNote(note).withSource(source);
        var cashLeg = LedgerAccountCashLeg.of(account, Money.of(currencyCode, facts.amount()), forex);
        var optionalSecurity = security != null ? LedgerOptionalSecurity.of(security) : LedgerOptionalSecurity.none();
        var creationUnits = creationUnits(facts.units());
        var creator = new LedgerTransactionCreator(client);

        var created = switch (type)
        {
            case DEPOSIT -> creator.createDeposit(metadata, cashLeg, creationUnits);
            case REMOVAL -> creator.createRemoval(metadata, cashLeg, creationUnits);
            case INTEREST -> creator.createInterest(metadata, cashLeg, optionalSecurity, creationUnits);
            case INTEREST_CHARGE -> creator.createInterestCharge(metadata, cashLeg, optionalSecurity, creationUnits);
            case FEES -> creator.createFee(metadata, cashLeg, optionalSecurity, creationUnits);
            case FEES_REFUND -> creator.createFeeRefund(metadata, cashLeg, optionalSecurity, creationUnits);
            case TAXES -> creator.createTax(metadata, cashLeg, optionalSecurity, creationUnits);
            case TAX_REFUND -> creator.createTaxRefund(metadata, cashLeg, optionalSecurity, creationUnits);
            case BUY, SELL, TRANSFER_IN, TRANSFER_OUT, DIVIDENDS -> throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_CONVERT_003.message("Unsupported account-only ledger production type: " + type)); //$NON-NLS-1$
        };

        return materializeAndFind(account, created);
    }

    private NormalizedAccountOnlyFacts normalizePrimaryUnit(AccountTransaction.Type type, long amount,
                    String currencyCode, List<Transaction.Unit> units)
    {
        var primaryUnitType = primaryUnitType(type);

        if (primaryUnitType == null || amount != 0)
            return new NormalizedAccountOnlyFacts(amount, units);

        var remainingUnits = new ArrayList<Transaction.Unit>();
        long primaryUnitAmount = 0;

        for (var unit : units)
        {
            if (unit.getType() == primaryUnitType && unit.getForex() == null
                            && currencyCode.equals(unit.getAmount().getCurrencyCode()))
            {
                primaryUnitAmount += unit.getAmount().getAmount();
            }
            else
            {
                remainingUnits.add(unit);
            }
        }

        return new NormalizedAccountOnlyFacts(amount != 0 ? amount : primaryUnitAmount, List.copyOf(remainingUnits));
    }

    private Transaction.Unit.Type primaryUnitType(AccountTransaction.Type type)
    {
        return switch (type)
        {
            case FEES, FEES_REFUND -> Transaction.Unit.Type.FEE;
            case TAXES, TAX_REFUND -> Transaction.Unit.Type.TAX;
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, BUY, SELL, TRANSFER_IN, TRANSFER_OUT, DIVIDENDS -> null;
        };
    }

    public boolean canUpdate(AccountTransaction transaction)
    {
        return transaction instanceof LedgerBackedAccountTransaction && isSupportedAccountOnlyType(transaction.getType());
    }

    public AccountTransaction update(AccountTransaction transaction, Account account, AccountTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security,
                    List<Transaction.Unit> units, String note, String source)
    {
        return update(transaction, account, type, dateTime, amount, currencyCode, security, null,
                        unitPostingPatch(transaction, units), note, source);
    }

    public AccountTransaction update(AccountTransaction transaction, Account account, AccountTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security,
                    LedgerForexAmount forex, LedgerUnitPostingPatch units, String note, String source)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(account);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(units);

        if (!(transaction instanceof LedgerBackedAccountTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_004
                            .message("Only ledger-backed account transactions can be updated")); //$NON-NLS-1$
        if (!isSupportedAccountOnlyType(type) || transaction.getType() != type)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_005.message("Unsupported account-only ledger production edit type: " + type)); //$NON-NLS-1$

        var editBuilder = LedgerAccountTransactionEdit.builder()
                        .metadata(LedgerEntryMetadataPatch.builder().dateTime(dateTime).note(note).source(source)
                                        .build())
                        .amount(amount)
                        .currency(currencyCode)
                        .security(security)
                        .clearExDate()
                        .units(units);

        applyForex(editBuilder, forex);

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
                                        "Ledger account projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }

    private boolean isSupportedAccountOnlyType(AccountTransaction.Type type)
    {
        return switch (type)
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, FEES, FEES_REFUND, TAXES, TAX_REFUND -> true;
            case BUY, SELL, TRANSFER_IN, TRANSFER_OUT, DIVIDENDS -> false;
        };
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
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_006
                            .message("Only ledger-backed account transactions can be updated")); //$NON-NLS-1$

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

    private record NormalizedAccountOnlyFacts(long amount, List<Transaction.Unit> units)
    {
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
                                        "Ledger account projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }
}
