package name.abuchen.portfolio.math;

public class LinearRegression
{
    private double sumX, sumY, sumXY, sumXX, sumYY;
    private int n;
    private double slopeX, slopeY, axisX, axisY, korrel;
    private boolean valid;

    private void invalidate()
    {
        if (valid)
        {
            slopeX = 0;
            slopeX = 0;
            axisX = 0;
            axisY = 0;
            korrel = 0;
            valid = false;
        }
    }

    private void validate()
    {
        if (valid)
            return;

        if (n > 0)
        {
            double val = n * sumXY - sumX * sumY;
            double detX = n * sumXX - sumX * sumX;
            double detY = n * sumYY - sumY * sumY;
            slopeX = (detX == 0 ? 0 : val / detX);
            slopeY = (detY == 0 ? 0 : val / detY);
            axisX = (sumY - slopeX * sumX) / n;
            axisY = (sumX - slopeY * sumY) / n;
            korrel = slopeX * slopeY;
        }
        valid = true;
    }

    public void add(double X, double Y)
    {
        invalidate();
        sumX += X;
        sumY += Y;
        sumXY += X * Y;
        sumXX += X * X;
        sumYY += Y * Y;
        n++;
    }

    public double getSlopeX()
    {
        validate();
        return slopeX;
    }

    public double getSlopeY()
    {
        validate();
        return slopeY;
    }

    public double getAxisX()
    {
        validate();
        return axisX;
    }

    public double getAxisY()
    {
        validate();
        return axisY;
    }

    public double getKorrel()
    {
        validate();
        return korrel;
    }

    public double getDeterminant()
    {
        validate();
        return korrel * korrel;
    }

    public double getValueX(double Y)
    {
        validate();
        return axisY + slopeY * Y;
    }

    public double getValueY(double X)
    {
        validate();
        return axisX + slopeX * X;
    }
}
