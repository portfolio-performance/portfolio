package name.abuchen.portfolio.math;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.math.LinearInterpolationPreprocessor.DataSeriesToPreprocess;

public class LinearInterpolationPreprocessorTest
{
    private static final double EPS = 1e-8;
    
    private static final DataSeriesToPreprocess EMPTY_DATA_SERIES = new DataSeriesToPreprocess(new LocalDate[0], new double[0]);

    private static final DataSeriesToPreprocess ONE_ENTRY_SERIES = new DataSeriesToPreprocess(new LocalDate[] {
                    LocalDate.of(2022, 11, 1),
    }, new double[] {
                    1d
    });
    
    private static final DataSeriesToPreprocess TWO_ENTRY_SERIES = new DataSeriesToPreprocess(new LocalDate[] {
                    LocalDate.of(2022, 11, 1),
                    LocalDate.of(2022, 11, 15),
    }, new double[] {
                    1d,
                    15d
    });
    
    private static final DataSeriesToPreprocess TWO_ENTRY_SHORT_SERIES = new DataSeriesToPreprocess(new LocalDate[] {
                    LocalDate.of(2022, 11, 4),
                    LocalDate.of(2022, 11, 13),
    }, new double[] {
                    4d,
                    13d
    });
    
    private static final DataSeriesToPreprocess TWO_ENTRY_NONOVERLAPPING_SERIES = new DataSeriesToPreprocess(new LocalDate[] {
                    LocalDate.of(2022, 12, 1),
                    LocalDate.of(2022, 12, 10),
    }, new double[] {
                    .123d,
                    .456d
    });

    private static final DataSeriesToPreprocess LONG_DATA_SERIES = new DataSeriesToPreprocess(new LocalDate[] {
                    LocalDate.of(2022, 11, 1),
                    LocalDate.of(2022, 11, 2),
                    LocalDate.of(2022, 11, 3),
                    LocalDate.of(2022, 11, 5),
                    LocalDate.of(2022, 11, 7),
                    LocalDate.of(2022, 11, 11),
                    LocalDate.of(2022, 11, 12),
                    LocalDate.of(2022, 11, 13),
                    LocalDate.of(2022, 11, 15),
    }, new double[] {
                    0d,
                    0.1d,
                    1d,
                    0d,
                    1d,
                    -3d,
                    -3.2d,
                    -2d,
                    -1d
    });

    private static final DataSeriesToPreprocess LONG2_DATA_SERIES = new DataSeriesToPreprocess(new LocalDate[] {
                    LocalDate.of(2022, 11, 1),
                    LocalDate.of(2022, 11, 2),
                    LocalDate.of(2022, 11, 6),
                    LocalDate.of(2022, 11, 8),
                    LocalDate.of(2022, 11, 9),
                    LocalDate.of(2022, 11, 11),
                    LocalDate.of(2022, 11, 12),
                    LocalDate.of(2022, 11, 14),
                    LocalDate.of(2022, 11, 15),
    }, new double[] {
                    0d,
                    1d,
                    1d,
                    0d,
                    1d,
                    -3d,
                    -3d,
                    -2d,
                    -1d
    });
    
    @Test
    public void testOnlyEmpty()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(EMPTY_DATA_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);

