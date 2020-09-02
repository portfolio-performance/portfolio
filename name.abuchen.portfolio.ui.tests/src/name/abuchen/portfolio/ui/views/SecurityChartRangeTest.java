package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartRange;

@SuppressWarnings("nls")
public class SecurityChartRangeTest
{
    private static List<SecurityPrice> prices;

    @BeforeClass
    public static void setupSecurityPrices()
    {
        prices = new ArrayList<>();

        for (int ii = 1; ii < 30; ii++)
        {
            // skip every 10th date to create gaps (to test the binary search)

            if (ii % 10 != 0)
                prices.add(new SecurityPrice(LocalDate.of(2019, 1, ii), Values.Quote.factor()));
        }
    }

    @Test
    public void testRangeIntersection()
    {
        ChartInterval interval = new ChartInterval(LocalDate.parse("2019-01-02"), LocalDate.parse("2019-01-05"));
        ChartRange range = ChartRange.createFor(prices, interval);

        assertThat(range.start, is(1));
        assertThat(range.size, is(4));
    }

    @Test
    public void testRangeIfIntervalStartsBefore()
    {
        ChartInterval interval = new ChartInterval(LocalDate.parse("2018-01-02"), LocalDate.parse("2019-01-05"));
        ChartRange range = ChartRange.createFor(prices, interval);

        assertThat(range.start, is(0));
        assertThat(range.size, is(5));
    }

    @Test
    public void testRangeIfIntervalEndsAfter()
    {
        ChartInterval interval = new ChartInterval(LocalDate.parse("2019-01-02"), LocalDate.parse("2019-12-01"));
        ChartRange range = ChartRange.createFor(prices, interval);

        assertThat(range.start, is(1));
        assertThat(range.size, is(prices.size() - 1));
    }

    @Test
    public void testRangeIfIntervalStartsOnGap()
    {
        ChartInterval interval = new ChartInterval(LocalDate.parse("2019-01-10"), LocalDate.parse("2019-01-12"));
        ChartRange range = ChartRange.createFor(prices, interval);

        assertThat(prices.get(range.start).getDate(), not(is(interval.getStart())));
        assertThat(range.start, is(9));
        assertThat(range.size, is(2));
    }

    @Test
    public void testRangeIfIntervalEndsOnGap()
    {
        ChartInterval interval = new ChartInterval(LocalDate.parse("2019-01-08"), LocalDate.parse("2019-01-10"));
        ChartRange range = ChartRange.createFor(prices, interval);

        assertThat(prices.get(range.start + range.size).getDate(), not(is(interval.getEnd())));
        assertThat(range.start, is(7));
        assertThat(range.size, is(2));
    }

    @Test
    public void testRangeIfIntervalStartsAndEndsOnGap()
    {
        ChartInterval interval = new ChartInterval(LocalDate.parse("2019-01-10"), LocalDate.parse("2019-01-20"));
        ChartRange range = ChartRange.createFor(prices, interval);

        assertThat(prices.get(range.start).getDate(), not(is(interval.getStart())));
        assertThat(prices.get(range.start + range.size).getDate(), not(is(interval.getEnd())));
        assertThat(range.start, is(9));
        assertThat(range.size, is(9));
    }

    @Test
    public void testRangeIfIntervalDoesNotIntersect()
    {
        ChartInterval interval = new ChartInterval(LocalDate.parse("2019-05-01"), LocalDate.parse("2019-05-10"));
        ChartRange range = ChartRange.createFor(prices, interval);

        assertThat(range, nullValue());
    }
}
