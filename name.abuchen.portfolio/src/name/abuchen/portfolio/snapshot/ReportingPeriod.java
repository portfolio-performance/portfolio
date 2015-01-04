package name.abuchen.portfolio.snapshot;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Predicate;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;

import org.joda.time.Interval;

public abstract class ReportingPeriod
{
    public static final ReportingPeriod from(String code) throws IOException
    {
        char type = code.charAt(0);

        if (type == LastX.CODE)
            return new LastX(code);
        else if (type == FromXtoY.CODE)
            return new FromXtoY(code);
        else if (type == SinceX.CODE)
            return new SinceX(code);

        // backward compatible
        if (code.charAt(code.length() - 1) == 'Y')
            return new LastX(Integer.parseInt(code.substring(0, code.length() - 1)), 0);

        throw new IOException(code);
    }

    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$

    protected Date startDate;
    protected Date endDate;

    public final Date getStartDate()
    {
        return startDate;
    }

    public final Date getEndDate()
    {
        return endDate;
    }

    public final Predicate<Transaction> containsTransaction()
    {
        return t -> t.getDate().getTime() > startDate.getTime() && t.getDate().getTime() <= endDate.getTime();
    }

    public final Interval toInterval()
    {
        return new Interval(startDate.getTime(), endDate.getTime());
    }

    public abstract void writeTo(StringBuilder buffer);

    public static class LastX extends ReportingPeriod
    {
        private static final char CODE = 'L';

        private final int years;
        private final int months;

        /* package */LastX(String code)
        {
            this(Integer.parseInt(code.substring(1, code.indexOf('Y'))), //
                            Integer.parseInt(code.substring(code.indexOf('Y') + 1)));
        }

        public LastX(int years, int months)
        {
            this.years = years;
            this.months = months;

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -years);
            cal.add(Calendar.MONTH, -months);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            startDate = cal.getTime();
            endDate = Dates.today();
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(years).append('Y').append(months);
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();

            if (years != 0)
            {
                buf.append(MessageFormat.format(Messages.LabelReportingPeriodYears, years));
                if (months != 0)
                    buf.append(", "); //$NON-NLS-1$
            }

            if (months != 0)
                buf.append(MessageFormat.format(Messages.LabelReportingPeriodMonths, months));

            return buf.toString();
        }
    }

    public static class FromXtoY extends ReportingPeriod
    {
        private static final char CODE = 'F';

        /* package */FromXtoY(String code)
        {
            try
            {
                int u = code.indexOf('_');
                this.startDate = DATE_FORMAT.parse(code.substring(1, u));
                this.endDate = DATE_FORMAT.parse(code.substring(u + 1));
            }
            catch (ParseException e)
            {
                throw new RuntimeException(e);
            }
        }

        public FromXtoY(Date startDate, Date endDate)
        {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(Values.Date.format(getStartDate())).append('_')
                            .append(Values.Date.format(getEndDate()));
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodFromXtoY, getStartDate(), getEndDate());
        }
    }

    public static class SinceX extends ReportingPeriod
    {
        private static final char CODE = 'S';

        /* package */SinceX(String code)
        {
            try
            {
                this.startDate = DATE_FORMAT.parse(code.substring(1));
                this.endDate = Dates.today();
            }
            catch (ParseException e)
            {
                throw new RuntimeException(e);
            }
        }

        public SinceX(Date startDate)
        {
            this.startDate = startDate;
            this.endDate = Dates.today();
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(Values.Date.format(getStartDate()));
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodSince, getStartDate());
        }

    }

}
