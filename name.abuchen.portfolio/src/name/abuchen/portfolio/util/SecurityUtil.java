package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.Type;

public class SecurityUtil
{
    private SecurityUtil()
    {
    }

    public static void addDividendEvent(final Security security, final LocalDate exDay)
    {
        Optional<SecurityEvent> securityEvent = security.getEvents().stream()
                        .filter(e -> e.getType() == SecurityEvent.Type.STOCK_EX_DIVIDEND && e.getDate().equals(exDay))
                        .findAny();
        if (!securityEvent.isPresent())
        {
            security.addEvent(new SecurityEvent(exDay, SecurityEvent.Type.STOCK_EX_DIVIDEND, ""));
        }
    }

    public static Optional<SecurityEvent> findLastDividendEvent(final Security security, final LocalDate date)
    {
        return security.getEvents().stream() //
                        .filter(e -> e.getType() == Type.STOCK_EX_DIVIDEND) // only the stock dividend events
                        .filter(e -> !e.getDate().isAfter(date)) // only events before the date
                        .sorted((o1, o2) -> {
                            if (o1.getDate() == null)
                                return 1;
                            return -1 * o1.getDate().compareTo(o2.getDate());
                        }) // sort descending
                        .findFirst();
    }
}
