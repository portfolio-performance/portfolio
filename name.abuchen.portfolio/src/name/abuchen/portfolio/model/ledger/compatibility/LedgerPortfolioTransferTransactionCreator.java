package name.abuchen.portfolio.model.ledger.compatibility;

import java.time.LocalDateTime;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.Money;

/**
 * Creates and updates ledger-backed portfolio transfer transactions.
 * This class is part of the Ledger compatibility layer for existing UI, import, and action
 * code. Contributor code should use it instead of mutating projected legacy transactions.
 */
public final class LedgerPortfolioTransferTransactionCreator
{
    private final Client client;

    public LedgerPortfolioTransferTransactionCreator(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public PortfolioTransferEntry create(Portfolio sourcePortfolio, Portfolio targetPortfolio, Security security,
                    LocalDateTime dateTime, long shares, long amount, String currencyCode, String note, String source)
    {
        return create(sourcePortfolio, targetPortfolio, security, dateTime, shares, amount, currencyCode,
                        LedgerForexAmount.none(), LedgerForexAmount.none(), LedgerUnitPostingPatch.none(), note,
                        source);
    }

    public PortfolioTransferEntry create(Portfolio sourcePortfolio, Portfolio targetPortfolio, Security security,
                    LocalDateTime dateTime, long shares, long amount, String currencyCode,
                    LedgerForexAmount sourceForex, LedgerForexAmount targetForex, LedgerUnitPostingPatch units,
                    String note, String source)
    {
        Objects.requireNonNull(sourcePortfolio);
        Objects.requireNonNull(targetPortfolio);
        Objects.requireNonNull(security);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(sourceForex);
        Objects.requireNonNull(targetForex);
        Objects.requireNonNull(units);

        var metadata = LedgerTransactionMetadata.of(dateTime).withNote(note).withSource(source);
        var value = Money.of(currencyCode, amount);
        var creator = new LedgerTransactionCreator(client);
        var entry = creator.createPortfolioTransferEntry(metadata, LedgerPortfolioTransferSecurity.of(security, shares),
                        LedgerPortfolioTransferLeg.of(sourcePortfolio, value, sourceForex),
                        LedgerPortfolioTransferLeg.of(targetPortfolio, value, targetForex));

        new LedgerUnitPostingUpdater().apply(entry, units);
        var created = creator.add(entry);

        return materializeAndWrap(sourcePortfolio, targetPortfolio, created);
    }

    public boolean isLedgerBacked(PortfolioTransferEntry entry)
    {
        return entry.getSourceTransaction() instanceof LedgerBackedTransaction
                        || entry.getTargetTransaction() instanceof LedgerBackedTransaction;
    }

    public boolean canUpdate(PortfolioTransferEntry entry)
    {
        return entry.getSourceTransaction() instanceof LedgerBackedPortfolioTransaction
                        && entry.getTargetTransaction() instanceof LedgerBackedPortfolioTransaction
                        && entry.getSourceTransaction().getType() == PortfolioTransaction.Type.TRANSFER_OUT
                        && entry.getTargetTransaction().getType() == PortfolioTransaction.Type.TRANSFER_IN;
    }

    public PortfolioTransferEntry update(PortfolioTransferEntry entry, Portfolio sourcePortfolio,
                    Portfolio targetPortfolio, Security security, LocalDateTime dateTime, long shares, long amount,
                    String currencyCode, String note, String source)
    {
        return update(entry, sourcePortfolio, targetPortfolio, security, dateTime, shares, amount, currencyCode, null,
                        null, LedgerUnitPostingPatch.none(), note, source);
    }

    public PortfolioTransferEntry update(PortfolioTransferEntry entry, Portfolio sourcePortfolio,
                    Portfolio targetPortfolio, Security security, LocalDateTime dateTime, long shares, long amount,
                    String currencyCode, LedgerForexAmount sourceForex, LedgerForexAmount targetForex,
                    LedgerUnitPostingPatch units, String note, String source)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(sourcePortfolio);
        Objects.requireNonNull(targetPortfolio);
        Objects.requireNonNull(security);
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(units);

        if (!canUpdate(entry))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_059.message("Only ledger-backed portfolio transfers can be updated")); //$NON-NLS-1$

