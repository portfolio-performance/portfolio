package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Aggregation
{
    public enum Period
    {
        WEEKLY
        {
            @Override
            public TemporalAmount getPeriod()
            {
                return java.time.Period.ofDays(7);
            }

            @Override
            public LocalDate getStartDateFor(LocalDate d)
            {
                TemporalField fieldISO = WeekFields.of(Locale.getDefault()).dayOfWeek();
                return d.with(fieldISO, 1);
            }
        },
        MONTHLY
        {
            @Override
            public TemporalAmount getPeriod()
            {
                return java.time.Period.ofMonths(1);
            }

            @Override
            public LocalDate getStartDateFor(LocalDate d)
            {
                return d.withDayOfMonth(1);
            }
        },
        QUARTERLY
        {
            @Override
            public TemporalAmount getPeriod()
            {
                return java.time.Period.ofMonths(3);
            }

            @Override
            public LocalDate getStartDateFor(LocalDate d)
            {
                int month = (((d.getMonthValue() - 1) / 3) * 3) + 1;
                return LocalDate.of(d.getYear(), month, 1);
            }
        },
        YEARLY
        {
            @Override
            public TemporalAmount getPeriod()
            {
                return java.time.Period.ofYears(1);
            }

            @Override
            public LocalDate getStartDateFor(LocalDate d)
            {
                return d.withDayOfYear(1);
            }
        };

        public abstract TemporalAmount getPeriod();

        public abstract LocalDate getStartDateFor(LocalDate d);

        private static final ResourceBundle RESOURCES = ResourceBundle
                        .getBundle("name.abuchen.portfolio.snapshot.labels"); //$NON-NLS-1$

        @Override
        public String toString()
        {
            return RESOURCES.getString("aggregation." + name()); //$NON-NLS-1$
        }
    }

    public static PerformanceIndex aggregate(PerformanceIndex index, Period period)
    {
        LocalDate[] dates = index.getDates();
        double[] accumulated = index.getAccumulatedPercentage();
        double[] delta = index.getDeltaPercentage();
        long[] inboundTransferals = index.getInboundTransferals();
        long[] outboundTransferals = index.getOutboundTransferals();
        long[] totals = index.getTotals();

        LocalDate start = period.getStartDateFor(dates[0]).plus(period.getPeriod());
        LocalDate kill = start.minusDays(1);

        List<LocalDate> cDates = new ArrayList<>();
        List<Double> cAccumulated = new ArrayList<>();
        List<Double> cDelta = new ArrayList<>();
        List<Long> cInboundTransferals = new ArrayList<>();
        List<Long> cOutboundTransferals = new ArrayList<>();
        List<Long> cTotals = new ArrayList<>();

        double d = 0d;
        long in_t = 0;
        long out_t = 0;

        for (int ii = 0; ii < dates.length; ii++)
        {
            LocalDate current = dates[ii];
            d = ((d + 1) * (delta[ii] + 1)) - 1;
            in_t += inboundTransferals[ii];
            out_t += outboundTransferals[ii];

            if (current.equals(kill) || ii == dates.length - 1)
            {
                cDates.add(current);
                cAccumulated.add(accumulated[ii]);
                cDelta.add(d);
                cInboundTransferals.add(in_t);
                cOutboundTransferals.add(out_t);
                cTotals.add(totals[ii]);

                d = 0d;
                in_t = 0;
                out_t = 0;

                start = start.plus(period.getPeriod());
                kill = start.minusDays(1);
            }
        }

        PerformanceIndex answer = new PerformanceIndex(index.getClient(), index.getCurrencyConverter(),
                        index.getReportInterval());
        answer.dates = cDates.toArray(new LocalDate[0]);
        answer.accumulated = asArrayD(cAccumulated);
        answer.delta = asArrayD(cDelta);
        answer.inboundTransferals = asArrayL(cInboundTransferals);
        answer.outboundTransferals = asArrayL(cOutboundTransferals);
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
