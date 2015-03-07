package name.abuchen.portfolio.math;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;

import name.abuchen.portfolio.math.Risk.Volatility;

public class SharpeRatio
{

    double ratio;

    public SharpeRatio(double[] returns, Predicate<Integer> filter, float benchmark)
    {
        Objects.requireNonNull(returns);
        // Calculate the return by the portfolio as the sum of all return deltas
        double totalReturn = DoubleStream.of(returns).sum();
        // Count how many days are in the calculation
        int returnCount = 0;
        for (int ii = 0; ii < returns.length; ii++)
        {
            if (!filter.test(ii))
                continue;
            returnCount++;
        }
        // Denormalize the return by the number of days it took to achieve said
        // return
        double averageReturn = totalReturn / Math.sqrt(returnCount);
        Volatility vola = new Volatility(returns, filter);
        // The benchmark is an annual rate of risk-free return, so the daily
        // value
        // is achieved by the reverse normalization
        double denormalizedBenchmark = (benchmark / Math.sqrt(250));
        // Calculate the sharp ratio
        ratio = (averageReturn - denormalizedBenchmark) / vola.getStandardDeviation();
    }

    public double getRatio()
    {
        return ratio;
    }

}
