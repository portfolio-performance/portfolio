package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;

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
}
