package name.abuchen.portfolio.math;

import name.abuchen.portfolio.math.NewtonGoalSeek.Function;

/* package */class PseudoDerivativeFunction implements Function
{
    private final Function function;

    public PseudoDerivativeFunction(Function function)
    {
        this.function = function;
    }

    @Override
    public double compute(double x)
    {
        double delta = Math.abs(x) / 1e6;

        double left = function.compute(x - delta);
        double right = function.compute(x + delta);

        return (right - left) / (2 * delta);
    }

}
