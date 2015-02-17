package name.abuchen.portfolio.math;

import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;

import name.abuchen.portfolio.math.Risk.Volatility;

public final class Performance
{
    
    public static class SharpeRatio {
        
        double ratio;
        
        public SharpeRatio(Date[] dates, double[] returns, int skip, Predicate<Date> filter, float benchmark)
        {
            Objects.requireNonNull(returns);
            //Calculate the return by the portfolio as the sum of all return deltas
            double totalReturn = DoubleStream.of(returns).sum();
            //Count how many days are in the calculation
            int returnCount = 0;
            for (int ii = skip; ii < returns.length; ii++)
            {
                if (!filter.test(dates[ii]))
                    continue;
                returnCount++;
            }
            //Denormalize the return by the number of days it took to achieve said return
            double averageReturn = totalReturn / Math.sqrt(returnCount);
            Volatility vola = new Volatility(dates, returns, skip, filter);
            //The benchmark is an annual rate of risk-free return, so the daily value
            //is achieved by the reverse normalization
            double denormalizedBenchmark = (benchmark/Math.sqrt(250));
            //Calculate the sharp ratio
            ratio = (averageReturn - denormalizedBenchmark) / vola.getStandardDeviation();
        }
        
        public double getRatio() {
            return ratio;
        }
        
    }
    
    private Performance() {}
    
}
