package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.math.NPVFunction;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

public class NPVFunctionTest
{
    @Test(expected = NullPointerException.class)
    public void testNullArguments()
    {
        new NPVFunction(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInvalidArguments()
    {
        new NPVFunction(Arrays.asList(new Date()), new ArrayList<Double>());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEmptyArguments()
    {
        new NPVFunction(new ArrayList<Date>(), new ArrayList<Double>());
    }

    @Test
    public void testSimple()
    {
        Calendar cal = Calendar.getInstance();
        cal.set(2010, 0, 1);
        cal.getTime();

        NPVFunction f = new NPVFunction(Arrays.asList( //
                        Dates.date(2010, Calendar.JANUARY, 1), //
                        Dates.date(2010, Calendar.DECEMBER, 31)), //
                        Arrays.asList(-200d, 210d));

        assertThat(f.compute(0.05d), is(3965.669635841685d));
    }

}
