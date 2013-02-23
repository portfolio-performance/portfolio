package name.abuchen.portfolio.snapshot;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.DateMidnight;
import org.junit.Test;

public class AggregationTest
{
    @Test
    public void testWeekly()
    {
        assertThat(Aggregation.Period.WEEKLY.getStartDateFor(new DateMidnight(2011, 10, 17)), //
                        is(new DateMidnight(2011, 10, 17)));
        assertThat(Aggregation.Period.WEEKLY.getStartDateFor(new DateMidnight(2012, 1, 13)), //
                        is(new DateMidnight(2012, 1, 9)));
        assertThat(Aggregation.Period.WEEKLY.getStartDateFor(new DateMidnight(2012, 8, 10)), //
                        is(new DateMidnight(2012, 8, 6)));
        assertThat(Aggregation.Period.WEEKLY.getStartDateFor(new DateMidnight(2012, 9, 23)), //
                        is(new DateMidnight(2012, 9, 17)));
    }

    @Test
    public void testMonthly()
    {
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(new DateMidnight(2011, 10, 17)), //
                        is(new DateMidnight(2011, 10, 1)));
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(new DateMidnight(2012, 1, 13)), //
                        is(new DateMidnight(2012, 1, 1)));
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(new DateMidnight(2012, 8, 10)), //
                        is(new DateMidnight(2012, 8, 1)));
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(new DateMidnight(2012, 9, 23)), //
                        is(new DateMidnight(2012, 9, 1)));
    }

    @Test
    public void testQuartlerly()
    {
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(new DateMidnight(2011, 10, 17)), //
                        is(new DateMidnight(2011, 10, 1)));
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(new DateMidnight(2012, 1, 13)), //
                        is(new DateMidnight(2012, 1, 1)));
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(new DateMidnight(2012, 8, 10)), //
                        is(new DateMidnight(2012, 7, 1)));
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(new DateMidnight(2012, 9, 23)), //
                        is(new DateMidnight(2012, 7, 1)));
    }

    @Test
    public void testYearly()
    {
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(new DateMidnight(2011, 10, 17)), //
                        is(new DateMidnight(2011, 1, 1)));
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(new DateMidnight(2012, 1, 13)), //
                        is(new DateMidnight(2012, 1, 1)));
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(new DateMidnight(2012, 8, 10)), //
                        is(new DateMidnight(2012, 1, 1)));
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(new DateMidnight(2012, 9, 23)), //
                        is(new DateMidnight(2012, 1, 1)));
    }

}
