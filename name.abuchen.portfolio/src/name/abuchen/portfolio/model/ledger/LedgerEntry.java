package name.abuchen.portfolio.model.ledger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;

/**
 * A single financial event, for example a buy, a dividend, or a cash transfer.
 * <p>
 * The entry carries the identity, the type, and the date/time of the event. What
 * the event actually moves is expressed by its {@link LedgerPosting postings};
 * further typed facts, such as the ex-date of a dividend, are attached as
 * {@link LedgerParameter parameters}.
 */
public class LedgerEntry
{
    private String uuid;
    private LedgerEntryType type;
    private LocalDateTime dateTime;
    private String note;
    private String source;
    private Instant updatedAt;
    private final List<LedgerParameter<?>> parameters = new ArrayList<>();
    private final List<LedgerPosting> postings = new ArrayList<>();

    public LedgerEntry()
    {
        this(UUID.randomUUID().toString());
    }

    public LedgerEntry(String uuid)
    {
        this.uuid = Objects.requireNonNull(uuid);
        this.updatedAt = Instant.now();
    }

    public String getUUID()
    {
        return uuid;
    }

    public void setUUID(String uuid)
    {
        this.uuid = Objects.requireNonNull(uuid);
        touch();
    }

    public LedgerEntryType getType()
    {
        return type;
    }

    public void setType(LedgerEntryType type)
    {
        this.type = type;
        touch();
    }

    public LocalDateTime getDateTime()
    {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime)
    {
        this.dateTime = dateTime;
        touch();
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
        touch();
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
        touch();
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public List<LedgerParameter<?>> getParameters()
    {
        return Collections.unmodifiableList(parameters);
    }

    public void addParameter(LedgerParameter<?> parameter)
    {
        parameters.add(Objects.requireNonNull(parameter));
        touch();
    }

    public boolean removeParameter(LedgerParameter<?> parameter)
    {
        var removed = parameters.remove(parameter);

        if (removed)
            touch();

        return removed;
    }

    public List<LedgerPosting> getPostings()
    {
        return Collections.unmodifiableList(postings);
    }

    public void addPosting(LedgerPosting posting)
    {
        postings.add(Objects.requireNonNull(posting));
        touch();
    }

    public boolean removePosting(LedgerPosting posting)
    {
        var removed = postings.remove(posting);

        if (removed)
            touch();

        return removed;
    }

    private void touch()
    {
        this.updatedAt = Instant.now();
    }
}
