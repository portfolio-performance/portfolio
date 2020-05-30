package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.Test;

public class AggregationTest
{
    @Test
    public void testWeekly()
    {
        // first day of week is locale dependent
        Locale locale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);

        try
        {
            assertThat(Aggregation.Period.WEEKLY.getStartDateFor(LocalDate.of(2011, 10, 17)), //
                            is(LocalDate.of(2011, 10, 17)));
            assertThat(Aggregation.Period.WEEKLY.getStartDateFor(LocalDate.of(2012, 1, 13)), //
                            is(LocalDate.of(2012, 1, 9)));
            assertThat(Aggregation.Period.WEEKLY.getStartDateFor(LocalDate.of(2012, 8, 10)), //
                            is(LocalDate.of(2012, 8, 6)));
            assertThat(Aggregation.Period.WEEKLY.getStartDateFor(LocalDate.of(2012, 9, 23)), //
                            is(LocalDate.of(2012, 9, 17)));
        }
        finally
        {
            Locale.setDefault(locale);
        }
    }

    @Test
    public void testMonthly()
    {
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(LocalDate.of(2011, 10, 17)), //
                        is(LocalDate.of(2011, 10, 1)));
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(LocalDate.of(2012, 1, 13)), //
                        is(LocalDate.of(2012, 1, 1)));
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(LocalDate.of(2012, 8, 10)), //
                        is(LocalDate.of(2012, 8, 1)));
        assertThat(Aggregation.Period.MONTHLY.getStartDateFor(LocalDate.of(2012, 9, 23)), //
                        is(LocalDate.of(2012, 9, 1)));
    }

    @Test
    public void testQuartlerly()
    {
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(LocalDate.of(2011, 10, 17)), //
                        is(LocalDate.of(2011, 10, 1)));
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(LocalDate.of(2012, 1, 13)), //
                        is(LocalDate.of(2012, 1, 1)));
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(LocalDate.of(2012, 8, 10)), //
                        is(LocalDate.of(2012, 7, 1)));
        assertThat(Aggregation.Period.QUARTERLY.getStartDateFor(LocalDate.of(2012, 9, 23)), //
                        is(LocalDate.of(2012, 7, 1)));
    }

    @Test
    public void testYearly()
    {
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(LocalDate.of(2011, 10, 17)), //
                        is(LocalDate.of(2011, 1, 1)));
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(LocalDate.of(2012, 1, 13)), //
                        is(LocalDate.of(2012, 1, 1)));
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(LocalDate.of(2012, 8, 10)), //
                        is(LocalDate.of(2012, 1, 1)));
        assertThat(Aggregation.Period.YEARLY.getStartDateFor(LocalDate.of(2012, 9, 23)), //
                        is(LocalDate.of(2012, 1, 1)));
    }

}
