package name.abuchen.portfolio.model.ledger.compatibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.Money;

/**
 * Creates and updates ledger-backed buy/sell transactions.
 * This class is part of the Ledger compatibility layer for existing UI, import, and action
 * code. Contributor code should use it instead of mutating projected legacy transactions.
 */
public final class LedgerBuySellTransactionCreator
{
    private final Client client;

    public LedgerBuySellTransactionCreator(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public BuySellEntry create(Portfolio portfolio, Account account, PortfolioTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security, long shares,
                    List<Transaction.Unit> units, String note, String source)
    {
        return create(portfolio, account, type, dateTime, amount, currencyCode, security, shares,
                        LedgerForexAmount.none(), LedgerForexAmount.none(), units, note, source);
    }

    public BuySellEntry create(Portfolio portfolio, Account account, PortfolioTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security, long shares,
                    LedgerForexAmount cashForex, LedgerForexAmount securityForex, List<Transaction.Unit> units,
                    String note, String source)
    {
        Objects.requireNonNull(portfolio);
        Objects.requireNonNull(account);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(security);
        Objects.requireNonNull(cashForex);
        Objects.requireNonNull(securityForex);
        Objects.requireNonNull(units);

        var metadata = LedgerTransactionMetadata.of(dateTime).withNote(note).withSource(source);
        var value = Money.of(currencyCode, amount);
        var cashLeg = LedgerAccountCashLeg.of(account, value, cashForex);
        var securityLeg = LedgerPortfolioSecurityLeg.of(portfolio, LedgerSecurityQuantity.of(security, shares), value,
                        securityForex);
        var creator = new LedgerTransactionCreator(client);
        var created = switch (type)
        {
            case BUY -> creator.createBuy(metadata, cashLeg, securityLeg, creationUnits(units));
            case SELL -> creator.createSell(metadata, cashLeg, securityLeg, creationUnits(units));
            case DELIVERY_INBOUND, DELIVERY_OUTBOUND, TRANSFER_IN, TRANSFER_OUT -> throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_CONVERT_030.message("Unsupported buy/sell ledger production type: " + type)); //$NON-NLS-1$
        };

        return materializeAndWrap(portfolio, account, created);
    }

    public boolean isLedgerBacked(BuySellEntry entry)
    {
        return entry.getAccountTransaction() instanceof LedgerBackedTransaction
                        || entry.getPortfolioTransaction() instanceof LedgerBackedTransaction;
    }

    public boolean canUpdate(BuySellEntry entry)
    {
        return entry.getAccountTransaction() instanceof LedgerBackedAccountTransaction
                        && entry.getPortfolioTransaction() instanceof LedgerBackedPortfolioTransaction
                        && isBuySell(entry.getAccountTransaction().getType())
                        && isBuySell(entry.getPortfolioTransaction().getType())
                        && entry.getAccountTransaction().getType().name()
                                        .equals(entry.getPortfolioTransaction().getType().name());
    }

    public BuySellEntry update(BuySellEntry entry, Portfolio portfolio, Account account, PortfolioTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security, long shares,
                    List<Transaction.Unit> units, String note, String source)
    {
        return update(entry, portfolio, account, type, dateTime, amount, currencyCode, security, shares, null, null,
                        unitPostingPatch(entry, units), note, source);
    }

    public BuySellEntry update(BuySellEntry entry, Portfolio portfolio, Account account, PortfolioTransaction.Type type,
                    LocalDateTime dateTime, long amount, String currencyCode, Security security, long shares,
                    LedgerForexAmount cashForex, LedgerForexAmount securityForex, LedgerUnitPostingPatch units,
                    String note, String source)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(portfolio);
        Objects.requireNonNull(account);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(security);
        Objects.requireNonNull(units);

