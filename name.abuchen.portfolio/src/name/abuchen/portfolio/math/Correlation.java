package name.abuchen.portfolio.math;

import java.time.LocalDate;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.math.LinearInterpolationPreprocessor.DataSeriesToPreprocess;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

public class Correlation
{
    public static class CorrelationNotDefinedException extends Exception
    {
        public CorrelationNotDefinedException(String message)
        {
            super(message);
        }

        private static final long serialVersionUID = 1565087734491785161L;
    }
    
    private static final double EPS = 1e-8;
    
    private PerformanceIndex performanceIndex1;
    private PerformanceIndex performanceIndex2;
    
    public Correlation(PerformanceIndex performanceIndex1, PerformanceIndex performanceIndex2)
    {
        this.performanceIndex1 = performanceIndex1;
        this.performanceIndex2 = performanceIndex2;
    }
    
    public double calculateCorrelationCoefficient() throws CorrelationNotDefinedException
    {
        LocalDate[] dates1 = performanceIndex1.getDates();
        LocalDate[] dates2 = performanceIndex2.getDates();
        double[] accumulatedPerformances1 = performanceIndex1.getAccumulatedPercentage();
        double[] accumulatedPerformances2 = performanceIndex2.getAccumulatedPercentage();
        double[] normalizedAccumulatedPerformances1 = new double[accumulatedPerformances1.length];
        double[] normalizedAccumulatedPerformances2 = new double[accumulatedPerformances2.length];
        for(int i = 0; i < normalizedAccumulatedPerformances1.length; i++)
            normalizedAccumulatedPerformances1[i] = accumulatedPerformances1[i] + 1d;
        for(int i = 0; i < normalizedAccumulatedPerformances2.length; i++)
            normalizedAccumulatedPerformances2[i] += accumulatedPerformances2[i] + 1d;
        DataSeriesToPreprocess dataSeries1 = new DataSeriesToPreprocess(dates1, normalizedAccumulatedPerformances1);
        DataSeriesToPreprocess dataSeries2 = new DataSeriesToPreprocess(dates2, normalizedAccumulatedPerformances2);
        
        LinearInterpolationPreprocessor preprocessor = new LinearInterpolationPreprocessor(dataSeries1, dataSeries2);
        double[] preprocessedAccumulatedPercentage1 = preprocessor.getPreprocessedData(0);
        double[] preprocessedAccumulatedPercentage2 = preprocessor.getPreprocessedData(1);
        
        // For the correlation-coefficient to be well defined, we need at least 2 dailyPerformance samples,
        // i.e. at least 3 accumulated percentage samples.
        if(preprocessedAccumulatedPercentage1.length < 3)
            throw new CorrelationNotDefinedException(Messages.MsgErrorCorrelationNotEnoughData);
        
        double[] dailyPerformances1 = new double[preprocessedAccumulatedPercentage1.length - 1];
        double[] dailyPerformances2 = new double[preprocessedAccumulatedPercentage1.length - 1];
        double summedDailyPerformances1 = 0;
        double summedDailyPerformances2 = 0;
        
        for(int i = 0; i < preprocessedAccumulatedPercentage1.length - 1; i++)
        {
            dailyPerformances1[i] = preprocessedAccumulatedPercentage1[i + 1] / preprocessedAccumulatedPercentage1[i];
            summedDailyPerformances1 += dailyPerformances1[i];
            dailyPerformances2[i] = preprocessedAccumulatedPercentage2[i + 1] / preprocessedAccumulatedPercentage2[i];
            summedDailyPerformances2 += dailyPerformances2[i];
        }

        double arithmeticMean1 = summedDailyPerformances1 / dailyPerformances1.length;
        double arithmeticMean2 = summedDailyPerformances2 / dailyPerformances1.length;
        double summedVariance1 = 0;
        double summedVariance2 = 0;
        double summedCovariance = 0;
        
        for(int i = 0; i < dailyPerformances1.length; i++)
        {
            double diff1 = dailyPerformances1[i] - arithmeticMean1;
            double diff2 = dailyPerformances2[i] - arithmeticMean2;
            summedVariance1 += diff1 * diff1;
            summedVariance2 += diff2 * diff2;
            summedCovariance += diff1 * diff2;
        }
        
        if(Math.abs(summedVariance1) < EPS || Math.abs(summedVariance2) < EPS)
            throw new CorrelationNotDefinedException(Messages.MsgErrorCorrelationNoVariance);
        
        double correlationCoefficient = summedCovariance / Math.sqrt(summedVariance1 * summedVariance2);
        
        return correlationCoefficient;
    }
}
