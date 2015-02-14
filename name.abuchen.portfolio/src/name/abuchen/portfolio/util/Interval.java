package name.abuchen.portfolio.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Interval
{
    private Instant start;
    private Instant end;

    private Interval(Instant start, Instant end)
    {
        this.start = start;
        this.end = end;
    }

    public static Interval of(Instant start, Instant end)
    {
        return new Interval(start, end);
    }

    public Instant getStart()
    {
        return start;
    }

    public Instant getEnd()
    {
        return end;
    }

    public Duration toDuration()
    {
        return Duration.between(start, end);
    }

    public boolean isLongerThan(Interval other)
    {
        return toDuration().compareTo(other.toDuration()) > 0;
    }

    public long getDays()
    {
        return ChronoUnit.DAYS.between(start, end);
    }
}
