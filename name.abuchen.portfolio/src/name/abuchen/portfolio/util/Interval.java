package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Interval
{

    private LocalDate start;
    private LocalDate end;

    private Interval(LocalDate start, LocalDate end)
    {
        this.start = start;
        this.end = end;
    }

    public static Interval of(LocalDate start, LocalDate end)
    {
        return new Interval(start, end);
    }

    public LocalDate getStart()
    {
        return start;
    }

    public LocalDate getEnd()
    {
        return end;
    }

    public boolean isLongerThan(Interval other)
    {
        return getDays() > other.getDays();
    }

    public boolean contains(LocalDate other)
    {
        return other.isAfter(start) && !other.isAfter(end);
    }

    public long getDays()
    {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Returns an Iterable with all the year contained in the interval.
     * Particularly, if the start of the interval is the last day of a year,
     * that year is not included.
     */
    public Iterable<Integer> iterYears()
    {
        return () -> new Iterator<Integer>()
        {
            LocalDate index = LocalDate.of(start.plusDays(1).getYear(), 1, 1);

            @Override
            public boolean hasNext()
            {
                return !index.isAfter(end);
            }

            @Override
            public Integer next()
            {
                if (!hasNext())
                    throw new NoSuchElementException();
                Integer answer = index.getYear();
                index = index.plusYears(1);
                return answer;
            }
        };
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((end == null) ? 0 : end.hashCode());
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Interval other = (Interval) obj;
        if (end == null)
        {
            if (other.end != null)
                return false;
        }
        else if (!end.equals(other.end))
            return false;
        if (start == null)
        {
            if (other.start != null)
                return false;
        }
        else if (!start.equals(other.start))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return start + " -> " + end; //$NON-NLS-1$
    }
}
