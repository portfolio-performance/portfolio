package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;

import name.abuchen.portfolio.util.Dates;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

public class IRRTest
{

    @Test
    public void testSimpleExcelTestCase()
    {
        Calendar cal = Calendar.getInstance();
        cal.set(2010, 0, 1);
        cal.getTime();

        double result = IRR.calculate(Arrays.asList( //
                        Dates.date(2010, Calendar.JANUARY, 1), //
                        Dates.date(2010, Calendar.DECEMBER, 31)), //
                        Arrays.asList(-200d, 210d));

        result = new BigDecimal(result).setScale(8, BigDecimal.ROUND_HALF_UP).doubleValue();
        double excel = new BigDecimal(0.050140747d).setScale(8, BigDecimal.ROUND_HALF_UP).doubleValue();

        assertThat(result, is(excel));
    }

    @Test
    public void testComplexExcelTestCaseWithSlowCurve()
    {
        Calendar cal = Calendar.getInstance();
        cal.set(2010, 0, 1);
        cal.getTime();

        double result = IRR.calculate(Arrays.asList( //
                        Dates.date(2002, Calendar.NOVEMBER, 30), //
                        Dates.date(2007, Calendar.JUNE, 11), //
                        Dates.date(2008, Calendar.MAY, 11), //
                        Dates.date(2009, Calendar.MAY, 1), //
                        Dates.date(2010, Calendar.JUNE, 1), //
                        Dates.date(2011, Calendar.MAY, 2), //
                        Dates.date(2012, Calendar.APRIL, 30), //
                        Dates.date(2012, Calendar.DECEMBER, 6)), //
                        Arrays.asList(-4398d, 200d, 270d, 280d, 280d, 300d, 330d, 14508d));

        double excel = 0.1444629967d;

        assertThat(result, IsCloseTo.closeTo(excel, 0.0001d));
    }

}
