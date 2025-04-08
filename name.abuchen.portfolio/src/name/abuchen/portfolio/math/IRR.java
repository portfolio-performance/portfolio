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
        if (Math.signum(fLeft) == Math.signum(fRight))
            throw new UnsupportedOperationException("Endpoints of interval must have different sign in f");

        double center = (left + right) / 2;

        if (right - left < 0.001d)
            return center;

        double fCenter = f.compute(center);
        if (fCenter == 0)
            return center;
        else if (Math.signum(fCenter) == Math.signum(fRight))
            return halving(f, left, center, fLeft, fCenter);
        else
            return halving(f, center, right, fCenter, fRight);
    }

    public static double calculate(List<LocalDate> dates, List<Double> values)
    {
        Function npv = new NPVFunction(dates, values);
        Function derivative = new PseudoDerivativeFunction(npv);

        // find a crude initial guess in interval (0,1)

        // npv(0) is undefined, but the limit diverges with sign given by most discounted cashflow
        // we only care about the sign, so we can use the last term
        double fLeft = values.get(values.size() - 1);
        // npv(1) is the sum of undiscounted flows
        double fRight = values.stream().collect(Collectors.summingDouble(f -> f));

        double guess;
        // if they have the same sign, let's hope the zero is reasonable, so just guess 5%
        if (Math.signum(fLeft) == Math.signum(fRight))
            guess = 1.05;
        else
            guess = halving(npv, 0, 1, fLeft, fRight);

        return NewtonGoalSeek.seek(npv, derivative, guess) - 1;
    }
}
