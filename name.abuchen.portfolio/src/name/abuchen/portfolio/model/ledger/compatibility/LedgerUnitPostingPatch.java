package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.List;
import java.util.Objects;

/**
 * Carries unit posting data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerUnitPostingPatch
{
    private static final LedgerUnitPostingPatch NONE = new LedgerUnitPostingPatch(List.of());

    private final List<LedgerUnitPostingEdit> edits;

    private LedgerUnitPostingPatch(List<LedgerUnitPostingEdit> edits)
    {
        this.edits = List.copyOf(edits);
    }

    public static LedgerUnitPostingPatch none()
    {
        return NONE;
    }

    public static LedgerUnitPostingPatch of(LedgerUnitPostingEdit first, LedgerUnitPostingEdit... rest)
    {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);

        var edits = new java.util.ArrayList<LedgerUnitPostingEdit>();

        edits.add(first);
        for (var edit : rest)
            edits.add(Objects.requireNonNull(edit));

        return new LedgerUnitPostingPatch(edits);
    }

    public List<LedgerUnitPostingEdit> getEdits()
    {
        return edits;
    }
}
