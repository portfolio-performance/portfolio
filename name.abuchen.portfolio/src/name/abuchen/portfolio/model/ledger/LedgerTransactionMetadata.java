package name.abuchen.portfolio.model.ledger;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Carries common transaction metadata for Ledger creator and editor calls.
 * This is compatibility-layer input data for date, note, and source values. It is not a
 * direct Ledger mutation API.
 */
public final class LedgerTransactionMetadata
{
    private final LocalDateTime dateTime;
    private final String note;
    private final String source;

    private LedgerTransactionMetadata(LocalDateTime dateTime, String note, String source)
    {
        this.dateTime = Objects.requireNonNull(dateTime);
        this.note = note;
        this.source = source;
    }

    public static LedgerTransactionMetadata of(LocalDateTime dateTime)
    {
        return new LedgerTransactionMetadata(dateTime, null, null);
    }

    public LedgerTransactionMetadata withNote(String note)
    {
        return new LedgerTransactionMetadata(dateTime, note, source);
    }

    public LedgerTransactionMetadata withSource(String source)
    {
        return new LedgerTransactionMetadata(dateTime, note, source);
    }

    public LocalDateTime getDateTime()
    {
        return dateTime;
    }

    public String getNote()
    {
        return note;
    }

    public String getSource()
    {
        return source;
    }
}
