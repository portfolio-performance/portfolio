package name.abuchen.portfolio.math;

import java.util.stream.DoubleStream;

public final class Risk
{

    public static double calculateMaxDrawdown(double[] values)
    {
        double peak = Double.MIN_VALUE;
        double maxDD = 0;

        for (double value : values)
        {
            peak = Math.max(peak, value);
            // as the values are the accumulated percentages there is no need to
            // calculate a relation. We can simply reduce the peak
            maxDD = Math.max(maxDD, (peak - value));
        }
        return maxDD;
    }

    private static double[] getReturns(long[] values)
    {
        double[] returns = new double[values.length - 1];
        for (int i = 0; i < returns.length; i++)
        {
            returns[i] = (double) values[i + 1] / values[i];
        }
        return returns;
    }

    public static double calculateAverageVolatility(long[] values)
    {
        double[] vola = Risk.getReturns(values);
        double average = DoubleStream.of(vola).average().getAsDouble();
        double variance = 0d;
        for (int i = 0; i < vola.length; i++)
        {
            variance = Math.pow(vola[i] - average, 2) + variance;
        }
        variance = variance / vola.length;
        variance = Math.sqrt(variance);
        return variance;
    }

    public static double calculateSemiVolatility(long[] values)
    {
        double[] returns = Risk.getReturns(values);
        double averageReturn = DoubleStream.of(returns).average().getAsDouble();
        double semiVariance = 0;
        for (int i = 0; i < returns.length; i++)
        {
            if (returns[i] < averageReturn)
            {
                semiVariance = semiVariance + Math.pow(averageReturn - returns[i], 2);
            }
        }
        semiVariance = semiVariance / returns.length;
        return Math.sqrt(semiVariance);
    }

    public static double annualize(double risk)
    {
        // as the values are always daily, the annualization does not need a
        // parameter
        return risk * Math.sqrt(250);
    }

}
