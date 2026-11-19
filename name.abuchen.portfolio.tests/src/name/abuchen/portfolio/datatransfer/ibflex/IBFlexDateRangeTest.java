package name.abuchen.portfolio.datatransfer.ibflex;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

public class IBFlexDateRangeTest
{
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 7);

    @Test
    public void testRejectsFromAfterTo()
    {
        assertThrows(IllegalArgumentException.class,
                        () -> new IBFlexDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    public void testWithoutLastImportFetchesMaximumSingleRequest()
    {
        var range = IBFlexDateRange.sinceLastImport(null, TODAY);

        assertThat(range, is(new IBFlexDateRange(LocalDate.of(2025, 11, 8), TODAY)));
        assertThat(range.split().size(), is(1));
    }

    @Test
    public void testWithLastImportIncludesDayOfLastImport()
    {
        var range = IBFlexDateRange.sinceLastImport(LocalDateTime.of(2026, 10, 20, 15, 30), TODAY);
        assertThat(range, is(new IBFlexDateRange(LocalDate.of(2026, 10, 20), TODAY)));
    }

    @Test
    public void testWithLastImportToday()
    {
        var range = IBFlexDateRange.sinceLastImport(TODAY.atTime(9, 0), TODAY);
        assertThat(range, is(new IBFlexDateRange(TODAY, TODAY)));
    }

    @Test
    public void testLastImportInTheFutureCollapsesToToday()
    {
        // e.g. the local date is already ahead of the date in New York
        var range = IBFlexDateRange.sinceLastImport(TODAY.plusDays(1).atTime(1, 0), TODAY);
        assertThat(range, is(new IBFlexDateRange(TODAY, TODAY)));
    }

    @Test
    public void testLastImportLongAgoIsSplitIntoMultipleRequests()
    {
        var range = IBFlexDateRange.sinceLastImport(LocalDateTime.of(2024, 6, 1, 0, 0), TODAY);

        assertThat(range, is(new IBFlexDateRange(LocalDate.of(2024, 6, 1), TODAY)));
        assertThat(range.split().size(), is(3));
    }

    @Test
    public void testSplitKeepsShortRangeIntact()
    {
        var range = new IBFlexDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(range.split(), is(List.of(range)));
    }

    @Test
    public void testSplitLongRangeIntoConsecutiveChunks()
    {
        var range = new IBFlexDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

        assertThat(range.split(), is(List.of( //
                        new IBFlexDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 30)),
                        new IBFlexDateRange(LocalDate.of(2024, 12, 31), LocalDate.of(2025, 12, 30)),
                        new IBFlexDateRange(LocalDate.of(2025, 12, 31), LocalDate.of(2025, 12, 31)))));
    }
}
