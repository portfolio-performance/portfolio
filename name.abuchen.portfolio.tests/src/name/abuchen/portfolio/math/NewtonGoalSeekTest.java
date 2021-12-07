package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class NewtonGoalSeekTest
{

    @Test
    public void testSimple()
    {
        NewtonGoalSeek.Function function = new NewtonGoalSeek.Function()
        {
            @Override
            public double compute(double x)
            {
                return Math.pow(x, 3) - 1;
            }
        };

        NewtonGoalSeek.Function derivative = new NewtonGoalSeek.Function()
        {
            @Override
            public double compute(double x)
            {
                return 3 * x;
            }
        };

        assertThat(NewtonGoalSeek.seek(function, derivative, 0.5d), is(1d));
    }

    @Test
    public void testComplex()
    {
        NewtonGoalSeek.Function function = new NewtonGoalSeek.Function()
        {
            @Override
            public double compute(double x)
            {
                return Math.sin(x);
            }
        };

        NewtonGoalSeek.Function derivative = new NewtonGoalSeek.Function()
        {
            @Override
            public double compute(double x)
            {
                return Math.cos(x);
            }
        };

        assertThat(NewtonGoalSeek.seek(function, derivative, 2d), is(3.141592653589793d));
    }

}
