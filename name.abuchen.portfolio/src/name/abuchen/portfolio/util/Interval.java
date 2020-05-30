package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import name.abuchen.portfolio.money.Values;

/**
 * The {@code Interval} class represents a period by a start and end date. The
 * interval is <em>half-open</em> — it <em>excludes</em> the start date but
 * <em>includes</em> the end date: {@code (start,end]}.
 */
public final class Interval
{

    private final LocalDate start;
    private final LocalDate end;

    private Interval(LocalDate start, LocalDate end)
    {
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
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

    /**
     * Tests whether the given date is included in the interval. The interval is
     * <em>half-open</em> — it <em>excludes</em> the start date but
     * <em>includes</em> the end date: {@code (start,end]}.
     */
    public boolean contains(LocalDate other)
    {
        return other.isAfter(start) && !other.isAfter(end);
    }

    /**
     * Tests whether the given date is included in the interval. The interval is
     * <em>half-open</em> — it <em>excludes</em> the start date but
     * <em>includes</em> the end date: {@code (start,end]}.
     */
    public boolean contains(LocalDateTime other)
    {
        return contains(other.toLocalDate());
    }

    public long getDays()
    {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Returns the list of {@link Year}s contained in the given interval. As the
     * interval excludes the first day (but includes the last day), the Year of
     * the first day is not included.
     */
    public List<Year> getYears()
    {
        return collect(Year::from);
    }

    /**
     * Returns the list of {@link YearMonth}s contained in the given interval.
     * As the interval excludes the first day (but includes the last day), the
     * YearMonth of the first day is not included.
     */
    public List<YearMonth> getYearMonths()
    {
        return collect(YearMonth::from);
    }

    private <T> List<T> collect(Function<LocalDate, T> collector)
    {
        List<T> answer = new ArrayList<>();

        T lastItem = null;

        LocalDate index = start.plusDays(1); // first day not in range
        while (!index.isAfter(end))
        {
            T item = collector.apply(index);
            if (!item.equals(lastItem))
            {
                answer.add(item);
                lastItem = item;
            }
            index = index.plusDays(1);
        }

        return answer;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + end.hashCode();
        result = prime * result + start.hashCode();
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
        return end.equals(other.end) && start.equals(other.start);
    }

    @Override
    public String toString()
    {
        return Values.Date.format(start) + " - " + Values.Date.format(end); //$NON-NLS-1$
    }
}
