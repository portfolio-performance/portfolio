package name.abuchen.portfolio.math;

import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.math.NewtonGoalSeek.Function;

public final class IRR
{
    public static double calculate(List<Date> dates, List<Double> values)
    {
        Function npv = new NPVFunction(dates, values);
        Function derivative = new PseudoDerivativeFunction(npv);
        return NewtonGoalSeek.seek(npv, derivative, 0.05d) - 1;
    }
}
