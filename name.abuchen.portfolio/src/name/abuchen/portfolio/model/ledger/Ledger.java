package name.abuchen.portfolio.model.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores the persisted Ledger entries for a client.
 * This is an internal Ledger model container. Normal contributor code should use creators,
 * editors, converters, or mutation contexts instead of editing the ledger graph directly.
 */
public class Ledger
{
    private final List<LedgerEntry> entries = new ArrayList<>();

    public List<LedgerEntry> getEntries()
    {
        return Collections.unmodifiableList(entries);
    }

    public void addEntry(LedgerEntry entry)
    {
        entries.add(Objects.requireNonNull(entry));
    }

    public boolean removeEntry(LedgerEntry entry)
    {
        return entries.remove(entry);
    }
}
