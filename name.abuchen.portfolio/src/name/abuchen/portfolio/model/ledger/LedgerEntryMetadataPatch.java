package name.abuchen.portfolio.model.ledger;

import java.time.LocalDateTime;

/**
 * Describes a metadata change for a persisted Ledger entry.
 * This is internal edit data used by Ledger mutation support. Contributor code should
 * normally use a compatibility write path that builds these patches.
 */
public final class LedgerEntryMetadataPatch
{
    private final LedgerFieldEdit<LocalDateTime> dateTime;
    private final LedgerFieldEdit<String> note;
    private final LedgerFieldEdit<String> source;

    private LedgerEntryMetadataPatch(Builder builder)
    {
        this.dateTime = builder.dateTime;
        this.note = builder.note;
        this.source = builder.source;
    }

    public static LedgerEntryMetadataPatch none()
    {
        return builder().build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public LedgerFieldEdit<LocalDateTime> getDateTime()
    {
        return dateTime;
    }

    public LedgerFieldEdit<String> getNote()
    {
        return note;
    }

    public LedgerFieldEdit<String> getSource()
    {
        return source;
    }

    public static final class Builder
    {
        private LedgerFieldEdit<LocalDateTime> dateTime = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<String> note = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<String> source = LedgerFieldEdit.omitted();

        private Builder()
        {
        }

        public Builder dateTime(LocalDateTime dateTime)
        {
            this.dateTime = LedgerFieldEdit.set(dateTime);
            return this;
        }

        public Builder note(String note)
        {
            this.note = note != null ? LedgerFieldEdit.set(note) : LedgerFieldEdit.clear();
            return this;
        }

        public Builder clearNote()
        {
            this.note = LedgerFieldEdit.clear();
            return this;
        }

        public Builder source(String source)
        {
            this.source = source != null ? LedgerFieldEdit.set(source) : LedgerFieldEdit.clear();
            return this;
        }

        public Builder clearSource()
        {
            this.source = LedgerFieldEdit.clear();
            return this;
        }

        public LedgerEntryMetadataPatch build()
        {
            return new LedgerEntryMetadataPatch(this);
        }
    }
}
