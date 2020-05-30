package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;

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
        new NPVFunction(Arrays.asList(LocalDate.now()), new ArrayList<Double>());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEmptyArguments()
    {
        new NPVFunction(new ArrayList<LocalDate>(), new ArrayList<Double>());
    }

    @Test
    public void testSimple()
    {
        NPVFunction function = new NPVFunction(Arrays.asList( //
                        LocalDate.of(2010, Month.JANUARY, 1), //
                        LocalDate.of(2010, Month.DECEMBER, 31)), //
                        Arrays.asList(-200d, 210d));
        double doubleRate = 0.05d;

        double result = function.compute(doubleRate);

        assertThat(result, is(3965.669635841685d));
    }
}
