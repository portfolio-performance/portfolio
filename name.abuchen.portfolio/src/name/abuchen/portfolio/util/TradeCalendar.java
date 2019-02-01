package name.abuchen.portfolio.util;

import java.text.Collator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TradeCalendar implements Comparable<TradeCalendar>
{
    public static final String EMPTY_CODE = "empty"; //$NON-NLS-1$

    private static final Set<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final String code;
    private final String description;

    private final List<HolidayType> holidayTypes = new ArrayList<>();
    private final Map<Integer, Map<LocalDate, Holiday>> cache = new HashMap<Integer, Map<LocalDate, Holiday>>()
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Map<LocalDate, Holiday> get(Object key)
        {
            return super.computeIfAbsent((Integer) key, year -> holidayTypes.stream().map(type -> type.getHoliday(year))
                            .filter(Objects::nonNull).collect(Collectors.toMap(Holiday::getDate, t -> t, (r, l) -> r)));
        }

    };

    /* package */ TradeCalendar(String code, String description)
    {
        this.code = Objects.requireNonNull(code);
        this.description = Objects.requireNonNull(description);
    }

    /* package */ void add(HolidayType type)
    {
        this.holidayTypes.add(type);
    }

    public String getCode()
    {
        return code;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString()
    {
        return getDescription();
    }

    public boolean isHoliday(LocalDate date)
    {
        if (getCode() == EMPTY_CODE)
            return true;
        if (WEEKEND.contains(date.getDayOfWeek()))
            return true;

        return cache.get(date.getYear()).containsKey(date);
    }

    public Collection<Holiday> getHolidays(int year)
    {
        return cache.get(year).values();
    }

    @Override
    public int compareTo(TradeCalendar other)
    {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        return collator.compare(getDescription(), other.getDescription());
    }

    @Override
    public int hashCode()
    {
        return code.hashCode();
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
        TradeCalendar other = (TradeCalendar) obj;
        return code.equals(other.code);
    }
}
