package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Updates same-shape ledger-backed portfolio transfer transactions.
 * This class is part of the Ledger compatibility layer. Contributor code should use it when
 * an existing UI or import path needs to edit Ledger truth safely.
 */
public final class LedgerPortfolioTransferEditor
{
    private final LedgerUnitPostingUpdater unitPostingUpdater = new LedgerUnitPostingUpdater();

    public void apply(LedgerBackedPortfolioTransaction transaction, LedgerPortfolioTransferEdit edit)
    {
        apply(transaction.getLedgerEntry(), edit);
    }

    public void apply(LedgerEntry entry, LedgerPortfolioTransferEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.SECURITY_TRANSFER)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_057
                            .message("Unsupported portfolio transfer edit for " + entry.getType())); //$NON-NLS-1$

        var sourceProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.TARGET_PORTFOLIO);
        var sourcePostingIndex = LedgerEntryEditSupport.postingIndex(entry, sourceProjection.getPrimaryPosting());
        var targetPostingIndex = LedgerEntryEditSupport.postingIndex(entry, targetProjection.getPrimaryPosting());
        var sourcePortfolio = sourceProjection.getPortfolio();
        var targetPortfolio = targetProjection.getPortfolio();

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourcePortfolio, targetPortfolio, sourcePostingIndex, targetPostingIndex));
    }

    public void validate(LedgerEntry entry, LedgerPortfolioTransferEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.SECURITY_TRANSFER)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_058
                            .message("Unsupported portfolio transfer edit for " + entry.getType())); //$NON-NLS-1$

        var sourceProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.TARGET_PORTFOLIO);
        var sourcePostingIndex = LedgerEntryEditSupport.postingIndex(entry, sourceProjection.getPrimaryPosting());
        var targetPostingIndex = LedgerEntryEditSupport.postingIndex(entry, targetProjection.getPrimaryPosting());
        var sourcePortfolio = sourceProjection.getPortfolio();
        var targetPortfolio = targetProjection.getPortfolio();

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourcePortfolio, targetPortfolio, sourcePostingIndex, targetPostingIndex));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerPortfolioTransferEdit edit,
                    name.abuchen.portfolio.model.Portfolio sourcePortfolio,
                    name.abuchen.portfolio.model.Portfolio targetPortfolio,
                    int sourcePostingIndex, int targetPostingIndex)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getSourcePosting().applyTo(LedgerEntryEditSupport.postingAt(editedEntry, sourcePostingIndex));
        edit.getTargetPosting().applyTo(LedgerEntryEditSupport.postingAt(editedEntry, targetPostingIndex));
        unitPostingUpdater.apply(editedEntry, edit.getUnits());
        ensureOwners(editedEntry, sourcePortfolio, targetPortfolio);
    }

    private void ensureOwners(LedgerEntry entry, name.abuchen.portfolio.model.Portfolio source,
                    name.abuchen.portfolio.model.Portfolio target)
    {
        var sourceProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.TARGET_PORTFOLIO);

        if (sourceProjection.getPortfolio() != source || targetProjection.getPortfolio() != target)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_057
                            .message("Portfolio transfer owner changes are not supported")); //$NON-NLS-1$
    }
}
