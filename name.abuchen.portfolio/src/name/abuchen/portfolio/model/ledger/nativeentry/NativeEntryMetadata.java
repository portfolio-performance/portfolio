package name.abuchen.portfolio.model.ledger.nativeentry;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Carries native entry metadata input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This input object is not persisted directly. The assembler copies supported values onto
 * the resulting Ledger entry.
 * </p>
 */
public final class NativeEntryMetadata
{
    private final LocalDateTime dateTime;
    private final String note;
    private final String source;

    private NativeEntryMetadata(LocalDateTime dateTime, String note, String source)
    {
        this.dateTime = Objects.requireNonNull(dateTime);
        this.note = note;
        this.source = source;
    }

    public static NativeEntryMetadata of(LocalDateTime dateTime)
    {
        return new NativeEntryMetadata(dateTime, null, null);
    }

    public NativeEntryMetadata note(String note)
    {
        return new NativeEntryMetadata(dateTime, note, source);
    }

    public NativeEntryMetadata source(String source)
    {
        return new NativeEntryMetadata(dateTime, note, source);
    }

    LocalDateTime getDateTime()
    {
        return dateTime;
    }

    String getNote()
    {
        return note;
    }

    String getSource()
    {
        return source;
    }
}
