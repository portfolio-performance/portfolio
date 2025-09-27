package name.abuchen.portfolio.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import name.abuchen.portfolio.Messages;

/**
 * Starting 2026, the Tel Aviv Stock Exchange will trade Monday through Friday.
 * Before, it was operating on a Sunday to Thursday schedule.
 */
/* package */ class TelAvivStockExchangeTradeCalendar extends TradeCalendar
{
    private static final Set<DayOfWeek> WEEKEND_BEFORE_2026 = EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    private static final Set<DayOfWeek> WEEKEND_STARTING_2026 = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public TelAvivStockExchangeTradeCalendar()
    {
        super("tlv", Messages.LabelTradeCalendarTLV, EnumSet.noneOf(DayOfWeek.class)); //$NON-NLS-1$
    }

    @Override
    public boolean isWeekend(LocalDate date)
    {
        if (date.getYear() >= 2026)
            return WEEKEND_STARTING_2026.contains(date.getDayOfWeek());
        else
            return WEEKEND_BEFORE_2026.contains(date.getDayOfWeek());
    }
}
