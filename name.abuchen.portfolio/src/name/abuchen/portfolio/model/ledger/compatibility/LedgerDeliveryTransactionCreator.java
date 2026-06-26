package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.Money;

/**
 * Creates and updates ledger-backed delivery transactions.
 * This class is part of the Ledger compatibility layer for existing UI, import, and action
 * code. Contributor code should use it instead of mutating projected legacy transactions.
 */
public final class LedgerDeliveryTransactionCreator
{
    private final Client client;

    public LedgerDeliveryTransactionCreator(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public PortfolioTransaction create(Portfolio portfolio, PortfolioTransaction.Type type, LocalDateTime dateTime,
                    long amount, String currencyCode, Security security, long shares, Money forexAmount,
                    BigDecimal exchangeRate, List<Transaction.Unit> units, String note, String source)
    {
        Objects.requireNonNull(portfolio);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(security);
        Objects.requireNonNull(units);

        var metadata = LedgerTransactionMetadata.of(dateTime).withNote(note).withSource(source);
        var deliveryLeg = LedgerDeliveryLeg.of(portfolio, LedgerSecurityQuantity.of(security, shares),
                        Money.of(currencyCode, amount), forex(forexAmount, exchangeRate), creationUnits(units));
        var creator = new LedgerTransactionCreator(client);

        var created = switch (type)
        {
            case DELIVERY_INBOUND -> creator.createInboundDelivery(metadata, deliveryLeg);
            case DELIVERY_OUTBOUND -> creator.createOutboundDelivery(metadata, deliveryLeg);
            case BUY, SELL, TRANSFER_IN, TRANSFER_OUT -> throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_CONVERT_036.message("Unsupported delivery ledger production type: " + type)); //$NON-NLS-1$
        };

        return materializeAndFind(portfolio, created);
    }

    public boolean canUpdate(PortfolioTransaction transaction)
    {
        return transaction instanceof LedgerBackedPortfolioTransaction && isSupportedDeliveryType(transaction.getType());
    }

    public PortfolioTransaction update(PortfolioTransaction transaction, Portfolio portfolio,
                    PortfolioTransaction.Type type, LocalDateTime dateTime, long amount, String currencyCode,
                    Security security, long shares, Money forexAmount, BigDecimal exchangeRate,
                    List<Transaction.Unit> units, String note, String source)
    {
        return update(transaction, portfolio, type, dateTime, amount, currencyCode, security, shares,
                        forexAmount != null ? LedgerForexAmount.of(forexAmount, Objects.requireNonNull(exchangeRate))
                                        : null,
                        unitPostingPatch(transaction, units), note, source);
    }

    public PortfolioTransaction update(PortfolioTransaction transaction, Portfolio portfolio,
                    PortfolioTransaction.Type type, LocalDateTime dateTime, long amount, String currencyCode,
                    Security security, long shares, LedgerForexAmount forex, LedgerUnitPostingPatch units, String note,
                    String source)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(portfolio);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(security);
        Objects.requireNonNull(units);

        if (!(transaction instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_037
                            .message("Only ledger-backed delivery transactions can be updated")); //$NON-NLS-1$
        if (!isSupportedDeliveryType(type) || transaction.getType() != type)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_038.message("Unsupported delivery ledger production edit type: " + type)); //$NON-NLS-1$

        var editBuilder = LedgerDeliveryTransactionEdit.builder()
                        .metadata(LedgerEntryMetadataPatch.builder().dateTime(dateTime).note(note).source(source)
                                        .build())
                        .amount(amount)
                        .currency(currencyCode)
                        .security(security)
                        .shares(shares)
                        .units(units);

        applyForex(editBuilder, forex);

        var edit = editBuilder.build();
        var editor = new LedgerDeliveryTransactionEditor();

        if (ledgerTransaction.getLedgerProjectionRef().getPortfolio() != portfolio)
        {
            var projectionUUID = ledgerTransaction.getLedgerProjectionRef().getUUID();

            editor.validate(ledgerTransaction, edit);
            new LedgerOwnerPatchHelper(client).moveDelivery(ledgerTransaction, portfolio);
            ledgerTransaction = (LedgerBackedPortfolioTransaction) find(portfolio, projectionUUID);
        }

        editor.apply(ledgerTransaction, edit);

        return ledgerTransaction;
    }

    private void applyForex(LedgerDeliveryTransactionEdit.Builder builder, LedgerForexAmount forex)
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

    private PortfolioTransaction find(Portfolio portfolio, String projectionUUID)
    {
        return portfolio.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(item -> projectionUUID.equals(item.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger delivery projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }

    private boolean isSupportedDeliveryType(PortfolioTransaction.Type type)
    {
        return switch (type)
        {
            case DELIVERY_INBOUND, DELIVERY_OUTBOUND -> true;
            case BUY, SELL, TRANSFER_IN, TRANSFER_OUT -> false;
        };
    }

    private LedgerForexAmount forex(Money forexAmount, BigDecimal exchangeRate)
    {
        return forexAmount != null ? LedgerForexAmount.of(forexAmount, Objects.requireNonNull(exchangeRate))
                        : LedgerForexAmount.none();
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

    private LedgerUnitPostingPatch unitPostingPatch(PortfolioTransaction transaction, List<Transaction.Unit> units)
    {
        Objects.requireNonNull(units);

        if (!(transaction instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_039
                            .message("Only ledger-backed delivery transactions can be updated")); //$NON-NLS-1$

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

    private PortfolioTransaction materializeAndFind(Portfolio portfolio,
                    LedgerTransactionCreator.CreatedTransaction created)
    {
        var projectionUUID = created.getProjectionRefs().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        return portfolio.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger delivery projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }
}
