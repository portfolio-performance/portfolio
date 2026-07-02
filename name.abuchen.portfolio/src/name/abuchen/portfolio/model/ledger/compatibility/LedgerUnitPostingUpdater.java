package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;

/**
 * Updates fee, tax, and forex unit postings on ledger-backed transactions.
 * This class is compatibility mutation support. Contributor code should use it instead of
 * changing unit projections or posting facts directly.
 */
public final class LedgerUnitPostingUpdater
{
    public void apply(LedgerEntry entry, LedgerUnitPostingPatch patch)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(patch);

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyDirect(editedEntry, patch));
    }

    void applyDirect(LedgerEntry entry, LedgerUnitPostingPatch patch)
    {
        for (var edit : patch.getEdits())
        {
            switch (edit.getOperation())
            {
                case ADD -> add(entry, edit);
                case UPDATE -> update(entry, edit);
                case REMOVE -> remove(entry, edit);
                default -> throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_070.message("Unsupported unit posting edit " + edit.getOperation())); //$NON-NLS-1$
            }
        }
    }

    private void add(LedgerEntry entry, LedgerUnitPostingEdit edit)
    {
        LedgerUnitPostingEdit.requireUnitType(edit.getPostingType());

        var posting = new LedgerPosting();

        posting.setType(edit.getPostingType());
        edit.getPostingPatch().applyTo(posting);
        markUnit(posting);
        entry.addPosting(posting);
    }

    private void update(LedgerEntry entry, LedgerUnitPostingEdit edit)
    {
        var posting = LedgerEntryEditSupport.postingByUUID(entry, edit.getPostingUUID());

        LedgerUnitPostingEdit.requireUnitType(posting.getType());
        edit.getPostingPatch().applyTo(posting);
    }

    private void remove(LedgerEntry entry, LedgerUnitPostingEdit edit)
    {
        var posting = LedgerEntryEditSupport.postingByUUID(entry, edit.getPostingUUID());

        LedgerUnitPostingEdit.requireUnitType(posting.getType());
        entry.removePosting(posting);
    }

    private void markUnit(LedgerPosting posting)
    {
        switch (posting.getType())
        {
            case FEE -> {
                posting.setSemanticRole(LedgerPostingSemanticRole.FEE);
                posting.setUnitRole(LedgerPostingUnitRole.FEE);
            }
            case TAX -> {
                posting.setSemanticRole(LedgerPostingSemanticRole.TAX);
                posting.setUnitRole(LedgerPostingUnitRole.TAX);
            }
            case GROSS_VALUE -> {
                posting.setSemanticRole(LedgerPostingSemanticRole.GROSS_VALUE);
                posting.setUnitRole(LedgerPostingUnitRole.GROSS_VALUE);
            }
            default -> throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_071
                            .message("Unsupported unit posting type: " + posting.getType())); //$NON-NLS-1$
        }
    }
}
