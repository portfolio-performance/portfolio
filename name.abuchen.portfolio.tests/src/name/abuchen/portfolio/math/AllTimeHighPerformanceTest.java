package name.abuchen.portfolio.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.snapshot.PerformanceIndex;

public class AllTimeHighPerformanceTest
{
    private PerformanceIndex emptyIndex;
    private PerformanceIndex oneEntryIndex;
    private PerformanceIndex athAtTheEndIndex;
    private PerformanceIndex athAtTheBeginnigIndex;
    private PerformanceIndex athInTheMiddle;
    private PerformanceIndex bankruptcy;
    private PerformanceIndex worseThanBankruptcy; // Asset that looses more than 100%

    @Before
    public void setUp()
    {
        emptyIndex = Mockito.mock(PerformanceIndex.class);
        when(emptyIndex.getAccumulatedPercentage()).thenReturn(new double[] {});
        when(emptyIndex.getDates()).thenReturn(new LocalDate[] {});

        oneEntryIndex = Mockito.mock(PerformanceIndex.class);
        when(oneEntryIndex.getAccumulatedPercentage()).thenReturn(new double[] { 0d });
        when(oneEntryIndex.getDates()).thenReturn(new LocalDate[] { LocalDate.of(2022, 11, 10) });

        athAtTheEndIndex = Mockito.mock(PerformanceIndex.class);
        when(athAtTheEndIndex.getAccumulatedPercentage()).thenReturn(new double[] { 0d, -.1d, -.1d, -.05d, .1d, .15d});
        when(athAtTheEndIndex.getDates()).thenReturn(new LocalDate[] { LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3), LocalDate.of(2022, 11, 4), LocalDate.of(2022, 11, 6),
                        LocalDate.of(2022, 11, 8), LocalDate.of(2022, 11, 11) });

        athAtTheBeginnigIndex = Mockito.mock(PerformanceIndex.class);
        when(athAtTheBeginnigIndex.getAccumulatedPercentage()).thenReturn(new double[] { 0d, -.1d, -.1d, -.05d, -.2d, -.15d});
        when(athAtTheBeginnigIndex.getDates()).thenReturn(new LocalDate[] { LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3), LocalDate.of(2022, 11, 4), LocalDate.of(2022, 11, 6),
                        LocalDate.of(2022, 11, 8), LocalDate.of(2022, 11, 11) });

        athInTheMiddle = Mockito.mock(PerformanceIndex.class);
        when(athInTheMiddle.getAccumulatedPercentage()).thenReturn(new double[] { 0d, -.1d, -.2d, 1d, .5d, 0d});
        when(athInTheMiddle.getDates()).thenReturn(new LocalDate[] { LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3), LocalDate.of(2022, 11, 4), LocalDate.of(2022, 11, 6),
                        LocalDate.of(2022, 11, 8), LocalDate.of(2022, 11, 11) });

        bankruptcy = Mockito.mock(PerformanceIndex.class);
        when(bankruptcy.getAccumulatedPercentage()).thenReturn(new double[] { 0d, .1d, .23d, -.9d, -1d, -1d});
        when(bankruptcy.getDates()).thenReturn(new LocalDate[] { LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3), LocalDate.of(2022, 11, 4), LocalDate.of(2022, 11, 6),
                        LocalDate.of(2022, 11, 8), LocalDate.of(2022, 11, 11) });

        worseThanBankruptcy = Mockito.mock(PerformanceIndex.class);
        when(worseThanBankruptcy.getAccumulatedPercentage()).thenReturn(new double[] { 0d, -.1d, -.23d, -.9d, -1d, -1.5d});
        when(worseThanBankruptcy.getDates()).thenReturn(new LocalDate[] { LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3), LocalDate.of(2022, 11, 4), LocalDate.of(2022, 11, 6),
                        LocalDate.of(2022, 11, 8), LocalDate.of(2022, 11, 11) });
    }

    @Test
    public void testEmptyIndex()
    {
        AllTimeHighPerformance ath = new AllTimeHighPerformance(emptyIndex);

        assertTrue(ath.getDistance().isEmpty());
        assertTrue(ath.getDate().isEmpty());
    }

    @Test
    public void testOneEntryIndex()
    {
        AllTimeHighPerformance ath = new AllTimeHighPerformance(oneEntryIndex);

        assertApproximatelyEquals(OptionalDouble.of(0d), ath.getDistance());
        assertEquals(Optional.of(LocalDate.of(2022, 11, 10)), ath.getDate());
    }

    @Test
    public void testATHAtTheEndIndex()
    {
        AllTimeHighPerformance ath = new AllTimeHighPerformance(athAtTheEndIndex);

        assertApproximatelyEquals(OptionalDouble.of(0d), ath.getDistance());
        assertEquals(Optional.of(LocalDate.of(2022, 11, 11)), ath.getDate());
    }

    @Test
    public void testATHAtTheBeginningIndex()
    {
        AllTimeHighPerformance ath = new AllTimeHighPerformance(athAtTheBeginnigIndex);

        assertApproximatelyEquals(OptionalDouble.of(-.15d), ath.getDistance());
        assertEquals(Optional.of(LocalDate.of(2022, 11, 2)), ath.getDate());
    }

    @Test
    public void testATHInTheMiddleIndex()
    {
        AllTimeHighPerformance ath = new AllTimeHighPerformance(athInTheMiddle);

        assertApproximatelyEquals(OptionalDouble.of(-.5d), ath.getDistance());
        assertEquals(Optional.of(LocalDate.of(2022, 11, 6)), ath.getDate());
    }

    @Test
    public void testBankruptcyIndex()
    {
        AllTimeHighPerformance ath = new AllTimeHighPerformance(bankruptcy);

        assertApproximatelyEquals(OptionalDouble.of(-1d), ath.getDistance());
        assertEquals(Optional.of(LocalDate.of(2022, 11, 4)), ath.getDate());
    }

    @Test
    public void testWorseThanBankruptcyIndex()
    {
        AllTimeHighPerformance ath = new AllTimeHighPerformance(worseThanBankruptcy);

        assertApproximatelyEquals(OptionalDouble.of(-1.5d), ath.getDistance());
        assertEquals(Optional.of(LocalDate.of(2022, 11, 2)), ath.getDate());
    }
    
    private static void assertApproximatelyEquals(OptionalDouble double1, OptionalDouble double2)
    {
        assertTrue(double1.isPresent() == double2.isPresent());
        assertEquals(double1.getAsDouble(), double2.getAsDouble(), 0.00001d);
    }
}
