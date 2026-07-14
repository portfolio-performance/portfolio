package name.abuchen.portfolio.model.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds the ledger entries of a client.
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
