package name.abuchen.portfolio.snapshot;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Predicate;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

public abstract class ReportingPeriod
{
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

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

    protected LocalDate startDate;
    protected LocalDate endDate;

    public final LocalDate getStartDate()
    {
        return startDate;
    }

    public final LocalDate getEndDate()
    {
        return endDate;
    }

    public final Predicate<Transaction> containsTransaction()
    {
        return t -> t.getDate().isAfter(startDate) && !t.getDate().isAfter(endDate);
    }

    public final Interval toInterval()
    {
        return Interval.of(startDate, endDate);
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

            endDate = LocalDate.now();
            startDate = endDate.minusYears(years).minusMonths(months);
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
            int u = code.indexOf('_');
            this.startDate = LocalDate.parse(code.substring(1, u));
            this.endDate = LocalDate.parse(code.substring(u + 1));
        }

        public FromXtoY(LocalDate startDate, LocalDate endDate)
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
            return MessageFormat.format(Messages.LabelReportingPeriodFromXtoY, getStartDate().format(DATE_FORMATTER),
                            getEndDate().format(DATE_FORMATTER));
        }
    }

    public static class SinceX extends ReportingPeriod
    {
        private static final char CODE = 'S';

        /* package */SinceX(String code)
        {
            this.startDate = LocalDate.parse(code.substring(1));
            this.endDate = LocalDate.now();
        }

        public SinceX(LocalDate startDate)
        {
            this.startDate = startDate;
            this.endDate = LocalDate.now();
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(Values.Date.format(getStartDate()));
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodSince, getStartDate().format(DATE_FORMATTER));
        }

    }

}
