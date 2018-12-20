package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Dates
{
    public static int daysBetween(LocalDate start, LocalDate end)
    {
        if (start.isAfter(end))
        {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        return (int) ChronoUnit.DAYS.between(start, end);
    }

    public static int tradingDaysBetween(LocalDate start, LocalDate end)
    {
        if (start.isAfter(end))
        {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        TradeCalendar calender = TradeCalendarManager.getDefaultInstance();

        int days = 0;

        while (start.isBefore(end))
        {
            if (!calender.isHoliday(end))
                days++;

            end = end.minusDays(1);
        }

        return days;
    }
}
