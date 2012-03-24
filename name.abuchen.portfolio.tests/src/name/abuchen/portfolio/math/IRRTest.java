package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.util.Dates;

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

}
