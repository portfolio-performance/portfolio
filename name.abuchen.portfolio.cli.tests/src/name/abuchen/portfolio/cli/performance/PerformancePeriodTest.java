package name.abuchen.portfolio.cli.performance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.FacetHarness;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

/**
 * {@link PerformancePeriod#FACET} at its own interface: the options, the rules
 * between them and the resolved days in <code>meta</code>. Today is 2026-10-16.
 */
public class PerformancePeriodTest
{
    private static FacetHarness.Result<PerformancePeriod> run(String... args)
    {
        return FacetHarness.run(PerformancePeriod.FACET, null, args);
    }

    @Test
    public void fromAndToAreTheFirstAndLastDayAndCoresIntervalStartsTheDayBefore()
    {
        var result = run("--from", "2026-01-02", "--to", "2026-09-30");

        var period = result.value();
        assertThat(period.firstDay(), is(LocalDate.parse("2026-01-02")));
        assertThat(period.lastDay(), is(LocalDate.parse("2026-09-30")));
        assertThat(period.code(), is(Optional.empty()));
        assertThat(period.interval().getStart(), is(LocalDate.parse("2026-01-01")));
        assertThat(period.interval().getEnd(), is(LocalDate.parse("2026-09-30")));

        assertThat(result.meta().get("from").getAsString(), is("2026-01-02"));
        assertThat(result.meta().get("to").getAsString(), is("2026-09-30"));
        assertThat(result.meta().has("period"), is(false));
    }

    @Test
    public void aSingleDayPeriodIsValid()
    {
        var result = run("--from", "2026-09-30", "--to", "2026-09-30");

        assertThat(result.meta().get("from").getAsString(), is("2026-09-30"));
        assertThat(result.meta().get("to").getAsString(), is("2026-09-30"));
    }

    @Test
    public void toDefaultsToToday()
    {
        var result = run("--from", "2026-01-01");

        assertThat(result.meta().get("to").getAsString(), is("2026-10-16"));
    }

    @Test
    public void periodCodesResolveThroughCoreAndAreEchoedWithTheirFirstAndLastDay() throws IOException
    {
        var today = LocalDate.parse("2026-10-16");

        for (var code : List.of("L1Y0", "L0Y6", "Y2025", "S2026-01-15"))
        {
            var interval = ReportingPeriod.from(code).toInterval(today);

            var result = run("--period", code);

            assertThat(code, result.value().interval(), is(interval));
            assertThat(code, result.meta().get("from").getAsString(),
                            is(interval.getStart().plusDays(1).toString()));
            assertThat(code, result.meta().get("to").getAsString(), is(interval.getEnd().toString()));
            assertThat(code, result.meta().get("period").getAsString(), is(code));
        }
    }

    @Test
    public void aCalendarYearIsEchoedFromItsFirstToItsLastDay()
    {
        var result = run("--period", "Y2025");

        assertThat(result.meta().get("from").getAsString(), is("2025-01-01"));
        assertThat(result.meta().get("to").getAsString(), is("2025-12-31"));
    }

    @Test
    public void yearToDateAndCurrentMonthResolveAgainstTheInjectedClock()
    {
        assertThat(run("--period", "YTD").value().firstDay(), is(LocalDate.parse("2026-01-01")));
        assertThat(run("--period", "X").value().firstDay(), is(LocalDate.parse("2026-01-01")));
        assertThat(run("--period", "M").value().firstDay(), is(LocalDate.parse("2026-10-01")));
        assertThat(run("--period", "M").value().lastDay(), is(LocalDate.parse("2026-10-16")));
    }

    @Test
    public void anUnknownPeriodCodeIsAnInvalidOptionValue()
    {
        for (var code : List.of("nonsense", "Y20x5", "9X9Y", "L1Y", " "))
            assertThat(code, run("--period", code).code(), is("invalid-option-value"));
    }

    @Test
    public void aPeriodTogetherWithFromOrToConflicts()
    {
        assertThat(run("--period", "L1Y0", "--from", "2026-01-01").code(), is("conflicting-options"));
        assertThat(run("--period", "L1Y0", "--to", "2026-06-30").code(), is("conflicting-options"));
    }

    @Test
    public void withoutAPeriodOrFromAPeriodIsRequired()
    {
        assertThat(run().code(), is("period-required"));
        assertThat(run("--to", "2026-06-30").code(), is("period-required"));
    }

    @Test
    public void aFromAfterToIsAnInvalidDateRange()
    {
        assertThat(run("--from", "2026-06-30", "--to", "2026-06-29").code(), is("invalid-date-range"));
    }

    @Test
    public void aPeriodCodeThatStartsAfterItEndsIsAnInvalidDateRange()
    {
        assertThat(run("--period", "S2026-10-16").code(), is("invalid-date-range"));
    }
}
