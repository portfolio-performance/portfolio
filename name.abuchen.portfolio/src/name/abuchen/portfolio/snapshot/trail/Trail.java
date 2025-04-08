package name.abuchen.portfolio.snapshot.trail;

import java.util.Optional;

import name.abuchen.portfolio.money.Values;

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

    public int getDepth()
    {
        return depth(1, record);
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(label).append('\n');

        int depth = getDepth();
        addRow(buffer, record, depth - 1, depth);

        return buffer.toString();
    }

    private void addRow(StringBuilder buffer, TrailRecord trail, int level, int depth)
    {
        for (TrailRecord child : trail.getInputs())
            addRow(buffer, child, level - 1, depth);

        buffer.append(String.format("%-10s | %-30s | %5s |", //$NON-NLS-1$
                        trail.getDate() != null ? Values.Date.format(trail.getDate()) : "", trail.getLabel(), //$NON-NLS-1$
                        trail.getShares() != null ? Values.Share.format(trail.getShares()) : "")); //$NON-NLS-1$

        for (int index = 0; index < depth; index++)
        {
            String value = ""; //$NON-NLS-1$
            if (index == level)
                value = trail.getValue() != null ? Values.Money.format(trail.getValue()) : "n/a"; //$NON-NLS-1$

            buffer.append(String.format(" %12s |", value)); //$NON-NLS-1$
        }

        buffer.append('\n');
    }

    private int depth(int level, TrailRecord t)
    {
        if (t.getInputs().isEmpty())
            return level;

        int d = level;

        for (TrailRecord child : t.getInputs())
            d = Math.max(d, depth(level + 1, child));

        return d;
    }
}
