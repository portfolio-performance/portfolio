package name.abuchen.portfolio.snapshot.trail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.money.Money;

/* package */ class DefaultTrail implements TrailRecord
{
    private final LocalDate date;
    private final String label;
    private final Long shares;
    private final Money value;

    private final List<TrailRecord> children = new ArrayList<>();

    public DefaultTrail(LocalDate date, String label, Long shares, Money value, TrailRecord... inputs)
    {
        this.date = date;
        this.label = label;
        this.shares = shares;
        this.value = value;

        this.children.addAll(Arrays.asList(inputs));
    }

    @Override
    public LocalDate getDate()
    {
        return date;
    }

    @Override
    public String getLabel()
    {
        return label;
    }

    @Override
    public Long getShares()
    {
        return shares;
    }

    @Override
    public Money getValue()
    {
        return value;
    }

    @Override
    public List<TrailRecord> getInputs()
    {
        return Collections.unmodifiableList(children);
    }
}
