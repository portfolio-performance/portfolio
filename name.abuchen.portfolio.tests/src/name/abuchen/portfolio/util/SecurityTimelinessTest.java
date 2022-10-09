package name.abuchen.portfolio.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class SecurityTimelinessTest
{
    private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 5, 6);
    private Clock clock;

    public SecurityTimelinessTest()
    {
        this.clock = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }

    @Test
    public void testStaleIfNoLatestFeed()
    {
        Security security = new Security();
        security.setRetired(false);

        SecurityTimeliness st = new SecurityTimeliness(security, 7, this.clock);

        assertTrue(st.isStale());
    }

    @Test
    public void testNotStaleIfRetired()
    {
        Security security = new Security();
        security.setRetired(true);

        SecurityTimeliness st = new SecurityTimeliness(security, 7, this.clock);

        assertFalse(st.isStale());
    }

    @Test
    public void testStaleIfNotUpdatedWithin8DaysWith1HolidayAndWeekend()
    {
        Security security = new Security();
        security.setRetired(false);
        security.setLatest(new LatestSecurityPrice(LocalDate.of(2020, 4, 23), 10));

        SecurityTimeliness st = new SecurityTimeliness(security, 7, this.clock);

        assertTrue(st.isStale());
    }

    @Test
    public void testNotStaleIfNotUpdatedWithin7DaysWith1HolidayAndWeekend()
    {
        Security security = new Security();
        security.setRetired(false);
        security.setLatest(new LatestSecurityPrice(LocalDate.of(2020, 4, 24), 10));

        SecurityTimeliness st = new SecurityTimeliness(security, 7, this.clock);

        assertFalse(st.isStale());
    }

    @Test
    public void testNotStaleIfUpdatedToday()
    {
        Security security = new Security();
        security.setRetired(false);
        security.setLatest(new LatestSecurityPrice(LocalDate.of(2020, 5, 6), 10));

        SecurityTimeliness st = new SecurityTimeliness(security, 7, this.clock);

        assertFalse(st.isStale());
    }

    @Test
    public void testNotStaleIfUpdatedYesterday()
    {
        Security security = new Security();
        security.setRetired(false);
        security.setLatest(new LatestSecurityPrice(LocalDate.of(2020, 5, 5), 10));

        SecurityTimeliness st = new SecurityTimeliness(security, 7, this.clock);

        assertFalse(st.isStale());
    }

    @Test
    public void testStaleIfUpdatedYesterdayAndInterval0()
    {
        Security security = new Security();
        security.setRetired(false);
        security.setLatest(new LatestSecurityPrice(LocalDate.of(2020, 5, 5), 10));

        SecurityTimeliness st = new SecurityTimeliness(security, 0, this.clock);

        assertTrue(st.isStale());
    }

    @Test
    public void testNoHolidaysOrWeekendsIfSecurityHasNoCalendar()
    {
        Security security = new Security();
        security.setRetired(false);
        security.setLatest(new LatestSecurityPrice(LocalDate.of(2020, 4, 28), 10));
        security.setCalendar("empty");

        SecurityTimeliness st = new SecurityTimeliness(security, 7, this.clock);

        assertTrue(st.isStale());
    }
}
