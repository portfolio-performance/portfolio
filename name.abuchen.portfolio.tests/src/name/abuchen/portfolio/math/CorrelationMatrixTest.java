package name.abuchen.portfolio.math;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.IntPredicate;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.math.CorrelationMatrix.DataFrame;

public class CorrelationMatrixTest
{
    private static class SampleDataFrame implements CorrelationMatrix.DataFrame
    {
        private LocalDate[] dates;
        private double[] values;

        public SampleDataFrame(LocalDate[] dates, double[] values)
        {
            this.dates = dates;
            this.values = values;
        }

        @Override
        public LocalDate[] dates()
        {
            return dates;
        }

        @Override
        public double[] values()
        {
            return values;
        }

        @Override
        public IntPredicate filter()
        {
            return i -> true;
        }
    }

    @Test
    public void testSimpleFormula()
    {
        // https://www.crashkurs-statistik.de/der-korrelationskoeffizient-nach-pearson/

        LocalDate[] dates = new LocalDate[7];
        for (int ii = 0; ii < dates.length; ii++)
            dates[ii] = LocalDate.now().plusDays(ii);

        DataFrame x = new SampleDataFrame(dates, new double[] { 4, 21, 2, 11, 14, 2, 6 });
        DataFrame y = new SampleDataFrame(dates, new double[] { 70, 63, 82, 65, 61, 74, 84 });

        CorrelationMatrix<DataFrame> matrix = new CorrelationMatrix<>(Arrays.asList(x, y));

        assertThat(matrix.getCorrelation(0, 1).orElseThrow(IllegalArgumentException::new).getR(),
                        is(IsCloseTo.closeTo(-0.7422210414d, 0.001)));
    }

    @Test
    public void testThatOnlyMatchingDatesAreUsed()
    {
        LocalDate[] datesX = new LocalDate[10];
        for (int ii = 0; ii < datesX.length; ii++)
            datesX[ii] = LocalDate.now().plusDays(ii);

        DataFrame x = new SampleDataFrame(datesX, new double[] { 0, 4, 21, 2, 0, 11, 14, 2, 6, 0 });

        LocalDate[] datesY = new LocalDate[7];
        System.arraycopy(datesX, 1, datesY, 0, 3);
        System.arraycopy(datesX, 5, datesY, 3, 4);

        DataFrame y = new SampleDataFrame(datesY, new double[] { 70, 63, 82, 65, 61, 74, 84 });

        CorrelationMatrix<DataFrame> matrix = new CorrelationMatrix<>(Arrays.asList(x, y));

        assertThat(matrix.getCorrelation(0, 1).orElseThrow(IllegalArgumentException::new).getR(),
                        is(IsCloseTo.closeTo(-0.7422210414d, 0.001)));

        CorrelationMatrix<DataFrame> matrix2 = new CorrelationMatrix<>(Arrays.asList(y, x));

        assertThat(matrix2.getCorrelation(0, 1).orElseThrow(IllegalArgumentException::new).getR(),
                        is(IsCloseTo.closeTo(-0.7422210414d, 0.001)));
    }

    @Test
    public void testFilter()
    {
        LocalDate[] datesX = new LocalDate[10];
        for (int ii = 0; ii < datesX.length; ii++)
            datesX[ii] = LocalDate.now().plusDays(ii);

        DataFrame x = new SampleDataFrame(datesX, new double[] { 0, 4, 21, 2, 0, 11, 14, 2, 6, 0 })
        {
            @Override
            public IntPredicate filter()
            {
                return ii -> this.values()[ii] != 0d;
            }
        };

        DataFrame y = new SampleDataFrame(datesX, new double[] { 80, 70, 63, 82, 100, 65, 61, 74, 84, 110 });

        CorrelationMatrix<DataFrame> matrix = new CorrelationMatrix<>(Arrays.asList(x, y));
        assertThat(matrix.getCorrelation(0, 1).orElseThrow(IllegalArgumentException::new).getR(),
                        is(IsCloseTo.closeTo(-0.7422210414d, 0.001)));
    }
}
