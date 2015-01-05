package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import org.joda.time.DateMidnight;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadablePeriod;
import org.joda.time.Years;

public class Aggregation
{
    public enum Period
    {
        WEEKLY
        {
            @Override
            public ReadablePeriod getPeriod()
            {
                return Days.days(7);
            }

            @Override
            public DateMidnight getStartDateFor(DateMidnight d)
            {
                return d.dayOfWeek().withMinimumValue();
            }
        },
        MONTHLY
        {
            @Override
            public ReadablePeriod getPeriod()
            {
                return Months.months(1);
            }

            @Override
            public DateMidnight getStartDateFor(DateMidnight d)
            {
                return d.dayOfMonth().withMinimumValue();
            }
        },
        QUARTERLY
        {
            @Override
            public ReadablePeriod getPeriod()
            {
                return Months.months(3);
            }

            @Override
            public DateMidnight getStartDateFor(DateMidnight d)
            {
                MutableDateTime answer = new MutableDateTime(d);
                answer.set(DateTimeFieldType.monthOfYear(), (((d.getMonthOfYear() - 1) / 3) * 3) + 1);
                answer.set(DateTimeFieldType.dayOfMonth(), 1);
                return new DateMidnight(answer);
            }
        },
        YEARLY
        {
            @Override
            public ReadablePeriod getPeriod()
            {
                return Years.years(1);
            }

            @Override
            public DateMidnight getStartDateFor(DateMidnight d)
            {
                return d.dayOfYear().withMinimumValue();
            }
        };

        public abstract ReadablePeriod getPeriod();

        public abstract DateMidnight getStartDateFor(DateMidnight d);

        private static final ResourceBundle RESOURCES = ResourceBundle
                        .getBundle("name.abuchen.portfolio.snapshot.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("aggregation." + name()); //$NON-NLS-1$
        }
    }

    public static PerformanceIndex aggregate(PerformanceIndex index, Period period)
    {
        Date[] dates = index.getDates();
        double[] accumulated = index.getAccumulatedPercentage();
        double[] delta = index.getDeltaPercentage();
        long[] transferals = index.getTransferals();
        long[] totals = index.getTotals();

        DateMidnight start = period.getStartDateFor(new DateMidnight(dates[0])).plus(period.getPeriod());
        DateMidnight kill = start.minusDays(1);

        List<Date> cDates = new ArrayList<Date>();
        List<Double> cAccumulated = new ArrayList<Double>();
        List<Double> cDelta = new ArrayList<Double>();
        List<Long> cTransferals = new ArrayList<Long>();
        List<Long> cTotals = new ArrayList<Long>();

        double d = 0d;
        long t = 0;

        for (int ii = 0; ii < dates.length; ii++)
        {
            DateMidnight current = new DateMidnight(dates[ii]);
            d = ((d + 1) * (delta[ii] + 1)) - 1;
            t += transferals[ii];

            if (current.equals(kill) || ii == dates.length - 1)
            {
                cDates.add(current.toDate());
                cAccumulated.add(accumulated[ii]);
                cDelta.add(d);
                cTransferals.add(t);
                cTotals.add(totals[ii]);

                d = 0d;
                t = 0;

                start = start.plus(period.getPeriod());
                kill = start.minusDays(1);
            }
        }

        PerformanceIndex answer = new PerformanceIndex(index.getClient(), index.getCurrencyConverter(),
                        index.getReportInterval());
        answer.dates = cDates.toArray(new Date[0]);
        answer.accumulated = asArrayD(cAccumulated);
        answer.delta = asArrayD(cDelta);
        answer.transferals = asArrayL(cTransferals);
        answer.totals = asArrayL(cTotals);
        return answer;
    }

    private static double[] asArrayD(List<Double> cAccumulated)
    {
        double[] answer = new double[cAccumulated.size()];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = cAccumulated.get(ii);
        return answer;
    }

    private static long[] asArrayL(List<Long> cAccumulated)
    {
        long[] answer = new long[cAccumulated.size()];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = cAccumulated.get(ii);
        return answer;
    }

}
