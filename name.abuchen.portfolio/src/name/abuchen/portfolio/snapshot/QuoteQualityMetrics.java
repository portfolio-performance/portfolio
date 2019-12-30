package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class QuoteQualityMetrics
{
    private final Security security;

    private Interval checkInterval;
    private List<Interval> missingIntervals = new ArrayList<>();

    private int expectedNumberOfQuotes;
    private int actualNumberOfQuotes;

    public QuoteQualityMetrics(Security security)
    {
        this.security = security;

        calculate();
    }

    /**
     * Returns the interval for which the quote quality metrics have been
     * calculated. The check interval is absent if there are no quotes available
     * at all.
     */
    public Optional<Interval> getCheckInterval()
    {
        return Optional.ofNullable(checkInterval);
    }

    /**
     * Completeness is the percentage of available quotes against the expected
     * number of quotes in the checked interval. The interval starts with the
     * first quote. The interval ends with the current date unless the quote
     * download is deactivated; then it ends with the last quote.
     */
    public double getCompleteness()
    {
        return expectedNumberOfQuotes == 0 ? 0d : actualNumberOfQuotes / (double) expectedNumberOfQuotes;
    }

    /**
     * Returns a list of intervals that describe the days for which quotes are
     * missing.
     */
    public List<Interval> getMissingIntervals()
    {
        return missingIntervals;
    }

    public int getExpectedNumberOfQuotes()
    {
        return expectedNumberOfQuotes;
    }

    public int getActualNumberOfQuotes()
    {
        return actualNumberOfQuotes;
    }

    public final void calculate()
    {
        List<SecurityPrice> copy = new ArrayList<>(security.getPrices());
        if (copy.isEmpty())
            return;

        final LocalDate start = copy.get(0).getDate();
        final LocalDate end = QuoteFeed.MANUAL.equals(security.getFeed()) ? copy.get(copy.size() - 1).getDate()
                        : LocalDate.now();

        this.checkInterval = Interval.of(start, end);

        TradeCalendar tradeCalendar = TradeCalendarManager.getInstance(security);
        List<LocalDate> missingDays = new ArrayList<>();

        expectedNumberOfQuotes = 0;
        actualNumberOfQuotes = 0;
        int index = 0;

        LocalDate currentDay = start;
        while (!currentDay.isAfter(end))
        {
            boolean hasQuote = index < copy.size() && currentDay.equals(copy.get(index).getDate());

            if (hasQuote)
            {
                actualNumberOfQuotes += 1;
                expectedNumberOfQuotes += 1;
                index += 1;
            }
            else if (!tradeCalendar.isHoliday(currentDay))
            {
                missingDays.add(currentDay);
                expectedNumberOfQuotes += 1;
            }

            currentDay = currentDay.plusDays(1);
        }

        missingIntervals = conflate(missingDays);
    }

    private List<Interval> conflate(List<LocalDate> dates)
    {
        List<Interval> answer = new ArrayList<>();

        Interval interval = null;
        for (LocalDate date : dates)
        {
            if (interval != null && interval.getEnd().plusDays(1).equals(date))
            {
                interval = Interval.of(interval.getStart(), date);
                answer.set(answer.size() - 1, interval);
            }
            else
            {
                interval = Interval.of(date, date);
                answer.add(interval);
            }
        }

        return answer;
    }
}
