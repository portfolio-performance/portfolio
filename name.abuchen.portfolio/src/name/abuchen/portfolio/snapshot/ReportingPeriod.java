package name.abuchen.portfolio.snapshot;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Predicate;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public abstract class ReportingPeriod
{
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private ReportingPeriod()
    {
    }

    public static final ReportingPeriod from(String code) throws IOException
    {
        char type = code.charAt(0);

        if (type == LastX.CODE)
            return new LastX(code);
        else if (type == LastXDays.CODE)
            return new LastXDays(code);
        else if (type == LastXTradingDays.CODE)
            return new LastXTradingDays(code);
        else if (type == FromXtoY.CODE)
            return new FromXtoY(code);
        else if (type == SinceX.CODE)
            return new SinceX(code);
        else if (type == YearX.CODE)
            return new YearX(code);
        else if (type == CurrentMonth.CODE)
            return new CurrentMonth();

        // backward compatible
        if (code.charAt(code.length() - 1) == 'Y')
            return new LastX(Integer.parseInt(code.substring(0, code.length() - 1)), 0);

        throw new IOException(code);
    }

    public final Predicate<Transaction> containsTransaction(LocalDate relativeTo)
    {
        Interval interval = toInterval(relativeTo);
        return t -> t.getDateTime().toLocalDate().isAfter(interval.getStart())
                        && !t.getDateTime().toLocalDate().isAfter(interval.getEnd());
    }

    public abstract Interval toInterval(LocalDate relativeTo);

    public abstract void writeTo(StringBuilder buffer);

    public String getCode()
    {
        StringBuilder buf = new StringBuilder();
        writeTo(buf);
        return buf.toString();
    }

    public static class LastX extends ReportingPeriod
    {
        private static final char CODE = 'L';

        private final int years;
        private final int months;

        /* package */ LastX(String code)
        {
            this(Integer.parseInt(code.substring(1, code.indexOf('Y'))), //
                            Integer.parseInt(code.substring(code.indexOf('Y') + 1)));
        }

        public LastX(int years, int months)
        {
            this.years = years;
            this.months = months;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(relativeTo.minusYears(years).minusMonths(months), relativeTo);
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

    public static class LastXDays extends ReportingPeriod
    {
        private static final char CODE = 'D';

        private final int days;

        /* package */ LastXDays(String code)
        {
            this(Integer.parseInt(code.substring(1)));
        }

        public LastXDays(int days)
        {
            this.days = days;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(relativeTo.minusDays(days), relativeTo);
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(days);
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodLastXDays, days);
        }
    }

    public static class LastXTradingDays extends ReportingPeriod
    {
        private static final char CODE = 'T';

        private final int tradingDays;

        /* package */ LastXTradingDays(String code)
        {
            this(Integer.parseInt(code.substring(1)));
        }

        public LastXTradingDays(int tradingDays)
        {
            this.tradingDays = tradingDays;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(tradingDaysSince(relativeTo, tradingDays), relativeTo);
        }

        /* testing */ static final LocalDate tradingDaysSince(LocalDate start, int tradingDays)
        {
            TradeCalendar calendar = TradeCalendarManager.getDefaultInstance();

            LocalDate date = start;
            int daysToGo = tradingDays;

            while (daysToGo > 0)
            {
                if (!calendar.isHoliday(date))
                    daysToGo--;

                date = date.minusDays(1);
            }

            while (calendar.isHoliday(date))
                date = date.minusDays(1);

            return date;
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(tradingDays);
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodLastXTradingDays, tradingDays);
        }
    }

    public static class FromXtoY extends ReportingPeriod
    {
        private static final char CODE = 'F';

        private final LocalDate startDate;
        private final LocalDate endDate;

        /* package */ FromXtoY(String code)
        {
            this(LocalDate.parse(code.substring(1, code.indexOf('_'))),
                            LocalDate.parse(code.substring(code.indexOf('_') + 1)));
        }

        public FromXtoY(LocalDate startDate, LocalDate endDate)
        {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public FromXtoY(Interval interval)
        {
            this(interval.getStart(), interval.getEnd());
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(startDate, endDate);
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(startDate.toString()).append('_').append(endDate.toString());
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodFromXtoY, startDate.format(DATE_FORMATTER),
                            endDate.format(DATE_FORMATTER));
        }
    }

    public static class SinceX extends ReportingPeriod
    {
        private static final char CODE = 'S';

        private final LocalDate startDate;

        /* package */ SinceX(String code)
        {
            this(LocalDate.parse(code.substring(1)));
        }

        public SinceX(LocalDate startDate)
        {
            this.startDate = startDate;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            if (startDate.isBefore(relativeTo))
                return Interval.of(startDate, relativeTo);
            else
                return Interval.of(startDate, startDate); // FIXME
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(startDate.toString());
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodSince, startDate.format(DATE_FORMATTER));
        }

    }

    public static class YearX extends ReportingPeriod
    {
        private static final char CODE = 'Y';

        private final int year;

        /* package */ YearX(String code)
        {
            this(Integer.parseInt(code.substring(1)));
        }

        public YearX(int year)
        {
            this.year = year;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(LocalDate.of(year - 1, 12, 31), LocalDate.of(year, 12, 31));
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE).append(year);
        }

        @Override
        public String toString()
        {
            return String.valueOf(year);
        }
    }

    public static class CurrentMonth extends ReportingPeriod
    {
        private static final char CODE = 'M';

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            LocalDate startDate = LocalDate.now().withDayOfMonth(1).minusDays(1);

            if (startDate.isBefore(relativeTo))
                return Interval.of(startDate, relativeTo);
            else
                return Interval.of(startDate, startDate); // FIXME
        }

        @Override
        public void writeTo(StringBuilder buffer)
        {
            buffer.append(CODE);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodCurrentMonth;
        }
    }
}
