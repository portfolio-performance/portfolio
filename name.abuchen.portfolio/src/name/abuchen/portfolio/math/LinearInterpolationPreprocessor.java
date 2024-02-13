package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

/**
 * This class does a pre-processing of time-based data series that might have
 * gaps at different points and different start and end dates.
 * 
 * It converts these
 * data series into data series with the exact same dates as follows:
 * - The new data series start on the date where the latest data series of
 * the input data series start.
 * - The new data series end when the earliest of the input data series end.
 * - In between, these data series have a data point whenever at
 * least one of the input data series have a data point. For data series that do
 * not have a data point for this date, a new data point is calculated with
 * linear interpolation between the latest data point before and the earliest
 * data point after this date.
 */
public class LinearInterpolationPreprocessor
{
    public static class DataSeriesToPreprocess
    {
        private LocalDate[] localDates;
        private double[] data;

        /**
         * @param localDates needs to be sorted
         */
        public DataSeriesToPreprocess(LocalDate[] localDates, double[] data)
        {
            if (localDates.length != data.length)
                throw new IllegalArgumentException();
            this.localDates = localDates;
            this.data = data;
        }
    }

    private LocalDate[] normalizedLocalDates;
    private double[][] preprocessedData;

    public LinearInterpolationPreprocessor(DataSeriesToPreprocess... dataSeries)
    {
        if(dataSeries.length == 0)
            throw new IllegalArgumentException();
        
        ArrayList<LocalDate> normalizedLocalDatesList = new ArrayList<>();
        
        if(dataSeries[0].localDates.length == 0)
        {
            empty(dataSeries.length);
            return;
        }
        
        LocalDate startDate = dataSeries[0].localDates[0];
        for(int i = 1; i < dataSeries.length; i++)
        {
            if(dataSeries[i].localDates.length == 0)
            {
                empty(dataSeries.length);
                return;
            }
            if(startDate.isBefore(dataSeries[i].localDates[0]))
            {
                startDate = dataSeries[i].localDates[0];
            }
        }
        
        LocalDate endDate = dataSeries[0].localDates[dataSeries[0].localDates.length - 1];
        for(int i = 1; i < dataSeries.length; i++)
        {
            if(endDate.isAfter(dataSeries[i].localDates[dataSeries[i].localDates.length - 1]))
            {
                endDate = dataSeries[i].localDates[dataSeries[i].localDates.length - 1];
            }
        }
        
        if(endDate.isBefore(startDate))
        {
            empty(dataSeries.length);
            return;
        }
        normalizedLocalDatesList.add(startDate);
        
        int[] indices = new int[dataSeries.length];
        
        // calculate dates with a data sample
        loop: while(true)
        {
            LocalDate nextDate = null;
            for(int i = 0; i < dataSeries.length; i++)
            {
                while(!dataSeries[i].localDates[indices[i]].isAfter(
                                    normalizedLocalDatesList.get(normalizedLocalDatesList.size() - 1)))
                {
                    indices[i]++;
                    if(dataSeries[i].localDates.length <= indices[i])
                        break loop; // dataSeries[i] has no more data
                }
                
                
                if(nextDate == null || nextDate.isAfter(dataSeries[i].localDates[indices[i]]))
                {
                    nextDate = dataSeries[i].localDates[indices[i]];
                }
            }
            normalizedLocalDatesList.add(nextDate);
        }
        
        normalizedLocalDates = normalizedLocalDatesList.toArray(LocalDate[]::new);
        preprocessedData = new double[dataSeries.length][normalizedLocalDates.length];
        
        indices = new int[dataSeries.length];
        
        // interpolate data
        for(int dateIndex = 0; dateIndex < normalizedLocalDates.length; dateIndex++)
        {
            for(int i = 0; i < indices.length; i++)
            {
                while(normalizedLocalDates[dateIndex].isAfter(dataSeries[i].localDates[indices[i]]))
                    indices[i]++;
                // at this point indices[i] points to the current date or the next later date, if there is no sample for the current date.
            
                if(dataSeries[i].localDates[indices[i]].equals(normalizedLocalDates[dateIndex])) // no interpolation necessary
                    preprocessedData[i][dateIndex] = dataSeries[i].data[indices[i]];
                else // interpolation necessary
                {
                    long gapDays = ChronoUnit.DAYS.between(dataSeries[i].localDates[indices[i] - 1],
                                    dataSeries[i].localDates[indices[i]]);
                    long positionInGap = ChronoUnit.DAYS.between(dataSeries[i].localDates[indices[i] - 1],
                                    normalizedLocalDates[dateIndex]);
                    double interpolatedValue = dataSeries[i].data[indices[i] - 1] + ((double) positionInGap) / ((double) gapDays)
                                    * (dataSeries[i].data[indices[i]] - dataSeries[i].data[indices[i] - 1]);
                    preprocessedData[i][dateIndex] = interpolatedValue;
                }
            }
        }
    }

    private void empty(int dataSeries)
    {
        normalizedLocalDates = new LocalDate[0];
        preprocessedData = new double[dataSeries][0];
    }

    public LocalDate[] getNormalizedLocalDates()
    {
        return normalizedLocalDates;
    }

    public double[] getPreprocessedData(int i)
    {
        return preprocessedData[i];
    }
}