        if (!canUpdate(entry))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_031
                            .message("Only ledger-backed buy/sell transactions can be updated")); //$NON-NLS-1$

        var accountTransaction = (LedgerBackedAccountTransaction) entry.getAccountTransaction();
        var portfolioTransaction = (LedgerBackedPortfolioTransaction) entry.getPortfolioTransaction();

        if (portfolioTransaction.getType() != type || !accountTransaction.getType().name().equals(type.name()))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_032.message("Changing buy/sell type is not supported")); //$NON-NLS-1$

        var ledgerEntry = accountTransaction.getLedgerEntry();
        var accountProjectionUUID = accountTransaction.getLedgerProjectionRef().getUUID();
        var portfolioProjectionUUID = portfolioTransaction.getLedgerProjectionRef().getUUID();
        var ownerPatchHelper = new LedgerOwnerPatchHelper(client);

        var editBuilder = LedgerBuySellEdit.builder()
                        .metadata(LedgerEntryMetadataPatch.builder().dateTime(dateTime).note(note).source(source)
                                        .build())
                        .cashAmount(amount)
                        .cashCurrency(currencyCode)
                        .securityAmount(amount)
                        .securityCurrency(currencyCode)
                        .security(security)
                        .shares(shares)
                        .units(units);

        applyCashForex(editBuilder, cashForex);
        applySecurityForex(editBuilder, securityForex);

        var edit = editBuilder.build();
        var editor = new LedgerBuySellEditor();

        if (accountTransaction.getLedgerProjectionRef().getAccount() != account
                        || portfolioTransaction.getLedgerProjectionRef().getPortfolio() != portfolio)
            editor.validate(ledgerEntry, edit);

        if (accountTransaction.getLedgerProjectionRef().getAccount() != account)
            ownerPatchHelper.moveBuySellAccountSide(ledgerEntry, account);

        if (portfolioTransaction.getLedgerProjectionRef().getPortfolio() != portfolio)
            ownerPatchHelper.moveBuySellPortfolioSide(ledgerEntry, portfolio);

        accountTransaction = (LedgerBackedAccountTransaction) find(account, accountProjectionUUID);
        portfolioTransaction = (LedgerBackedPortfolioTransaction) find(portfolio, portfolioProjectionUUID);
        entry = (BuySellEntry) accountTransaction.getCrossEntry();

        editor.apply(portfolioTransaction, edit);

        return entry;
    }

    private void applyCashForex(LedgerBuySellEdit.Builder builder, LedgerForexAmount forex)
    {
        if (forex == null)
            return;

        if (forex.isPresent())
            builder.cashForexAmount(forex.getForexAmount().getAmount())
                            .cashForexCurrency(forex.getForexAmount().getCurrencyCode())
                            .cashExchangeRate(forex.getExchangeRate());
        else
            builder.cashForexAmount(null).cashForexCurrency(null).cashExchangeRate(null);
    }

    private void applySecurityForex(LedgerBuySellEdit.Builder builder, LedgerForexAmount forex)
    {
        if (forex == null)
            return;

        if (forex.isPresent())
            builder.securityForexAmount(forex.getForexAmount().getAmount())
                            .securityForexCurrency(forex.getForexAmount().getCurrencyCode())
                            .securityExchangeRate(forex.getExchangeRate());
        else
            builder.securityForexAmount(null).securityForexCurrency(null).securityExchangeRate(null);
    }

    private boolean isBuySell(AccountTransaction.Type type)
    {
        return type == AccountTransaction.Type.BUY || type == AccountTransaction.Type.SELL;
    }

    private boolean isBuySell(PortfolioTransaction.Type type)
    {
        return type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL;
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

    private LedgerUnitPostingPatch unitPostingPatch(LedgerBackedPortfolioTransaction transaction,
                    List<Transaction.Unit> units)
    {
        var edits = new java.util.ArrayList<LedgerUnitPostingEdit>();

        transaction.getLedgerEntry().getPostings().stream()
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

    private LedgerUnitPostingPatch unitPostingPatch(BuySellEntry entry, List<Transaction.Unit> units)
    {
        Objects.requireNonNull(units);

        if (!(entry.getPortfolioTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_033
                            .message("Only ledger-backed buy/sell transactions can be updated")); //$NON-NLS-1$

        return unitPostingPatch(ledgerTransaction, units);
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

    private BuySellEntry materializeAndWrap(Portfolio portfolio, Account account,
                    LedgerTransactionCreator.CreatedTransaction created)
    {
        var accountProjectionUUID = created.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT)
                        .findFirst()
                        .orElseThrow()
                        .getUUID();
        var portfolioProjectionUUID = created.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.PORTFOLIO)
                        .findFirst()
                        .orElseThrow()
                        .getUUID();

        LedgerProjectionService.materialize(client);

        return BuySellEntry.readOnly(portfolio, find(portfolio, portfolioProjectionUUID), account,
                        find(account, accountProjectionUUID));
    }

    private PortfolioTransaction find(Portfolio portfolio, String projectionUUID)
    {
        return portfolio.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger buy/sell portfolio projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }

    private AccountTransaction find(Account account, String projectionUUID)
    {
        return account.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger buy/sell account projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }
}
