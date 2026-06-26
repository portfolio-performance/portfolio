package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Carries creation units data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerCreationUnits
{
    private static final LedgerCreationUnits NONE = new LedgerCreationUnits(List.of());

    private final List<LedgerCreationUnit> units;

    private LedgerCreationUnits(List<LedgerCreationUnit> units)
    {
        this.units = List.copyOf(units);
    }

    public static LedgerCreationUnits none()
    {
        return NONE;
    }

    public static LedgerCreationUnits of(LedgerCreationUnit first, LedgerCreationUnit... rest)
    {
        var units = new ArrayList<LedgerCreationUnit>();

        units.add(Objects.requireNonNull(first));

        for (var unit : rest)
            units.add(Objects.requireNonNull(unit));

        return new LedgerCreationUnits(units);
    }

    public List<LedgerCreationUnit> getUnits()
    {
        return Collections.unmodifiableList(units);
    }
}
