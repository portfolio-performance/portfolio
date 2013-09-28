package name.abuchen.portfolio.math;

public class LogarithmicRegression extends LinearRegression
{
    @Override
    public void add(double X, double Y)
    {
        super.add(X, Math.log(Y));
    }

    @Override
    public double getValueX(double Y)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getValueY(double X)
    {
        return Math.exp(super.getValueY(X));
    }
}
