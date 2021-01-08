package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.Test;

import name.abuchen.portfolio.math.NewtonGoalSeek.Function;

public class PseudoDerivateFunctionTest
{
    @Test
    public void testSimple()
    {

        Function f = new Function()
        {
            @Override
            public double compute(double x)
            {
                return x;
            }
        };

        PseudoDerivativeFunction p = new PseudoDerivativeFunction(f);

        double result = p.compute(1d);

        assertThat(new BigDecimal(result).setScale(5, RoundingMode.HALF_UP).doubleValue(), is(1d));
    }
}
