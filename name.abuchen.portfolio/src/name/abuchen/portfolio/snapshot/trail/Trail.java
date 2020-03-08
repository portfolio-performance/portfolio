package name.abuchen.portfolio.snapshot.trail;

import java.util.Optional;

public final class Trail
{
    private final String label;
    private final TrailRecord record;

    public Trail(String label, TrailRecord record)
    {
        this.label = label;
        this.record = record;
    }

    public static Optional<Trail> of(String label, TrailRecord record)
    {
        if (record == null || record.isEmpty())
            return Optional.empty();

        return Optional.of(new Trail(label, record));
    }

    public String getLabel()
    {
        return label;
    }

    public TrailRecord getRecord()
    {
        return record;
    }
}
