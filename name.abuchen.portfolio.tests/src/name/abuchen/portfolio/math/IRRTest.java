package name.abuchen.portfolio.math;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

public class IRRTest
{

    @Test
    public void testSimpleExcelTestCase()
    {
        double result = IRR.calculate(Arrays.asList( //
                        LocalDate.of(2010, Month.JANUARY, 1), //
                        LocalDate.of(2010, Month.DECEMBER, 31)), //
                        Arrays.asList(-200d, 210d));

        result = new BigDecimal(result).setScale(8, RoundingMode.HALF_UP).doubleValue();
        double excel = new BigDecimal(0.050140747d).setScale(8, RoundingMode.HALF_UP).doubleValue();

        assertThat(result, is(excel));
    }

    @Test
    public void testComplexExcelTestCaseWithSlowCurve()
    {
        double result = IRR.calculate(Arrays.asList( //
                        LocalDate.of(2002, Month.NOVEMBER, 30), //
                        LocalDate.of(2007, Month.JUNE, 11), //
                        LocalDate.of(2008, Month.MAY, 11), //
                        LocalDate.of(2009, Month.MAY, 1), //
                        LocalDate.of(2010, Month.JUNE, 1), //
                        LocalDate.of(2011, Month.MAY, 2), //
                        LocalDate.of(2012, Month.APRIL, 30), //
                        LocalDate.of(2012, Month.DECEMBER, 6)), //
                        Arrays.asList(-4398d, 200d, 270d, 280d, 280d, 300d, 330d, 14508d));

        double excel = 0.1444629967d;

        assertThat(result, IsCloseTo.closeTo(excel, 0.0001d));
    }

    // issue #1904
    @Test
    public void testSmallConvergenceInterval()
    {
        double result = IRR.calculate(Arrays.asList(
                        LocalDate.of(2019, Month.MAY, 24),
                        LocalDate.of(2020, Month.JANUARY, 13),
                        LocalDate.of(2020, Month.JUNE, 29)),
                        Arrays.asList(-1560.94, -1160d, 42.80));

        double excel = -0.999251643;

        assertThat(result, IsCloseTo.closeTo(excel, 0.0001d));
    }

    @Test
    public void testSmallConvergenceInterval_Issue5095()
    {
        double error = 1e-5;
        List<LocalDate> dates = Arrays
                        .asList("2019-05-24, 2019-06-21, 2020-01-13, 2020-05-04, 2020-06-29, 2021-01-04".split(", "))
                        .stream().map(LocalDate::parse).toList();
        List<Double> values = Arrays.asList(-9930.94, 12.8, -10240.0, -9799.0, 396.56, 23.2);

        assertThat(IRR.calculate(dates, values), IsCloseTo.closeTo(-0.9998592, error));
    }

    @Test
    public void testSmallConvergenceInterval_Day()
    {
        double error = 1e-5;
        List<LocalDate> dates = Arrays.asList("2000-01-01, 2000-01-02".split(", ")).stream().map(LocalDate::parse)
                        .toList();

        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0)), IsCloseTo.closeTo(0, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 0.99)), IsCloseTo.closeTo(-0.974482035, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 0.97)), IsCloseTo.closeTo(-0.999985151, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 0.96)), IsCloseTo.closeTo(-0.999999661, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 0.95)), IsCloseTo.closeTo(-0.999999992, error));
    }

    @Test
    public void testYear()
    {
        double error = 1e-5;
        List<LocalDate> dates = Arrays.asList("2001-01-01, 2002-01-01".split(", ")).stream().map(LocalDate::parse)
                        .toList();

        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e9)), IsCloseTo.closeTo(999999999.0, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e6)), IsCloseTo.closeTo(999999.0, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e3)), IsCloseTo.closeTo(999.0, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e2)), IsCloseTo.closeTo(99.0, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e1)), IsCloseTo.closeTo(9.0, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e0)), IsCloseTo.closeTo(0.0, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e-1)), IsCloseTo.closeTo(-.9, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e-2)), IsCloseTo.closeTo(-.99, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e-3)), IsCloseTo.closeTo(-.999, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e-4)), IsCloseTo.closeTo(-.9999, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e-5)), IsCloseTo.closeTo(-.99999, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e-6)), IsCloseTo.closeTo(-.999999, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 1.0e-9)), IsCloseTo.closeTo(-.9999999, error));
        assertThat(IRR.calculate(dates, Arrays.asList(-1.0, 0.0)), IsCloseTo.closeTo(-1.0, error));
    }

}
