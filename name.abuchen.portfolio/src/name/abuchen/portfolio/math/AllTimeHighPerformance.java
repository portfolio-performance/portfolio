package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;

import name.abuchen.portfolio.snapshot.PerformanceIndex;

public class AllTimeHighPerformance
{
    private PerformanceIndex performanceIndex;
    private OptionalDouble athDistanceInPercent;
    private Optional<LocalDate> athDate;

    public AllTimeHighPerformance(PerformanceIndex index)
    {
        this.performanceIndex = index;
        athDistanceInPercent = OptionalDouble.empty();
        athDate = Optional.empty();

        this.calculate();
    }

    private void calculate()
    {
        if (performanceIndex == null)
            return;

        double[] accumulatedPercentage = performanceIndex.getAccumulatedPercentage();
        LocalDate[] dates = performanceIndex.getDates();
        OptionalDouble max = OptionalDouble.empty();
        
        for(int i = 0; i < accumulatedPercentage.length; i++)
        {
            accumulatedPercentage[i] += 1d;
            if(max.isEmpty() || accumulatedPercentage[i] >= max.getAsDouble())
            {
                max = OptionalDouble.of(accumulatedPercentage[i]);
                athDate = Optional.of(dates[i]);
            }
        }

        if (max.isEmpty())
            return;

        double latest = accumulatedPercentage[accumulatedPercentage.length - 1];

        this.athDistanceInPercent = OptionalDouble.of((latest - max.getAsDouble()) / max.getAsDouble());
    }

    public OptionalDouble getDistance()
    {
        return this.athDistanceInPercent;
    }
    
    public Optional<LocalDate> getDate()
    {
        return athDate;
    }
}
