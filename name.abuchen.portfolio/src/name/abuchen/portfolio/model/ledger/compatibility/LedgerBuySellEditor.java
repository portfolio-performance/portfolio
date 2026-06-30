package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Updates same-shape ledger-backed buy/sell transactions.
 * This class is part of the Ledger compatibility layer. Contributor code should use it when
 * an existing UI or import path needs to edit Ledger truth safely.
 */
public final class LedgerBuySellEditor
{
    private final LedgerUnitPostingUpdater unitPostingUpdater = new LedgerUnitPostingUpdater();

    public void apply(LedgerBackedAccountTransaction transaction, LedgerBuySellEdit edit)
    {
        apply(transaction.getLedgerEntry(), edit);
    }

    public void apply(LedgerBackedPortfolioTransaction transaction, LedgerBuySellEdit edit)
    {
        apply(transaction.getLedgerEntry(), edit);
    }

    public void apply(LedgerEntry entry, LedgerBuySellEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_025
                            .message("Unsupported buy/sell edit for " + entry.getType())); //$NON-NLS-1$

        var accountProjection = projection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = projection(entry, LedgerProjectionRole.PORTFOLIO);
        var cashPostingUUID = LedgerProjectionSupport.primaryPosting(entry, accountProjection).getUUID();
        var securityPostingUUID = LedgerProjectionSupport.primaryPosting(entry, portfolioProjection).getUUID();

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit,
                        accountProjection.getUUID(), portfolioProjection.getUUID(), cashPostingUUID,
                        securityPostingUUID));
    }

    public void validate(LedgerEntry entry, LedgerBuySellEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_026
                            .message("Unsupported buy/sell edit for " + entry.getType())); //$NON-NLS-1$

        var accountProjection = projection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = projection(entry, LedgerProjectionRole.PORTFOLIO);
        var cashPostingUUID = LedgerProjectionSupport.primaryPosting(entry, accountProjection).getUUID();
        var securityPostingUUID = LedgerProjectionSupport.primaryPosting(entry, portfolioProjection).getUUID();

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit,
                        accountProjection.getUUID(), portfolioProjection.getUUID(), cashPostingUUID,
                        securityPostingUUID));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerBuySellEdit edit, String accountProjectionUUID,
                    String portfolioProjectionUUID, String cashPostingUUID, String securityPostingUUID)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getCashPosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, cashPostingUUID));
        edit.getSecurityPosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, securityPostingUUID));
        unitPostingUpdater.apply(editedEntry, edit.getUnits());
        ensureProjectionExists(editedEntry, accountProjectionUUID);
        ensureProjectionExists(editedEntry, portfolioProjectionUUID);
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream() //
                        .filter(projection -> projection.getRole() == role) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Projection not found: " + role)); //$NON-NLS-1$
    }

    private void ensureProjectionExists(LedgerEntry entry, String projectionUUID)
    {
        if (entry.getProjectionRefs().stream().noneMatch(projection -> projection.getUUID().equals(projectionUUID)))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_027
                            .message("Projection was removed by edit: " + projectionUUID)); //$NON-NLS-1$
    }
}
