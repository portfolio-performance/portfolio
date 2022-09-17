package name.abuchen.portfolio.snapshot;

import static java.time.temporal.IsoFields.DAY_OF_QUARTER;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Objects;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public abstract class ReportingPeriod
{
    public enum Type
    {
        LAST_X_DAYS('D', LastXDays.class), //
        LAST_X_TRADING_DAYS('T', LastXTradingDays.class), //
        LAST_X_YEARS_Y_MONTHS('L', LastX.class), //
        PREVIOUS_DAY('P', PreviousDay.class), //
        PREVIOUS_TRADING_DAY('E', PreviousTradingDay.class), //
        PREVIOUS_WEEK('A', PreviousWeek.class), //
        PREVIOUS_MONTH('B', PreviousMonth.class), //
        PREVIOUS_QUARTER('C', PreviousQuarter.class), //
        PREVIOUS_YEAR('Z', PreviousYear.class), //
        CURRENT_WEEK('W', CurrentWeek.class), //
        CURRENT_MONTH('M', CurrentMonth.class), //
        CURRENT_QUARTER('Q', CurrentQuarter.class), //
        YEAR_TO_DATE('X', YearToDate.class), //
        SINCE_X('S', SinceX.class), //
        FROM_X_TO_Y('F', FromXtoY.class), //
        YEAR_X('Y', YearX.class);

        private char code;

        private Class<? extends ReportingPeriod> implementation;

        private Type(char code, Class<? extends ReportingPeriod> implementation)
        {
            this.code = code;
            this.implementation = implementation;
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private static final Map<Character, Type> CODE2TYPE = Stream.of(Type.values())
                    .collect(Collectors.toMap(t -> t.code, t -> t));

    private static final Map<Class<? extends ReportingPeriod>, Type> CLASS2TYPE = Stream.of(Type.values())
                    .collect(Collectors.toMap(t -> t.implementation, t -> t));

    private ReportingPeriod()
    {
    }

    public static final ReportingPeriod from(String code) throws IOException
    {
        Type type = CODE2TYPE.get(code.charAt(0));

        if (type != null)
        {
            try
            {
                if (code.length() > 1)
                {
                    return type.implementation.getConstructor(String.class).newInstance(code.substring(1));
                }
                else
                {
                    return type.implementation.getDeclaredConstructor().newInstance();
                }
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                throw new IOException(e);
            }
        }

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

    protected void writeTo(StringBuilder buffer)
    {
    }

    public String getCode()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(CLASS2TYPE.get(this.getClass()).code);
        writeTo(buf);
        return buf.toString();
    }

    public static class LastX extends ReportingPeriod
    {
        private final int years;
        private final int months;

        public LastX(String code)
        {
            this(Integer.parseInt(code.substring(0, code.indexOf('Y'))), //
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
            buffer.append(years).append('Y').append(months);
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

        @Override
        public int hashCode()
        {
            return Objects.hashCode(years, months);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LastX other = (LastX) obj;
            return years == other.years && months == other.months;
        }
    }

    public static class LastXDays extends ReportingPeriod
    {
        private final int days;

        public LastXDays(String code)
        {
            this(Integer.parseInt(code));
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
            buffer.append(days);
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodLastXDays, days);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(days);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LastXDays other = (LastXDays) obj;
            return days == other.days;
        }
    }

    public static class LastXTradingDays extends ReportingPeriod
    {
        private final int tradingDays;

        public LastXTradingDays(String code)
        {
            this(Integer.parseInt(code));
        }

        public LastXTradingDays(int tradingDays)
        {
            this.tradingDays = tradingDays;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(tradingDaysUntil(relativeTo, tradingDays), relativeTo);
        }

        public static final LocalDate tradingDaysUntil(LocalDate referenceDate, int tradingDays)
        {
            TradeCalendar calendar = TradeCalendarManager.getDefaultInstance();

            LocalDate date = referenceDate;
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
            buffer.append(tradingDays);
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodLastXTradingDays, tradingDays);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(tradingDays);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LastXTradingDays other = (LastXTradingDays) obj;
            return tradingDays == other.tradingDays;
        }
    }

    public static class FromXtoY extends ReportingPeriod
    {
        private final LocalDate startDate;
        private final LocalDate endDate;

        public FromXtoY(String code)
        {
            this(LocalDate.parse(code.substring(0, code.indexOf('_'))),
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
            buffer.append(startDate.toString()).append('_').append(endDate.toString());
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodFromXtoY, startDate.format(DATE_FORMATTER),
                            endDate.format(DATE_FORMATTER));
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(startDate, endDate);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FromXtoY other = (FromXtoY) obj;
            return startDate.equals(other.startDate) && endDate.equals(other.endDate);
        }
    }

    public static class SinceX extends ReportingPeriod
    {
        private final LocalDate startDate;

        public SinceX(String code)
        {
            this(LocalDate.parse(code));
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
            buffer.append(startDate.toString());
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodSince, startDate.format(DATE_FORMATTER));
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(startDate);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SinceX other = (SinceX) obj;
            return startDate.equals(other.startDate);
        }
    }

    public static class YearX extends ReportingPeriod
    {
        private final int year;

        public YearX(String code)
        {
            this(Integer.parseInt(code));
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
            buffer.append(year);
        }

        @Override
        public String toString()
        {
            return String.valueOf(year);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(year);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            YearX other = (YearX) obj;
            return year == other.year;
        }
    }

    public static class CurrentWeek extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            // the first day of the week is local-specific: in Germany it is the
            // Monday, in the US it is the Sunday

            final DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();

            // reporting periods always run from the end of the day of the
            // starting data. In order to include the full week, we need to
            // start at the end of Sunday

            LocalDate firstDay = relativeTo.with(previousOrSame(firstDayOfWeek)).minusDays(1);
            LocalDate lastDay = firstDay.plusDays(7);

            return Interval.of(firstDay, lastDay);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodCurrentWeek;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.CURRENT_WEEK);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class CurrentMonth extends ReportingPeriod
    {
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
        public String toString()
        {
            return Messages.LabelReportingPeriodCurrentMonth;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.CURRENT_MONTH);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class CurrentQuarter extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            LocalDate firstDayOfQuarter = relativeTo.with(DAY_OF_QUARTER, 1L);
            LocalDate lastDayOfQuarter = firstDayOfQuarter.plusMonths(2).with(lastDayOfMonth());

            return Interval.of(firstDayOfQuarter.minusDays(1), lastDayOfQuarter);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodCurrentQuarter;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.CURRENT_QUARTER);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class YearToDate extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            // a reporting period is half-open, i.e. it excludes the first day
            // but includes the last day
            LocalDate startDate = LocalDate.now().withDayOfMonth(1).withMonth(1).minusDays(1);

            if (startDate.isBefore(relativeTo))
                return Interval.of(startDate, relativeTo);
            else
                return Interval.of(startDate, startDate); // FIXME
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodYTD;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.YEAR_TO_DATE);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class PreviousDay extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            LocalDate firstDay = relativeTo.minusDays(2);
            LocalDate lastDay = relativeTo.minusDays(1);

            return Interval.of(firstDay, lastDay);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodPreviousDay;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.PREVIOUS_DAY);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class PreviousTradingDay extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            TradeCalendar calendar = TradeCalendarManager.getDefaultInstance();

            LocalDate tradingDay = relativeTo.minusDays(1);
            while (calendar.isHoliday(tradingDay))
                tradingDay = tradingDay.minusDays(1);

            return Interval.of(tradingDay.minusDays(1), tradingDay);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodPreviousTradingDay;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.PREVIOUS_TRADING_DAY);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class PreviousWeek extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            final DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();

            LocalDate firstDay = relativeTo.minusWeeks(1).with(previousOrSame(firstDayOfWeek)).minusDays(1);
            LocalDate lastDay = firstDay.plusDays(7);

            return Interval.of(firstDay, lastDay);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodPreviousWeek;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.PREVIOUS_WEEK);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class PreviousMonth extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            LocalDate startMonth = relativeTo.minusMonths(1).with(firstDayOfMonth());
            LocalDate endMonth = relativeTo.minusMonths(1).with(lastDayOfMonth());

            LocalDate intervalStart = startMonth.minusDays(1);
            LocalDate intervalEnd = endMonth;

            return Interval.of(intervalStart, intervalEnd);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodPreviousMonth;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.PREVIOUS_MONTH);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class PreviousQuarter extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            LocalDate firstDayOfCurrentQuarter = relativeTo.with(DAY_OF_QUARTER, 1L);

            LocalDate firstDayOfLastQuarter = firstDayOfCurrentQuarter.minusMonths(3);
            LocalDate lastDayOfLastQuarter = firstDayOfLastQuarter.plusMonths(2).with(lastDayOfMonth());

            LocalDate intervalStart = firstDayOfLastQuarter.minusDays(1);
            LocalDate intervalEnd = lastDayOfLastQuarter;

            return Interval.of(intervalStart, intervalEnd);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodPreviousQuarter;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.PREVIOUS_QUARTER);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }

    public static class PreviousYear extends ReportingPeriod
    {
        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            LocalDate firstDayOfLastYear = relativeTo.withDayOfMonth(1).withMonth(1).minusYears(1);
            LocalDate lastDayOfLastYear = firstDayOfLastYear.withMonth(12).with(lastDayOfMonth());

            LocalDate intervalStart = firstDayOfLastYear.minusDays(1);
            LocalDate intervalEnd = lastDayOfLastYear;

            return Interval.of(intervalStart, intervalEnd);
        }

        @Override
        public String toString()
        {
            return Messages.LabelReportingPeriodPreviousYear;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(Type.PREVIOUS_YEAR);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return getClass() == obj.getClass();
        }
    }
}
