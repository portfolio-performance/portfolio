package name.abuchen.portfolio.snapshot.trail;

public final class Trail
{
    private final String label;
    private final TrailRecord record;

    public Trail(String label, TrailRecord record)
    {
        this.label = label;
        this.record = record;
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
