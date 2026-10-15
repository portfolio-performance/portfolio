package name.abuchen.portfolio.ui.views.panes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class AbsolutePerformancePaneTest
{
    @Test
    public void testUsesTodayByDefault()
    {
        var pane = new AbsolutePerformancePane();
        var before = LocalDate.now();
        var date = pane.getSnapshotDate();
        var after = LocalDate.now();
        assertThat(date.equals(before) || date.equals(after), is(true));
    }

    @Test
    public void testReadsCurrentHistoricalDateOnEveryRecalculation()
    {
        var pane = new AbsolutePerformancePane();
        var date = new AtomicReference<>(LocalDate.of(2024, 2, 15));
        pane.setSnapshotDateSupplier(date::get);

        assertThat(pane.getSnapshotDate(), is(LocalDate.of(2024, 2, 15)));
        date.set(LocalDate.of(2024, 3, 15));
        assertThat(pane.getSnapshotDate(), is(LocalDate.of(2024, 3, 15)));
    }
}
