package name.abuchen.portfolio.math;

import java.util.ArrayList;
import java.util.List;

public final class Risk
{

    public static double calculateMaxDrawdown(long[] values)
    {
        Double peak = Double.MIN_VALUE;
        List<Double> dd = new ArrayList<Double>();
        Double maxDD = 0d;
        int i = 0;
        for (double value : values)
        {
            peak = Math.max(peak, value);
            dd.add(i, (peak - values[i]) / peak);
            maxDD = Math.max(maxDD, dd.get(i));
            i++;
        }
        return maxDD;
    }

    public static double calculateAverageVolatility(List<Double> values)
    {
        Double temp;
        List<Double> vola = new ArrayList<Double>();
        Double average = 0d;
        Double variance = 0d;
        for (int i = 1; i < values.size(); i++)
        {
            temp = (values.get(i) - values.get(i - 1)) / values.get(i - 1);
            vola.add(temp);
            average = average + temp;
        }
        average = average / vola.size();
        for (int i = 0; i < vola.size(); i++)
        {
            variance = Math.pow(vola.get(i) - average, 2) + variance;
        }
        variance = variance / vola.size();
        variance = Math.sqrt(variance);
        return variance;
    }

}
