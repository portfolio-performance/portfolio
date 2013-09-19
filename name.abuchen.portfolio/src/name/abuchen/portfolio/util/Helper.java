package name.abuchen.portfolio.util;

import java.util.Date;

import name.abuchen.portfolio.model.Values;

import org.joda.time.DateTime;
import org.joda.time.Days;

public class Helper
{
    public static void Assert(boolean check)
    {
        if (!check)
        {
            Assert(true); // for setting a breakpoint here
        }
    }

    public static Date dateAddDays(Date date, int offset)
    {
        return new DateTime(date).plusDays(offset).toDate();
    }

    public static int daysBetween(Date dateFrom, Date dateTo)
    {
        return Days.daysBetween(new DateTime(dateFrom), new DateTime(dateTo)).getDays();
    }

    public static double sqr(double Value)
    {
        return Value * Value;
    }
    
    public static String getNonZeroValueFormat (Values<Long> val, Long value)
    {
        return (Math.abs(value) > 0 ? val.format (value) : null);
    }

    public static String getNonZeroValueFormat (Values<Integer> val, Integer value)
    {
        return (Math.abs(value) > 0 ? val.format (value) : null);
    }

    public static String getNonZeroValueFormat (Values<Double> val, double value, double eps)
    {
        return (Math.abs(value) >= eps ? val.format (value) : null);
    }

    public static class Statistics
    {
        private int n;
        private double sum1, sum2, min, max;

        public void clear()
        {
            n = 0;
            sum1 = 0;
            sum2 = 0;
            min = 0;
            max = 0;
        }

        public void add(double Value)
        {
            if (n == 0)
            {
                min = Value;
                max = Value;
            }
            else
            {
                if (Value < min)
                    min = Value;
                if (Value > max)
                    max = Value;
            }
            sum1 += Value;
            sum2 += sqr(Value);
            n++;
        }

        public double mean()
        {
            return n == 0 ? 0 : sum1 / n;
        }

        public double sDev1()
        {
            return Math.sqrt(sDev2());
        }

        public double sDev2()
        {
            return n == 0 ? 0 : sum2 / n - sqr(mean());
        }
    }

    public static class LinearRegression
    {
        private double sumX, sumY, sumXY, sumXX, sumYY;
        private int n;
        private double slopeX, slopeY, axisX, axisY, korrel;
        private boolean valid;

        public void clear()
        {
            sumX = 0;
            sumY = 0;
            sumXY = 0;
            sumXX = 0;
            sumYY = 0;
            n = 0;
        }

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
            return korrel*korrel;
        }

        public double getValueX(double Y)
        {
            validate ();
            return axisY + slopeY * Y;
        }

        public double getValueY(double X)
        {
            validate ();
            return axisX + slopeX * X;
        }
    }

    public static class LogarithmicRegression extends LinearRegression
    {
        @Override
        public void add(double X, double Y)
        {
            super.add(X, Math.log(Y));
        }

        @Override
        public double getValueX(double Y)
        {
            return 0; // Math.exp(super.getValueX());
            // das stimmt nicht, wird z.Zt. aber nicht benötigt
        }

        @Override
        public double getValueY(double X)
        {
            return Math.exp(super.getValueY(X));
        }
    }
}
