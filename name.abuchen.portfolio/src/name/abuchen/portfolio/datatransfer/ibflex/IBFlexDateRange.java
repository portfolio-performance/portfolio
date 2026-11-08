package name.abuchen.portfolio.datatransfer.ibflex;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An inclusive date range to request from the IB Flex Web Service. Overrides
 * the period configured in the Flex Query.
 */
public record IBFlexDateRange(LocalDate from, LocalDate to)
{
    /**
     * Maximum number of days (inclusive) a single SendRequest may cover.
     */
    public static final int MAX_DAYS_PER_REQUEST = 365;

    /**
     * Interactive Brokers reports statement dates in US Eastern time.
     */
    public static final ZoneId IB_ZONE = ZoneId.of("America/New_York");

    public IBFlexDateRange
    {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (from.isAfter(to))
            throw new IllegalArgumentException(from + " is after " + to);
    }

    /**
     * Returns the current date in the time zone of Interactive Brokers.
     */
    public static LocalDate today()
    {
        return LocalDate.now(IB_ZONE);
    }

    /**
     * Returns the range to fetch: everything from the day of the last import
     * (inclusive, as more transactions might have been booked on that day)
     * through today, or the maximum period of a single request if nothing has
     * been imported yet.
     */
    public static IBFlexDateRange sinceLastImport(LocalDateTime lastImport, LocalDate today)
    {
        if (lastImport == null)
            return new IBFlexDateRange(today.minusDays(MAX_DAYS_PER_REQUEST - 1L), today);

        var from = lastImport.toLocalDate();
        return new IBFlexDateRange(from.isAfter(today) ? today : from, today);
    }

    /**
     * Splits the range into consecutive chunks which each can be fetched with a
     * single request.
     */
    public List<IBFlexDateRange> split()
    {
        var chunks = new ArrayList<IBFlexDateRange>();

        var start = from;
        while (!start.isAfter(to))
        {
            var end = start.plusDays(MAX_DAYS_PER_REQUEST - 1L);
            if (end.isAfter(to))
                end = to;
            chunks.add(new IBFlexDateRange(start, end));
            start = end.plusDays(1);
        }

        return chunks;
    }
}