        assertEquals(0, dates.length);
        assertEquals(0, data0.length);
    }
    
    @Test
    public void testOnlyOneEntry()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(ONE_ENTRY_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);

        assertEquals(1, dates.length);
        assertEquals(1, data0.length);
        
        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);

        assertEquals(1d, data0[0], EPS);
    }
    
    @Test
    public void testOnlyTwoEntry()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(TWO_ENTRY_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);

        assertEquals(2, dates.length);
        assertEquals(2, data0.length);

        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 15), dates[1]);

        assertEquals(1d, data0[0], EPS);
        assertEquals(15d, data0[1], EPS);
    }
    
    @Test
    public void testOnlyLong()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(LONG_DATA_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);

        assertEquals(9, dates.length);
        assertEquals(9, data0.length);

        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 2), dates[1]);
        assertEquals(LocalDate.of(2022, 11, 3), dates[2]);
        assertEquals(LocalDate.of(2022, 11, 5), dates[3]);
        assertEquals(LocalDate.of(2022, 11, 7), dates[4]);
        assertEquals(LocalDate.of(2022, 11, 11), dates[5]);
        assertEquals(LocalDate.of(2022, 11, 12), dates[6]);
        assertEquals(LocalDate.of(2022, 11, 13), dates[7]);
        assertEquals(LocalDate.of(2022, 11, 15), dates[8]);

        assertEquals(0d, data0[0], EPS);
        assertEquals(0.1d, data0[1], EPS);
        assertEquals(1d, data0[2], EPS);
        assertEquals(0d, data0[3], EPS);
        assertEquals(1d, data0[4], EPS);
        assertEquals(-3d, data0[5], EPS);
        assertEquals(-3.2d, data0[6], EPS);
        assertEquals(-2d, data0[7], EPS);
        assertEquals(-1d, data0[8], EPS);
    }
    
    @Test
    public void testEmptyEmpty()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(EMPTY_DATA_SERIES, EMPTY_DATA_SERIES);
        
        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(0, dates.length);
        assertEquals(0, data0.length);
        assertEquals(0, data1.length);
    }
    
    @Test
    public void testEmptyOne()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(EMPTY_DATA_SERIES, ONE_ENTRY_SERIES);
        
        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(0, dates.length);
        assertEquals(0, data0.length);
        assertEquals(0, data1.length);
    }
    
    @Test
    public void testEmptyTwo()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(TWO_ENTRY_SERIES, EMPTY_DATA_SERIES);
        
        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(0, dates.length);
        assertEquals(0, data0.length);
        assertEquals(0, data1.length);
    }
    
    @Test
    public void testEmptyLong()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(EMPTY_DATA_SERIES, LONG_DATA_SERIES);
        
        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(0, dates.length);
        assertEquals(0, data0.length);
        assertEquals(0, data1.length);
    }
    
    @Test
    public void testOneOne()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(ONE_ENTRY_SERIES, ONE_ENTRY_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(1, dates.length);
        assertEquals(1, data0.length);
        assertEquals(1, data1.length);
        
        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);

        assertEquals(1d, data0[0], EPS);
        
        assertEquals(1d, data1[0], EPS);
    }
    
    @Test
    public void testOneTwo()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(ONE_ENTRY_SERIES, TWO_ENTRY_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(1, dates.length);
        assertEquals(1, data0.length);
        assertEquals(1, data1.length);
        
        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);

        assertEquals(1d, data0[0], EPS);
        
        assertEquals(1d, data1[0], EPS);
    }
    
    @Test
    public void testOneLong()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(LONG_DATA_SERIES, ONE_ENTRY_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(1, dates.length);
        assertEquals(1, data0.length);
        assertEquals(1, data1.length);
        
        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);

        assertEquals(0d, data0[0], EPS);
        
        assertEquals(1d, data1[0], EPS);
    }
    
    @Test
    public void testTwoTwo()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(TWO_ENTRY_SERIES, TWO_ENTRY_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(2, dates.length);
        assertEquals(2, data0.length);
        assertEquals(2, data1.length);

        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 15), dates[1]);

        assertEquals(1d, data0[0], EPS);
        assertEquals(15d, data0[1], EPS);
        
        assertEquals(1d, data1[0], EPS);
        assertEquals(15d, data1[1], EPS);
    }
    
    @Test
    public void testTwoLong()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(TWO_ENTRY_SERIES, LONG_DATA_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);


        assertEquals(9, dates.length);
        assertEquals(9, data0.length);
        assertEquals(9, data1.length);

        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 2), dates[1]);
        assertEquals(LocalDate.of(2022, 11, 3), dates[2]);
        assertEquals(LocalDate.of(2022, 11, 5), dates[3]);
        assertEquals(LocalDate.of(2022, 11, 7), dates[4]);
        assertEquals(LocalDate.of(2022, 11, 11), dates[5]);
        assertEquals(LocalDate.of(2022, 11, 12), dates[6]);
        assertEquals(LocalDate.of(2022, 11, 13), dates[7]);
        assertEquals(LocalDate.of(2022, 11, 15), dates[8]);

        assertEquals(1d, data0[0], EPS);
        assertEquals(2d, data0[1], EPS);
        assertEquals(3d, data0[2], EPS);
        assertEquals(5d, data0[3], EPS);
        assertEquals(7d, data0[4], EPS);
        assertEquals(11d, data0[5], EPS);
        assertEquals(12d, data0[6], EPS);
        assertEquals(13d, data0[7], EPS);
        assertEquals(15d, data0[8], EPS);
        
        assertEquals(0d, data1[0], EPS);
        assertEquals(0.1d, data1[1], EPS);
        assertEquals(1d, data1[2], EPS);
        assertEquals(0d, data1[3], EPS);
        assertEquals(1d, data1[4], EPS);
        assertEquals(-3d, data1[5], EPS);
        assertEquals(-3.2d, data1[6], EPS);
        assertEquals(-2d, data1[7], EPS);
        assertEquals(-1d, data1[8], EPS);
    }
    
    @Test
    public void testTwoShortLong()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(TWO_ENTRY_SHORT_SERIES, LONG_DATA_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);


        assertEquals(6, dates.length);
        assertEquals(6, data0.length);
        assertEquals(6, data1.length);

        assertEquals(LocalDate.of(2022, 11, 4), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 5), dates[1]);
        assertEquals(LocalDate.of(2022, 11, 7), dates[2]);
        assertEquals(LocalDate.of(2022, 11, 11), dates[3]);
        assertEquals(LocalDate.of(2022, 11, 12), dates[4]);
        assertEquals(LocalDate.of(2022, 11, 13), dates[5]);

        assertEquals(4d, data0[0], EPS);
        assertEquals(5d, data0[1], EPS);
        assertEquals(7d, data0[2], EPS);
        assertEquals(11d, data0[3], EPS);
        assertEquals(12d, data0[4], EPS);
        assertEquals(13d, data0[5], EPS);
        
        assertEquals(0.5d, data1[0], EPS);
        assertEquals(0d, data1[1], EPS);
        assertEquals(1d, data1[2], EPS);
        assertEquals(-3d, data1[3], EPS);
        assertEquals(-3.2d, data1[4], EPS);
        assertEquals(-2d, data1[5], EPS);
    }
    
    @Test
    public void testLongLong2()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(LONG_DATA_SERIES, LONG2_DATA_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);


        assertEquals(13, dates.length);
        assertEquals(13, data0.length);
        assertEquals(13, data1.length);

        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 2), dates[1]);
        assertEquals(LocalDate.of(2022, 11, 3), dates[2]);
        assertEquals(LocalDate.of(2022, 11, 5), dates[3]);
        assertEquals(LocalDate.of(2022, 11, 6), dates[4]);
        assertEquals(LocalDate.of(2022, 11, 7), dates[5]);
        assertEquals(LocalDate.of(2022, 11, 8), dates[6]);
        assertEquals(LocalDate.of(2022, 11, 9), dates[7]);
        assertEquals(LocalDate.of(2022, 11, 11), dates[8]);
        assertEquals(LocalDate.of(2022, 11, 12), dates[9]);
        assertEquals(LocalDate.of(2022, 11, 13), dates[10]);
        assertEquals(LocalDate.of(2022, 11, 14), dates[11]);
        assertEquals(LocalDate.of(2022, 11, 15), dates[12]);

        assertEquals(0d, data0[0], EPS);
        assertEquals(0.1d, data0[1], EPS);
        assertEquals(1d, data0[2], EPS);
        assertEquals(0d, data0[3], EPS);
        assertEquals(0.5d, data0[4], EPS);
        assertEquals(1d, data0[5], EPS);
        assertEquals(0d, data0[6], EPS);
        assertEquals(-1d, data0[7], EPS);
        assertEquals(-3d, data0[8], EPS);
        assertEquals(-3.2d, data0[9], EPS);
        assertEquals(-2d, data0[10], EPS);
        assertEquals(-1.5d, data0[11], EPS);
        assertEquals(-1d, data0[12], EPS);
        
        assertEquals(0d, data1[0], EPS);
        assertEquals(1d, data1[1], EPS);
        assertEquals(1d, data1[2], EPS);
        assertEquals(1d, data1[3], EPS);
        assertEquals(1d, data1[4], EPS);
        assertEquals(0.5d, data1[5], EPS);
        assertEquals(0d, data1[6], EPS);
        assertEquals(1d, data1[7], EPS);
        assertEquals(-3d, data1[8], EPS);
        assertEquals(-3d, data1[9], EPS);
        assertEquals(-2.5d, data1[10], EPS);
        assertEquals(-2d, data1[11], EPS);
        assertEquals(-1d, data1[12], EPS);
    }
    
    @Test
    public void testTwoLongLong2()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(LONG_DATA_SERIES, LONG2_DATA_SERIES, TWO_ENTRY_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);
        double[] data2 = preprocessor.getPreprocessedData(2);


        assertEquals(13, dates.length);
        assertEquals(13, data0.length);
        assertEquals(13, data1.length);
        assertEquals(13, data2.length);

        assertEquals(LocalDate.of(2022, 11, 1), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 2), dates[1]);
        assertEquals(LocalDate.of(2022, 11, 3), dates[2]);
        assertEquals(LocalDate.of(2022, 11, 5), dates[3]);
        assertEquals(LocalDate.of(2022, 11, 6), dates[4]);
        assertEquals(LocalDate.of(2022, 11, 7), dates[5]);
        assertEquals(LocalDate.of(2022, 11, 8), dates[6]);
        assertEquals(LocalDate.of(2022, 11, 9), dates[7]);
        assertEquals(LocalDate.of(2022, 11, 11), dates[8]);
        assertEquals(LocalDate.of(2022, 11, 12), dates[9]);
        assertEquals(LocalDate.of(2022, 11, 13), dates[10]);
        assertEquals(LocalDate.of(2022, 11, 14), dates[11]);
        assertEquals(LocalDate.of(2022, 11, 15), dates[12]);

        assertEquals(0d, data0[0], EPS);
        assertEquals(0.1d, data0[1], EPS);
        assertEquals(1d, data0[2], EPS);
        assertEquals(0d, data0[3], EPS);
        assertEquals(0.5d, data0[4], EPS);
        assertEquals(1d, data0[5], EPS);
        assertEquals(0d, data0[6], EPS);
        assertEquals(-1d, data0[7], EPS);
        assertEquals(-3d, data0[8], EPS);
        assertEquals(-3.2d, data0[9], EPS);
        assertEquals(-2d, data0[10], EPS);
        assertEquals(-1.5d, data0[11], EPS);
        assertEquals(-1d, data0[12], EPS);
        
        assertEquals(0d, data1[0], EPS);
        assertEquals(1d, data1[1], EPS);
        assertEquals(1d, data1[2], EPS);
        assertEquals(1d, data1[3], EPS);
        assertEquals(1d, data1[4], EPS);
        assertEquals(0.5d, data1[5], EPS);
        assertEquals(0d, data1[6], EPS);
        assertEquals(1d, data1[7], EPS);
        assertEquals(-3d, data1[8], EPS);
        assertEquals(-3d, data1[9], EPS);
        assertEquals(-2.5d, data1[10], EPS);
        assertEquals(-2d, data1[11], EPS);
        assertEquals(-1d, data1[12], EPS);
        
        assertEquals(1d, data2[0], EPS);
        assertEquals(2d, data2[1], EPS);
        assertEquals(3d, data2[2], EPS);
        assertEquals(5d, data2[3], EPS);
        assertEquals(6d, data2[4], EPS);
        assertEquals(7d, data2[5], EPS);
        assertEquals(8d, data2[6], EPS);
        assertEquals(9d, data2[7], EPS);
        assertEquals(11d, data2[8], EPS);
        assertEquals(12d, data2[9], EPS);
        assertEquals(13d, data2[10], EPS);
        assertEquals(14d, data2[11], EPS);
        assertEquals(15d, data2[12], EPS);
    }
    
    @Test
    public void testTwoShortLongLong2()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(LONG_DATA_SERIES, LONG2_DATA_SERIES, TWO_ENTRY_SHORT_SERIES);

        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);
        double[] data2 = preprocessor.getPreprocessedData(2);


        assertEquals(9, dates.length);
        assertEquals(9, data0.length);
        assertEquals(9, data1.length);
        assertEquals(9, data2.length);

        assertEquals(LocalDate.of(2022, 11, 4), dates[0]);
        assertEquals(LocalDate.of(2022, 11, 5), dates[1]);
        assertEquals(LocalDate.of(2022, 11, 6), dates[2]);
        assertEquals(LocalDate.of(2022, 11, 7), dates[3]);
        assertEquals(LocalDate.of(2022, 11, 8), dates[4]);
        assertEquals(LocalDate.of(2022, 11, 9), dates[5]);
        assertEquals(LocalDate.of(2022, 11, 11), dates[6]);
        assertEquals(LocalDate.of(2022, 11, 12), dates[7]);
        assertEquals(LocalDate.of(2022, 11, 13), dates[8]);

        assertEquals(0.5d, data0[0], EPS);
        assertEquals(0d, data0[1], EPS);
        assertEquals(0.5d, data0[2], EPS);
        assertEquals(1d, data0[3], EPS);
        assertEquals(0d, data0[4], EPS);
        assertEquals(-1d, data0[5], EPS);
        assertEquals(-3d, data0[6], EPS);
        assertEquals(-3.2d, data0[7], EPS);
        assertEquals(-2d, data0[8], EPS);
        
        assertEquals(1d, data1[0], EPS);
        assertEquals(1d, data1[1], EPS);
        assertEquals(1d, data1[2], EPS);
        assertEquals(0.5d, data1[3], EPS);
        assertEquals(0d, data1[4], EPS);
        assertEquals(1d, data1[5], EPS);
        assertEquals(-3d, data1[6], EPS);
        assertEquals(-3d, data1[7], EPS);
        assertEquals(-2.5d, data1[8], EPS);
        
        assertEquals(4d, data2[0], EPS);
        assertEquals(5d, data2[1], EPS);
        assertEquals(6d, data2[2], EPS);
        assertEquals(7d, data2[3], EPS);
        assertEquals(8d, data2[4], EPS);
        assertEquals(9d, data2[5], EPS);
        assertEquals(11d, data2[6], EPS);
        assertEquals(12d, data2[7], EPS);
        assertEquals(13d, data2[8], EPS);
    }
    
    @Test
    public void testNonOverlapping()
    {
        LinearInterpolationPreprocessor preprocessor =
                        new LinearInterpolationPreprocessor(TWO_ENTRY_SERIES, TWO_ENTRY_NONOVERLAPPING_SERIES);
        
        LocalDate[] dates = preprocessor.getNormalizedLocalDates();
        double[] data0 = preprocessor.getPreprocessedData(0);
        double[] data1 = preprocessor.getPreprocessedData(1);

        assertEquals(0, dates.length);
        assertEquals(0, data0.length);
        assertEquals(0, data1.length);
    }
}
