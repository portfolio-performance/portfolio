package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.money.Values;

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

    public boolean contains(LocalDate other)
    {
        return other.isAfter(start) && !other.isAfter(end);
    }

    public boolean contains(LocalDateTime other)
    {
        LocalDate otherDate = other.toLocalDate();
        return otherDate.isAfter(start) && !otherDate.isAfter(end);
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
        List<Year> answer = new ArrayList<>();

        LocalDate index = start.plusDays(1);
        while (!index.isAfter(end))
        {
            answer.add(Year.from(index));
            index = index.plusYears(1);
        }

        return answer;
    }

    /**
     * Returns the list of {@link YearMonth}s contained in the given interval.
     * As the interval excludes the first day (but includes the last day), the
     * YearMonth of the first day is not included.
     */
    public List<YearMonth> getYearMonths()
    {
        List<YearMonth> answer = new ArrayList<>();

        LocalDate index = start.plusDays(1);
        while (!index.isAfter(end))
        {
            answer.add(YearMonth.from(index));
            index = index.plusMonths(1);
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
