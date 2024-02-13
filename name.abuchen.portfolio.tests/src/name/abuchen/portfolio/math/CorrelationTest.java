package name.abuchen.portfolio.math;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.math.Correlation.CorrelationNotDefinedException;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

public class CorrelationTest
{
    private static final double EPS = 1e-8;
    
    private static final PerformanceIndex PERFORMANCE_1 = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
                        LocalDate.of(2022, 11, 1),
                        LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3),
                        LocalDate.of(2022, 11, 4),
        };
        double[] accumulatedPercentage = new double[] {
                        0d,
                        0.5d,
                        -0.25d,
                        0.125d,
        };
        when(PERFORMANCE_1.getDates()).thenReturn(dates);
        when(PERFORMANCE_1.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }

    private static final PerformanceIndex PERFORMANCE_2 = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
                        LocalDate.of(2022, 11, 1),
                        LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3),
                        LocalDate.of(2022, 11, 4),
        };
        double[] accumulatedPercentage = new double[] {
                        0d,
                        -0.5d,
                        0d,
                        -0.5d,
        };
        when(PERFORMANCE_2.getDates()).thenReturn(dates);
        when(PERFORMANCE_2.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }

    private static final PerformanceIndex PERFORMANCE_3 = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
                        LocalDate.of(2022, 11, 1),
                        LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3),
                        LocalDate.of(2022, 11, 4),
        };
        double[] accumulatedPercentage = new double[] {
                        0d,
                        0.5d,
                        0.5d,
                        -0.25d,
        };
        when(PERFORMANCE_3.getDates()).thenReturn(dates);
        when(PERFORMANCE_3.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }

    private static final PerformanceIndex PERFORMANCE_MONOTONE = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
                        LocalDate.of(2022, 11, 1),
                        LocalDate.of(2022, 11, 2),
                        LocalDate.of(2022, 11, 3),
                        LocalDate.of(2022, 11, 4),
        };
        double[] accumulatedPercentage = new double[] {
                        0d,
                        1d,
                        3d,
                        7d,
        };
        when(PERFORMANCE_MONOTONE.getDates()).thenReturn(dates);
        when(PERFORMANCE_MONOTONE.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }

    private static final PerformanceIndex PERFORMANCE_NONOVERLAPPING = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
                        LocalDate.of(2022, 12, 1),
                        LocalDate.of(2022, 12, 2),
                        LocalDate.of(2022, 12, 3),
                        LocalDate.of(2022, 12, 4),
        };
        double[] accumulatedPercentage = new double[] {
                        0d,
                        0.1d,
                        0.2d,
                        0.3d,
        };
        when(PERFORMANCE_NONOVERLAPPING.getDates()).thenReturn(dates);
        when(PERFORMANCE_NONOVERLAPPING.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }

    private static final PerformanceIndex PERFORMANCE_TOO_SHORT1 = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
                        LocalDate.of(2022, 12, 2),
                        LocalDate.of(2022, 12, 3),
        };
        double[] accumulatedPercentage = new double[] {
                        0d,
                        0.1d,
        };
        when(PERFORMANCE_TOO_SHORT1.getDates()).thenReturn(dates);
        when(PERFORMANCE_TOO_SHORT1.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }

    private static final PerformanceIndex PERFORMANCE_TOO_SHORT2 = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
                        LocalDate.of(2022, 12, 4),
        };
        double[] accumulatedPercentage = new double[] {
                        0d,
        };
        when(PERFORMANCE_TOO_SHORT2.getDates()).thenReturn(dates);
        when(PERFORMANCE_TOO_SHORT2.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }

    private static final PerformanceIndex PERFORMANCE_EMPTY = Mockito.mock(PerformanceIndex.class);
    static
    {
        LocalDate[] dates = new LocalDate[] {
        };
        double[] accumulatedPercentage = new double[] {
        };
        when(PERFORMANCE_EMPTY.getDates()).thenReturn(dates);
        when(PERFORMANCE_EMPTY.getAccumulatedPercentage()).thenReturn(accumulatedPercentage);
    }
    
    @Test
    public void testPerfectCorrelation1() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_1, PERFORMANCE_1);
        double coefficient = correlation.calculateCorrelationCoefficient();
        assertEquals(1d, coefficient, EPS);
    }
    
    @Test
    public void testPerfectCorrelation2() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_2, PERFORMANCE_2);
        double coefficient = correlation.calculateCorrelationCoefficient();
        assertEquals(1d, coefficient, EPS);
    }
    
    @Test
    public void testPerfectCorrelation3() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_3, PERFORMANCE_3);
        double coefficient = correlation.calculateCorrelationCoefficient();
        assertEquals(1d, coefficient, EPS);
    }
    
    @Test
    public void testNegativeCorrelation() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_1, PERFORMANCE_2);
        double coefficient = correlation.calculateCorrelationCoefficient();
        assertEquals(-1d, coefficient, EPS);
    }
    
    @Test
    public void testUncorrelated1() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_1, PERFORMANCE_3);
        double coefficient = correlation.calculateCorrelationCoefficient();
        assertEquals(0d, coefficient, EPS);
    }
    
    @Test
    public void testUncorrelated2() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_2, PERFORMANCE_3);
        double coefficient = correlation.calculateCorrelationCoefficient();
        assertEquals(0d, coefficient, EPS);
    }
    
    @Test(expected = CorrelationNotDefinedException.class)
    public void testNoVariance() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_1, PERFORMANCE_MONOTONE);
        correlation.calculateCorrelationCoefficient();
    }
    
    @Test(expected = CorrelationNotDefinedException.class)
    public void testNoVariance2() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_MONOTONE, PERFORMANCE_2);
        correlation.calculateCorrelationCoefficient();
    }
    
    @Test(expected = CorrelationNotDefinedException.class)
    public void testNotEnoughData1() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_TOO_SHORT1, PERFORMANCE_1);
        correlation.calculateCorrelationCoefficient();
    }
    
    @Test(expected = CorrelationNotDefinedException.class)
    public void testNotEnoughData2() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_2, PERFORMANCE_TOO_SHORT2);
        correlation.calculateCorrelationCoefficient();
    }
    
    @Test(expected = CorrelationNotDefinedException.class)
    public void testNotEnoughData3() throws CorrelationNotDefinedException
    {
        Correlation correlation = new Correlation(PERFORMANCE_EMPTY, PERFORMANCE_3);
        correlation.calculateCorrelationCoefficient();
    }
}
