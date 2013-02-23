package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.ConsumerPriceIndex;

import org.joda.time.DateMidnight;
import org.joda.time.Interval;

public class CPIIndex
{
    public static CPIIndex forClient(ClientIndex clientIndex, List<Exception> warnings)
    {
        CPIIndex index = new CPIIndex();
        index.calculate(clientIndex, warnings);
        return index;
    }

    private Date[] dates;
    private double[] accumulated;

    public Date[] getDates()
    {
        return dates;
    }

    public double[] getAccumulatedPercentage()
    {
        return accumulated;
    }

    private void calculate(ClientIndex clientIndex, List<Exception> warnings)
    {
        Interval interval = new Interval(clientIndex.getFirstDataPoint(), clientIndex.getReportInterval().toInterval()
                        .getEnd());

        List<ConsumerPriceIndex> cpiSeries = clientIndex.getClient().getConsumerPriceIndeces();
        Collections.sort(cpiSeries);

        List<Date> dates = new ArrayList<Date>();
        List<Double> accumulated = new ArrayList<Double>();

        int baseline = -1;

        for (ConsumerPriceIndex cpi : cpiSeries)
        {
            DateMidnight date = new DateMidnight(cpi.getYear(), cpi.getMonth() + 1, 1).dayOfMonth().withMaximumValue();

            if (interval.contains(date))
            {
                if (baseline == -1)
                    baseline = cpi.getIndex();

                dates.add(date.toDate());
                accumulated.add(((double) cpi.getIndex() / (double) baseline) - 1);
            }
        }

        this.dates = dates.toArray(new Date[0]);
        this.accumulated = new double[accumulated.size()];
        for (int ii = 0; ii < this.accumulated.length; ii++)
            this.accumulated[ii] = accumulated.get(ii);
    }

}
