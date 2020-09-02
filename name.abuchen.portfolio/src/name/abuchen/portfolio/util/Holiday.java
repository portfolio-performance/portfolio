package name.abuchen.portfolio.util;

import java.time.LocalDate;

public class Holiday
{
    private final HolidayName name;
    private final LocalDate date;

    /* package */ Holiday(HolidayName name, LocalDate date)
    {
        this.name = name;
        this.date = date;
    }

    /* protected */ HolidayName getName()
    {
        return name;
    }

    public String getLabel()
    {
        return name.toString();
    }

    public LocalDate getDate()
    {
        return date;
    }
}
