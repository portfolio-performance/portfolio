package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.util.Interval;

/* package */class CPIIndex extends PerformanceIndex
{
    /* package */CPIIndex(Client client, CurrencyConverter converter, ReportingPeriod reportInterval)
    {
        super(client, converter, reportInterval);
    }

    /* package */void calculate(PerformanceIndex clientIndex, boolean isNormalized)
    {
        Interval actualInterval = clientIndex.getActualInterval();
        LocalDate firstDataPoint = clientIndex.getFirstDataPoint().orElse(actualInterval.getStart());
        Interval interval = Interval.of(firstDataPoint, actualInterval.getEnd());

        List<ConsumerPriceIndex> cpiSeries = new ArrayList<ConsumerPriceIndex>(clientIndex.getClient()
                        .getConsumerPriceIndices());
        Collections.sort(cpiSeries, new ConsumerPriceIndex.ByDate());

        List<LocalDate> dates = new ArrayList<LocalDate>();
        List<Double> accumulated = new ArrayList<Double>();

        int baseline = -1;

        for (ConsumerPriceIndex cpi : cpiSeries)
        {
            LocalDate date = LocalDate.of(cpi.getYear(), cpi.getMonth(), 1);
            date = date.with(ChronoField.DAY_OF_MONTH, date.lengthOfMonth());

            if (interval.contains(date))
            {
                if (baseline == -1)
                    baseline = cpi.getIndex();

                dates.add(date);
                accumulated.add(((double) cpi.getIndex() / (double) baseline) - 1);
            }
        }

        this.dates = dates.toArray(new LocalDate[0]);
        this.accumulated = new double[accumulated.size()];
        for (int ii = 0; ii < this.accumulated.length; ii++)
            this.accumulated[ii] = accumulated.get(ii);
    }

}
