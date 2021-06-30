package name.abuchen.portfolio.snapshot;

import static java.util.Arrays.stream;
import static java.util.Locale.ENGLISH;

import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentMonth;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentQuarter;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentWeek;
import name.abuchen.portfolio.snapshot.ReportingPeriod.YearToDate;
import name.abuchen.portfolio.snapshot.ReportingPeriod.FromXtoY;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastX;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXDays;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastYear;
import name.abuchen.portfolio.snapshot.ReportingPeriod.SinceX;
import name.abuchen.portfolio.snapshot.ReportingPeriod.YearX;

public enum ReportingPeriodType 
{
    
    PREVIOUS_X_DAYS,
    PREVIOUS_X_TRADING_DAYS,
    PREVIOUS_X_YEARS_Y_MONTHS,
    PREVIOUS_WEEK,
    PREVIOUS_MONTH,
    PREVIOUS_QUARTER,
    PREVIOUS_YEAR,
    CURRENT_WEEK,
    CURRENT_MONTH,
    CURRENT_QUARTER,
    CURRENT_YEAR,
    SINCE_X,
    FROM_X_TO_Y,
    YEAR_X,
    UNKNOWN;
    
    public int getLength() 
    {
        return name().length();
    }
    
    public static ReportingPeriodType fromString(String s) 
    {
        if (s == null || s.isEmpty()) return UNKNOWN;
        
        s = s.trim();
        if (s.isEmpty()) return UNKNOWN;
        
        final String code = s.toUpperCase(ENGLISH);

        ReportingPeriodType type = stream(values())
                .filter(value -> ! value.equals(UNKNOWN))
                .filter(value -> code.contains(value.name()))
                .findAny()
                .orElseGet(() -> applyFallback(code));
        
        return type;
    }

    static ReportingPeriodType applyFallback(String code)
    {
        char type = code.charAt(0);

        if (type == LastX.CODE)
            return PREVIOUS_X_YEARS_Y_MONTHS;
        else if (type == LastXDays.CODE)
            return PREVIOUS_X_DAYS;
        else if (type == LastXTradingDays.CODE)
            return PREVIOUS_X_TRADING_DAYS;
        else if (type == FromXtoY.CODE)
            return FROM_X_TO_Y;
        else if (type == SinceX.CODE)
            return SINCE_X;
        else if (type == YearX.CODE)
            return YEAR_X;
        else if (type == CurrentWeek.CODE)
            return CURRENT_WEEK;
        else if (type == CurrentMonth.CODE)
            return CURRENT_MONTH;
        else if (type == CurrentQuarter.CODE)
            return CURRENT_QUARTER;
        else if (type == YearToDate.CODE)
            return CURRENT_YEAR;
        else if (type == LastYear.CODE)
            return PREVIOUS_YEAR;

        // backward compatible
        if (code.charAt(code.length() - 1) == 'Y')
            return PREVIOUS_X_YEARS_Y_MONTHS;
        
        return UNKNOWN;
    }
    
}