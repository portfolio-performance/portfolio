package name.abuchen.portfolio.snapshot.reportingperiod;

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
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.UNKNOWN;
import static name.abuchen.portfolio.snapshot.ReportingPeriodType.YEAR_X;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;

public class ReportingPeriodTypeTest
{

    @Test
    public void testNull()
    {
        ReportingPeriodType type = ReportingPeriodType.fromString(null);

        assertEquals(UNKNOWN, type); // NOSONAR
    }
    
    @Test
    public void testEmpty()
    {
        ReportingPeriodType type = ReportingPeriodType.fromString("");

        assertEquals(UNKNOWN, type);
    }
    
    @Test
    public void testEmptyWithBlank()
    {
        ReportingPeriodType type = ReportingPeriodType.fromString(" ");

        assertEquals(UNKNOWN, type);
    }
    
    @Test
    public void testInvalidInput()
    {
        ReportingPeriodType type = ReportingPeriodType.fromString("42");

        assertEquals(UNKNOWN, type);
    }
    
    @Test
    public void testCurrentWeekFallback()
    {
        String code = new ReportingPeriod.CurrentWeek().getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_WEEK, type);
    }
    
    @Test
    public void testCurrentMonthFallback()
    {
        String code = new ReportingPeriod.CurrentMonth().getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_MONTH, type);
    }
    
    @Test
    public void testCurrentQuarterFallback()
    {
        String code = new ReportingPeriod.CurrentQuarter().getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_QUARTER, type);
    }
    
    @Test
    public void testCurrentYearFallback()
    {
        String code = new ReportingPeriod.CurrentYear().getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_YEAR, type);
    }
    
    @Test
    public void testSinceXFallback()
    {
        LocalDate sinceDate = LocalDate.now();
        String code = new ReportingPeriod.SinceX(sinceDate).getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(SINCE_X, type);
    }
    
    @Test
    public void testFromXToYFallback()
    {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusDays(1);
        LocalDate endDate = now;
        String code = new ReportingPeriod.FromXtoY(startDate, endDate).getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(FROM_X_TO_Y, type);
    }
    
    @Test
    public void testYearXFallback()
    {
        int years = 2;
        String code = new ReportingPeriod.YearX(years).getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(YEAR_X, type);
    }
    
    @Test
    public void testPreviousXYearsYMonthsFallback()
    {
        int years = 2;
        int months = 6;
        String code = new ReportingPeriod.PreviousXYearsYMonths(years, months).getCode();

        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_X_YEARS_Y_MONTHS, type);
    }
    
    @Test
    public void testPreviousXYearsYMonthsLegacyFallback()
    {
        String code = "6Y";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_X_YEARS_Y_MONTHS, type);
    }
    
    @Test
    public void testPreviousXDaysFallback()
    {
        int days = 3;
        String code = new ReportingPeriod.PreviousXDays(days).getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_X_DAYS, type);
    }
    
    @Test
    public void testPreviousXTradingDaysFallback()
    {
        int days = 3;
        String code = new ReportingPeriod.PreviousXTradingDays(days).getCode();
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_X_TRADING_DAYS, type);
    }
    
    @Test
    public void testCurrentWeek()
    {
        String code = "CURRENT_WEEK";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_WEEK, type);
    }
    
    @Test
    public void testCurrentMonth()
    {
        String code = "CURRENT_MONTH";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_MONTH, type);
    }
    
    @Test
    public void testCurrentQuarter()
    {
        String code = "CURRENT_QUARTER";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_QUARTER, type);
    }
    
    @Test
    public void testCurrentYear()
    {
        String code = "CURRENT_YEAR";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(CURRENT_YEAR, type);
    }
    
    @Test
    public void testPreviousWeek()
    {
        String code = "PREVIOUS_WEEK";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_WEEK, type);
    }
    
    @Test
    public void testPreviousMonth()
    {
        String code = "PREVIOUS_MONTH";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_MONTH, type);
    }
    
    @Test
    public void testPreviousQuarter()
    {
        String code = "PREVIOUS_QUARTER";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_QUARTER, type);
    }
    
    @Test
    public void testPreviousYear()
    {
        String code = "PREVIOUS_YEAR";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_YEAR, type);
    }
    
    @Test
    public void testPreviousXDays()
    {
        String code = "PREVIOUS_X_DAYS=42";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_X_DAYS, type);
    }
    
    @Test
    public void testPreviousXTradingDays()
    {
        String code = "PREVIOUS_X_TRADING_DAYS=42";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_X_TRADING_DAYS, type);
    }
    
    @Test
    public void testPreviousXYearsYMonths()
    {
        String code = "PREVIOUS_X_YEARS_Y_MONTHS=3Y6";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(PREVIOUS_X_YEARS_Y_MONTHS, type);
    }
    
    @Test
    public void testSinceX()
    {
        String code = "SINCE_X=3";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(SINCE_X, type);
    }
    
    @Test
    public void testFromXToY()
    {
        String code = "FROM_X_TO_Y=2020-04-04_2020-04-08";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(FROM_X_TO_Y, type);
    }
    
    @Test
    public void testYearX()
    {
        String code = "YEAR_X=2020";
        ReportingPeriodType type = ReportingPeriodType.fromString(code);

        assertEquals(YEAR_X, type);
    }
}
