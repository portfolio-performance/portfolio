package name.abuchen.portfolio.util;

import java.time.Clock;
import java.time.LocalDate;

import name.abuchen.portfolio.model.Security;

public final class SecurityTimeliness
{
    private Security security;
    private TradeCalendar tradeCalendar;
    private Clock clock;
    private int numberOfTradeDaysToLookBack;

    public SecurityTimeliness(Security security, int numberOfTradeDaysToLookBack, Clock clock)
    {
        this.security = security;
        this.numberOfTradeDaysToLookBack = numberOfTradeDaysToLookBack;
        this.clock = clock;
        this.tradeCalendar = TradeCalendarManager.getInstance(security);
    }

    public boolean isStale()
    {
        final LocalDate daysAgo = this.getStartDate();

        return !this.security.isRetired()
                        && (this.security.getLatest() == null || this.security.getLatest().getDate().isBefore(daysAgo));
    }

    private LocalDate getStartDate()
    {
        LocalDate currentDay = LocalDate.now(this.clock);
        while (this.numberOfTradeDaysToLookBack > 0)
        {
            currentDay = currentDay.minusDays(1);

            if (this.tradeCalendar.isHoliday(currentDay) || this.tradeCalendar.isWeekend(currentDay))
            {
                continue;
            }

            numberOfTradeDaysToLookBack--;
        }

        return currentDay;
    }
}
