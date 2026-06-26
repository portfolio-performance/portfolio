package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;

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

    private void applyDirect(LedgerEntry entry, LedgerUnitPostingPatch patch)
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
        entry.addPosting(posting);
        addUnitMemberships(entry, posting);
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
        removeUnitMemberships(entry, posting);
    }

    private void addUnitMemberships(LedgerEntry entry, LedgerPosting posting)
    {
        var role = unitMembershipRole(posting);

        entry.getProjectionRefs().forEach(projection -> projection.addMembership(posting.getUUID(), role));
    }

    private void removeUnitMemberships(LedgerEntry entry, LedgerPosting posting)
    {
        entry.getProjectionRefs().forEach(projection -> projection.removeMembershipsForPostingUUID(posting.getUUID()));
    }

    private ProjectionMembershipRole unitMembershipRole(LedgerPosting posting)
    {
        return switch (posting.getType())
        {
            case FEE -> ProjectionMembershipRole.FEE_UNIT;
            case TAX -> ProjectionMembershipRole.TAX_UNIT;
            case GROSS_VALUE -> ProjectionMembershipRole.GROSS_VALUE_UNIT;
            default -> throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_071.message("Unsupported unit posting type: " + posting.getType())); //$NON-NLS-1$
        };
    }
}