        var sourceTransaction = (LedgerBackedPortfolioTransaction) entry.getSourceTransaction();
        var targetTransaction = (LedgerBackedPortfolioTransaction) entry.getTargetTransaction();

        var ledgerEntry = sourceTransaction.getLedgerEntry();
        var sourceProjectionUUID = sourceTransaction.getLedgerProjectionRef().getUUID();
        var targetProjectionUUID = targetTransaction.getLedgerProjectionRef().getUUID();
        var ownerPatchHelper = new LedgerOwnerPatchHelper(client);

        var editBuilder = LedgerPortfolioTransferEdit.builder()
                        .metadata(LedgerEntryMetadataPatch.builder().dateTime(dateTime).note(note).source(source)
                                        .build())
                        .sourceSecurity(security)
                        .sourceShares(shares)
                        .sourceAmount(amount)
                        .sourceCurrency(currencyCode)
                        .targetSecurity(security)
                        .targetShares(shares)
                        .targetAmount(amount)
                        .targetCurrency(currencyCode)
                        .units(units);

        applySourceForex(editBuilder, sourceForex);
        applyTargetForex(editBuilder, targetForex);

        var edit = editBuilder.build();
        var editor = new LedgerPortfolioTransferEditor();

        if (sourceTransaction.getLedgerProjectionRef().getPortfolio() != sourcePortfolio
                        || targetTransaction.getLedgerProjectionRef().getPortfolio() != targetPortfolio)
            editor.validate(ledgerEntry, edit);

        if (sourceTransaction.getLedgerProjectionRef().getPortfolio() != sourcePortfolio)
            ownerPatchHelper.movePortfolioTransferSource(ledgerEntry, sourcePortfolio);

        if (targetTransaction.getLedgerProjectionRef().getPortfolio() != targetPortfolio)
            ownerPatchHelper.movePortfolioTransferTarget(ledgerEntry, targetPortfolio);

        sourceTransaction = (LedgerBackedPortfolioTransaction) find(sourcePortfolio, sourceProjectionUUID);
        targetTransaction = (LedgerBackedPortfolioTransaction) find(targetPortfolio, targetProjectionUUID);
        entry = (PortfolioTransferEntry) sourceTransaction.getCrossEntry();

        editor.apply(sourceTransaction, edit);

        return entry;
    }

    private void applySourceForex(LedgerPortfolioTransferEdit.Builder builder, LedgerForexAmount forex)
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

    private void applyTargetForex(LedgerPortfolioTransferEdit.Builder builder, LedgerForexAmount forex)
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

    private PortfolioTransferEntry materializeAndWrap(Portfolio sourcePortfolio, Portfolio targetPortfolio,
                    LedgerTransactionCreator.CreatedTransaction created)
    {
        var sourceProjectionUUID = created.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.SOURCE_PORTFOLIO)
                        .findFirst()
                        .orElseThrow()
                        .getUUID();
        var targetProjectionUUID = created.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.TARGET_PORTFOLIO)
                        .findFirst()
                        .orElseThrow()
                        .getUUID();

        LedgerProjectionService.materialize(client);

        var sourceTransaction = find(sourcePortfolio, sourceProjectionUUID);
        var targetTransaction = find(targetPortfolio, targetProjectionUUID);

        return PortfolioTransferEntry.readOnly(sourcePortfolio, sourceTransaction, targetPortfolio, targetTransaction);
    }

    private PortfolioTransaction find(Portfolio portfolio, String projectionUUID)
    {
        return portfolio.getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger portfolio transfer projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }
}
