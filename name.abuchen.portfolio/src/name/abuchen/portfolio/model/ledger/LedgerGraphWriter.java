package name.abuchen.portfolio.model.ledger;

import java.util.List;
import java.util.Objects;

/**
 * Writes a replacement Ledger graph back to the client after validation.
 * This is internal mutation infrastructure. Contributor code should reach it through a
 * mutation context or a higher-level Ledger write path.
 */
final class LedgerGraphWriter
{
    private LedgerGraphWriter()
    {
    }

    static void addEntry(Ledger ledger, LedgerEntry entry)
    {
        Objects.requireNonNull(ledger).addEntry(Objects.requireNonNull(entry));
    }

    static void removeEntry(Ledger ledger, LedgerEntry entry)
    {
        Objects.requireNonNull(ledger).removeEntry(Objects.requireNonNull(entry));
    }

    static void replaceEntry(Ledger ledger, LedgerEntry current, LedgerEntry replacement)
    {
        removeEntry(ledger, current);
        addEntry(ledger, replacement);
    }

    static void replaceEntryContents(LedgerEntry target, LedgerEntry source)
    {
        Objects.requireNonNull(target);

        var copy = LedgerModelCopy.copyEntry(source);

        target.setUUID(copy.getUUID());
        target.setType(copy.getType());
        target.setDateTime(copy.getDateTime());
        target.setNote(copy.getNote());
        target.setSource(copy.getSource());

        for (var parameter : List.copyOf(target.getParameters()))
            target.removeParameter(parameter);

        for (var posting : List.copyOf(target.getPostings()))
            target.removePosting(posting);

        copy.getParameters().forEach(target::addParameter);
        copy.getPostings().forEach(target::addPosting);
        target.setUpdatedAt(copy.getUpdatedAt());
    }
}
