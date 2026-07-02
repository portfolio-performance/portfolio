package name.abuchen.portfolio.model.ledger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;

/**
 * Represents one persisted Ledger transaction entry.
 * This is an internal Ledger model type. Normal contributor code should not mutate entries
 * directly; it should use a creator, editor, converter, deleter, or mutation context.
 */
public class LedgerEntry
{
    private String uuid;
    private LedgerEntryType type;
    private LocalDateTime dateTime;
    private String note;
    private String source;
    private Instant updatedAt;
    private String generatedByPlanKey;
    private LocalDate planExecutionDate;
    private Integer planExecutionSequence;
    private String preferredViewKind;
    private List<LedgerParameter<?>> parameters = new ArrayList<>();
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

    public String getGeneratedByPlanKey()
    {
        return generatedByPlanKey;
    }

    public void setGeneratedByPlanKey(String generatedByPlanKey)
    {
        this.generatedByPlanKey = generatedByPlanKey;
        touch();
    }

    public LocalDate getPlanExecutionDate()
    {
        return planExecutionDate;
    }

    public void setPlanExecutionDate(LocalDate planExecutionDate)
    {
        this.planExecutionDate = planExecutionDate;
        touch();
    }

    public Integer getPlanExecutionSequence()
    {
        return planExecutionSequence;
    }

    public void setPlanExecutionSequence(Integer planExecutionSequence)
    {
        this.planExecutionSequence = planExecutionSequence;
        touch();
    }

    public String getPreferredViewKind()
    {
        return preferredViewKind;
    }

    public void setPreferredViewKind(String preferredViewKind)
    {
        this.preferredViewKind = preferredViewKind;
        touch();
    }

    public List<LedgerParameter<?>> getParameters()
    {
        return Collections.unmodifiableList(parameters());
    }

    public void addParameter(LedgerParameter<?> parameter)
    {
        parameters().add(Objects.requireNonNull(parameter));
        touch();
    }

    public boolean removeParameter(LedgerParameter<?> parameter)
    {
        var removed = parameters().remove(parameter);

        if (removed)
            touch();

        return removed;
    }

    private List<LedgerParameter<?>> parameters()
    {
        if (parameters == null)
            parameters = new ArrayList<>();

        return parameters;
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
