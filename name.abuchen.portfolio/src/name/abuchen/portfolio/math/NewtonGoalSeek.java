package name.abuchen.portfolio.math;

/**
 * http://en.wikipedia.org/wiki/Newton's_method
 */
/* package */class NewtonGoalSeek
{
    public interface Function
    {
        double compute(double x);
    }

    /**
     * x(i+1) = xi - ( f(xi) / f'(xi) )
     * 
     * @param f
     *            function
     * @param fd
     *            derivative function
     * @param x0
     *            start value
     */
    public static double seek(Function f, Function fd, double x0)
    {
        double stop = 0.00001d;
        double delta = 1;
        double xi = x0;

        for (int ii = 0; delta > stop && ii < 500; ii++)
        {
            double fxi = f.compute(xi);
            double fdxi = fd.compute(xi);

            double xi1 = xi - (fxi / fdxi);

            // the functions used here (NPV with rate = 1 + r) are only
            // defined for x > 0. If the step overshoots, approach 0 instead
            if (xi1 <= 0 && xi > 0)
                xi1 = xi / 10;

            delta = Math.abs(xi1 - xi);
            xi = xi1;
        }

        return xi;
    }
}
