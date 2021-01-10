package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.math.NewtonGoalSeek.Function;

public final class IRR
{
    private IRR()
    {
    }

    @SuppressWarnings("nls")
    private static double halving(Function f, double left, double right, double fLeft, double fRight)
    {
        if (fLeft * fRight >= 0)
            throw new UnsupportedOperationException("Endpoints of interval must have different sign in f");

        double center = (left + right) / 2;

        if (right - left < 0.001d)
            return center;

        double fCenter = f.compute(center);
        if (fCenter == 0)
            return center;
        else if (fCenter * fLeft < 0)
            return halving(f, left, center, fLeft, fCenter);
        else
            return halving(f, center, right, fCenter, fRight);
    }

    public static double calculate(List<LocalDate> dates, List<Double> values)
    {
        Function npv = new NPVFunction(dates, values);
        Function derivative = new PseudoDerivativeFunction(npv);

        // find a crude initial guess in interval (0,1)
        // npv(0) is undefined, but diverges with sign given by last term
        double fLeft = Double.POSITIVE_INFINITY * values.get(values.size() - 1);
        // npv(1) is the sum of undiscounted flows
        double fRight = values.stream().collect(Collectors.summingDouble(f -> f));
        // if they have the same sign, we hopefully don't have a very extreme case, so just guess 5%
        double guess = fLeft * fRight < 0 ? halving(npv, 0, 1, fLeft, fRight) : 1.05;

        return NewtonGoalSeek.seek(npv, derivative, guess) - 1;
    }
}
