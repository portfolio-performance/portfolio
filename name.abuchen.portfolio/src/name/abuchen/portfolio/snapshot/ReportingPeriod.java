package name.abuchen.portfolio.snapshot;

import static java.time.temporal.IsoFields.DAY_OF_QUARTER;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.CURRENT_MONTH;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.CURRENT_QUARTER;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.CURRENT_WEEK;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.CURRENT_YEAR;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.FROM_X_TO_Y;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_MONTH;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_QUARTER;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_WEEK;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_X_DAYS;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_X_TRADING_DAYS;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_X_YEARS_Y_MONTHS;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_YEAR;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.SINCE_X;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.YEAR_X;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.function.Predicate;

import com.google.common.base.Objects;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public abstract class ReportingPeriod
{
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    protected ReportingPeriodType type;
    
    protected ReportingPeriod(ReportingPeriodType type)
    {
        this.type = type;
    }

    public static final ReportingPeriod from(String code) throws IOException
    {
        ReportingPeriodType type = ReportingPeriodType.fromString(code);
        switch (type) 
        {
            case CURRENT_MONTH:
                return new CurrentMonth();
            case CURRENT_QUARTER:
                return new CurrentQuarter();
            case CURRENT_WEEK:
                return new CurrentWeek();
            case CURRENT_YEAR:
                return new CurrentYear();
            case FROM_X_TO_Y:
                return FromXtoY.fromString(code);
            case PREVIOUS_MONTH:
                return new PreviousMonth();
            case PREVIOUS_QUARTER:
                return new PreviousQuarter();
            case PREVIOUS_WEEK:
                return new PreviousWeek();
            case PREVIOUS_X_DAYS:
                return PreviousXDays.fromString(code);
            case PREVIOUS_X_TRADING_DAYS:
                return PreviousXTradingDays.fromString(code);
            case PREVIOUS_X_YEARS_Y_MONTHS:
                return PreviousXYearsYMonths.fromString(code);
            case PREVIOUS_YEAR:
                return new PreviousYear();
            case SINCE_X:
                return SinceX.fromString(code);
            case YEAR_X:
                return YearX.fromString(code);
            case UNKNOWN:
            default:
                throw new IOException("unsupported reporting period type: " + type + " for code " + code); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public final Predicate<Transaction> containsTransaction(LocalDate relativeTo)
    {
        Interval interval = toInterval(relativeTo);
        return t -> t.getDateTime().toLocalDate().isAfter(interval.getStart())
                        && !t.getDateTime().toLocalDate().isAfter(interval.getEnd());
    }

    public final void writeTo(StringBuilder buffer) 
    {
        buffer.append(JClient.GSON.toJson(this));
    }

    public final String getCode()
    {
        StringBuilder buf = new StringBuilder();
        writeTo(buf);
        return buf.toString();
    }
    
    public final ReportingPeriodType getType()
    {
        return type;
    }

    public abstract Interval toInterval(LocalDate relativeTo);
    

    public static class PreviousXYearsYMonths extends ReportingPeriod
    {
        static final char CODE = 'L';

        private final int years;
        private final int months;

        public PreviousXYearsYMonths(int years, int months)
        {
            super(PREVIOUS_X_YEARS_Y_MONTHS);
            
            this.years = years;
            this.months = months;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(relativeTo.minusYears(years).minusMonths(months), relativeTo);
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
            PreviousXYearsYMonths other = (PreviousXYearsYMonths) obj;
            return years == other.years && months == other.months;
        }

        public static ReportingPeriod fromString(String code)
        {
            if (code.startsWith("{")) //$NON-NLS-1$
            {
                return JClient.GSON.fromJson(code, PreviousXYearsYMonths.class);
            }
            
            int years;
            int months;
            if (code.endsWith("Y")) //$NON-NLS-1$
            {
                // backward compatibility - try to parse oldest, first format
                years = Integer.parseInt(code.substring(0, code.length() - 1));
                months = 0;
            }
            else
            {
                // backward compatibility - try to parse previous, old format
                years = Integer.parseInt(code.substring(1, code.lastIndexOf('Y')));
                months = Integer.parseInt(code.substring(code.lastIndexOf('Y') + 1));
            }
            
            return new PreviousXYearsYMonths(years, months);
        }
    }

    public static class PreviousXDays extends ReportingPeriod
    {
        static final char CODE = 'D';

        private final int days;

        public PreviousXDays(int days)
        {
            super(PREVIOUS_X_DAYS);
            
            this.days = days;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(relativeTo.minusDays(days), relativeTo);
        }

        @Override
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodPreviousXDays, days);
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
            PreviousXDays other = (PreviousXDays) obj;
            return days == other.days;
        }

        public static ReportingPeriod fromString(String code)
        {
            if (code.startsWith("{")) //$NON-NLS-1$
            {
                return JClient.GSON.fromJson(code, PreviousXDays.class);
            }
            else
            {
                // backward compatibility - try to parse old format
                int days = Integer.parseInt(code.substring(1));
                return new PreviousXDays(days);
            }
        }
    }

    public static class PreviousXTradingDays extends ReportingPeriod
    {
        static final char CODE = 'T';
        
        private final int tradingDays;

        public PreviousXTradingDays(int tradingDays)
        {
            super(PREVIOUS_X_TRADING_DAYS);
            
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
        public String toString()
        {
            return MessageFormat.format(Messages.LabelReportingPeriodPreviousXTradingDays, tradingDays);
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
            PreviousXTradingDays other = (PreviousXTradingDays) obj;
            return tradingDays == other.tradingDays;
        }

        public static ReportingPeriod fromString(String code)
        {
            if (code.startsWith("{")) //$NON-NLS-1$
            {
                return JClient.GSON.fromJson(code, PreviousXTradingDays.class);
            }
            else
            {
                // backward compatibility - try to parse old format
                int days = Integer.parseInt(code.substring(1));
                return new PreviousXTradingDays(days);
            }
        }
    }

    public static class FromXtoY extends ReportingPeriod
    {
        static final char CODE = 'F';
        
        private final LocalDate startDate;
        private final LocalDate endDate;

        public FromXtoY(LocalDate startDate, LocalDate endDate)
        {
            super(FROM_X_TO_Y);
            
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

        public static ReportingPeriod fromString(String code)
        {
            
            if (code.startsWith("{")) //$NON-NLS-1$
            {
                return JClient.GSON.fromJson(code, FromXtoY.class);
            }
            else
            {
                // backward compatibility - try to parse old format
                LocalDate x = LocalDate.parse(code.substring(1, code.lastIndexOf('_')));
                LocalDate y = LocalDate.parse(code.substring(code.lastIndexOf('_') + 1));
                return new FromXtoY(x, y);
            }
        }
    }

    public static class SinceX extends ReportingPeriod
    {
        static final char CODE = 'S';

        private final LocalDate startDate;

        public SinceX(LocalDate startDate)
        {
            super(SINCE_X);
            
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

        public static ReportingPeriod fromString(String code)
        {
            if (code.startsWith("{")) //$NON-NLS-1$
            {
                return JClient.GSON.fromJson(code, SinceX.class);
            }
            else
            {
                // backward compatibility - try to parse old format
                LocalDate since = LocalDate.parse(code.substring(1));
                return new SinceX(since);
            }
        }
    }

    public static class YearX extends ReportingPeriod
    {
        static final char CODE = 'Y';

        private final int year;

        public YearX(int year)
        {
            super(YEAR_X);
            
            this.year = year;
        }

        @Override
        public Interval toInterval(LocalDate relativeTo)
        {
            return Interval.of(LocalDate.of(year - 1, 12, 31), LocalDate.of(year, 12, 31));
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

        public static ReportingPeriod fromString(String code)
        {
            if (code.startsWith("{")) //$NON-NLS-1$
            {
                return JClient.GSON.fromJson(code, YearX.class);
            }
            else
            {
                // backward compatibility - try to parse old format
                int years = Integer.parseInt(code.substring(1));
                return new YearX(years);
            }
        }
    }

    public static class CurrentWeek extends ReportingPeriod
    {

        static final char CODE = 'W';

        public CurrentWeek()
        {
            super(CURRENT_WEEK);
        }

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
            return Objects.hashCode(type);
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
        static final char CODE = 'M';

        public CurrentMonth()
        {
            super(CURRENT_MONTH);
        }

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
            return Objects.hashCode(type);
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
        static final char CODE = 'Q';

        public CurrentQuarter()
        {
            super(CURRENT_QUARTER);
        }
        
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
            return Objects.hashCode(type);
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

    public static class CurrentYear extends ReportingPeriod
    {
        static final char CODE = 'X';

        public CurrentYear()
        {
            super(CURRENT_YEAR);
        }
        
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
            return Objects.hashCode(type);
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
        
        public PreviousWeek()
        {
            super(PREVIOUS_WEEK);
        }
        
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
            return Objects.hashCode(type);
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

        public PreviousMonth()
        {
            super(PREVIOUS_MONTH);
        }
        
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
            return Objects.hashCode(type);
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

        public PreviousQuarter()
        {
            super(PREVIOUS_QUARTER);
        }
        
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
            return Objects.hashCode(type);
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

        public PreviousYear()
        {
            super(PREVIOUS_YEAR);
        }
        
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
            return Messages.LabelReportingPeriodPreviousQuarter;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(type);
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
