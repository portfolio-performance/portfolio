package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
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

        var accountProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.PORTFOLIO);
        var cashPostingIndex = LedgerEntryEditSupport.postingIndex(entry, accountProjection.getPrimaryPosting());
        var securityPostingIndex = LedgerEntryEditSupport.postingIndex(entry, portfolioProjection.getPrimaryPosting());

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit,
                        LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO, cashPostingIndex,
                        securityPostingIndex));
    }

    public void validate(LedgerEntry entry, LedgerBuySellEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_026
                            .message("Unsupported buy/sell edit for " + entry.getType())); //$NON-NLS-1$

        var accountProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.PORTFOLIO);
        var cashPostingIndex = LedgerEntryEditSupport.postingIndex(entry, accountProjection.getPrimaryPosting());
        var securityPostingIndex = LedgerEntryEditSupport.postingIndex(entry, portfolioProjection.getPrimaryPosting());

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit,
                        LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO, cashPostingIndex,
                        securityPostingIndex));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerBuySellEdit edit, LedgerProjectionRole accountRole,
                    LedgerProjectionRole portfolioRole, int cashPostingIndex, int securityPostingIndex)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getCashPosting().applyTo(LedgerEntryEditSupport.postingAt(editedEntry, cashPostingIndex));
        edit.getSecurityPosting().applyTo(LedgerEntryEditSupport.postingAt(editedEntry, securityPostingIndex));
        unitPostingUpdater.applyDirect(editedEntry, edit.getUnits());
        LedgerProjectionSupport.descriptor(editedEntry, accountRole);
        LedgerProjectionSupport.descriptor(editedEntry, portfolioRole);
    }
}
