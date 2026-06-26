package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
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

        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO);
        var sourcePostingUUID = LedgerProjectionSupport.primaryPosting(entry, sourceProjection).getUUID();
        var targetPostingUUID = LedgerProjectionSupport.primaryPosting(entry, targetProjection).getUUID();
        var sourcePortfolio = sourceProjection.getPortfolio();
        var targetPortfolio = targetProjection.getPortfolio();

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourceProjection.getUUID(), sourcePortfolio, targetProjection.getUUID(), targetPortfolio,
                        sourcePostingUUID, targetPostingUUID));
    }

    public void validate(LedgerEntry entry, LedgerPortfolioTransferEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.SECURITY_TRANSFER)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_058
                            .message("Unsupported portfolio transfer edit for " + entry.getType())); //$NON-NLS-1$

        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO);
        var sourcePostingUUID = LedgerProjectionSupport.primaryPosting(entry, sourceProjection).getUUID();
        var targetPostingUUID = LedgerProjectionSupport.primaryPosting(entry, targetProjection).getUUID();
        var sourcePortfolio = sourceProjection.getPortfolio();
        var targetPortfolio = targetProjection.getPortfolio();

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourceProjection.getUUID(), sourcePortfolio, targetProjection.getUUID(), targetPortfolio,
                        sourcePostingUUID, targetPostingUUID));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerPortfolioTransferEdit edit, String sourceProjectionUUID,
                    name.abuchen.portfolio.model.Portfolio sourcePortfolio, String targetProjectionUUID,
                    name.abuchen.portfolio.model.Portfolio targetPortfolio, String sourcePostingUUID,
                    String targetPostingUUID)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getSourcePosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, sourcePostingUUID));
        edit.getTargetPosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, targetPostingUUID));
        unitPostingUpdater.apply(editedEntry, edit.getUnits());
        ensureOwners(editedEntry, sourceProjectionUUID, sourcePortfolio, targetProjectionUUID, targetPortfolio);
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream() //
                        .filter(projection -> projection.getRole() == role) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Projection not found: " + role)); //$NON-NLS-1$
    }

    private void ensureOwners(LedgerEntry entry, String sourceProjectionUUID,
                    name.abuchen.portfolio.model.Portfolio source, String targetProjectionUUID,
                    name.abuchen.portfolio.model.Portfolio target)
    {
        var sourceProjection = entry.getProjectionRefs().stream()
                        .filter(projection -> projection.getUUID().equals(sourceProjectionUUID)).findFirst()
                        .orElseThrow();
        var targetProjection = entry.getProjectionRefs().stream()
                        .filter(projection -> projection.getUUID().equals(targetProjectionUUID)).findFirst()
                        .orElseThrow();

        if (sourceProjection.getPortfolio() != source || targetProjection.getPortfolio() != target)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_057
                            .message("Portfolio transfer owner changes are not supported")); //$NON-NLS-1$
    }
}
