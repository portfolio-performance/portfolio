package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.List;

import name.abuchen.portfolio.math.NewtonGoalSeek.Function;

public final class IRR
{
    private IRR()
    {
    }

    public static double calculate(List<LocalDate> dates, List<Double> values)
    {
        Function npv = new NPVFunction(dates, values);
        Function derivative = new PseudoDerivativeFunction(npv);
        return NewtonGoalSeek.seek(npv, derivative, 0.05d) - 1;
    }
}
