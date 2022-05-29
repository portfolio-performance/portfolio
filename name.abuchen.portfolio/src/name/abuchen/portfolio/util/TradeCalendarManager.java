package name.abuchen.portfolio.util;

import static name.abuchen.portfolio.util.HolidayName.ASCENSION_DAY;
import static name.abuchen.portfolio.util.HolidayName.ASSUMPTION_DAY;
import static name.abuchen.portfolio.util.HolidayName.BERCHTOLDSTAG;
import static name.abuchen.portfolio.util.HolidayName.BOXING_DAY;
import static name.abuchen.portfolio.util.HolidayName.CHRISTMAS;
import static name.abuchen.portfolio.util.HolidayName.CHRISTMAS_EVE;
import static name.abuchen.portfolio.util.HolidayName.CHRISTMAS_EVE_RUSSIA;
import static name.abuchen.portfolio.util.HolidayName.CIVIC_DAY;
import static name.abuchen.portfolio.util.HolidayName.CORPUS_CHRISTI;
import static name.abuchen.portfolio.util.HolidayName.DEFENDER_OF_THE_FATHERLAND_DAY;
import static name.abuchen.portfolio.util.HolidayName.EARLY_MAY_BANK_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.EASTER_MONDAY;
import static name.abuchen.portfolio.util.HolidayName.FAMILY_DAY;
import static name.abuchen.portfolio.util.HolidayName.FIRST_CHRISTMAS_DAY;
import static name.abuchen.portfolio.util.HolidayName.FUNERAL_OF_PRESIDENT_NIXON;
import static name.abuchen.portfolio.util.HolidayName.FUNERAL_OF_PRESIDENT_REAGAN;
import static name.abuchen.portfolio.util.HolidayName.GOOD_FRIDAY;
import static name.abuchen.portfolio.util.HolidayName.HURRICANE_SANDY;
import static name.abuchen.portfolio.util.HolidayName.INTERNATION_WOMENS_DAY;
import static name.abuchen.portfolio.util.HolidayName.INDEPENDENCE;
import static name.abuchen.portfolio.util.HolidayName.JUNETEENTH;
import static name.abuchen.portfolio.util.HolidayName.LABOUR_DAY;
import static name.abuchen.portfolio.util.HolidayName.MARTIN_LUTHER_KING;
import static name.abuchen.portfolio.util.HolidayName.MEMORIAL;
import static name.abuchen.portfolio.util.HolidayName.MILLENNIUM;
import static name.abuchen.portfolio.util.HolidayName.NATION_DAY;
import static name.abuchen.portfolio.util.HolidayName.NEW_YEAR;
import static name.abuchen.portfolio.util.HolidayName.NEW_YEARS_EVE;
import static name.abuchen.portfolio.util.HolidayName.NEW_YEAR_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.REFORMATION_DAY;
import static name.abuchen.portfolio.util.HolidayName.REMEMBERANCE_OF_PRESIDENT_FORD;
import static name.abuchen.portfolio.util.HolidayName.REPENTANCE_AND_PRAYER;
import static name.abuchen.portfolio.util.HolidayName.ROYAL_JUBILEE;
import static name.abuchen.portfolio.util.HolidayName.ROYAL_WEDDING;
import static name.abuchen.portfolio.util.HolidayName.SAINT_STEPHEN;
import static name.abuchen.portfolio.util.HolidayName.SECOND_CHRISTMAS_DAY;
import static name.abuchen.portfolio.util.HolidayName.SPRING_MAY_BANK_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.SUMMER_BANK_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.TERRORIST_ATTACKS;
import static name.abuchen.portfolio.util.HolidayName.THANKSGIVING;
import static name.abuchen.portfolio.util.HolidayName.UNIFICATION_GERMANY;
import static name.abuchen.portfolio.util.HolidayName.UNITY_DAY;
import static name.abuchen.portfolio.util.HolidayName.VICTORIA_DAY;
import static name.abuchen.portfolio.util.HolidayName.VICTORY_DAY;
import static name.abuchen.portfolio.util.HolidayName.WASHINGTONS_BIRTHDAY;
import static name.abuchen.portfolio.util.HolidayName.WHIT_MONDAY;
import static name.abuchen.portfolio.util.HolidayType.easter;
import static name.abuchen.portfolio.util.HolidayType.fixed;
import static name.abuchen.portfolio.util.HolidayType.weekday;

import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Security;

public class TradeCalendarManager
{
    private static String defaultCalendarCode = "default"; //$NON-NLS-1$

    public static final String TARGET2_CALENDAR_CODE = "TARGET2"; //$NON-NLS-1$
    public static final String FIRST_OF_THE_MONTH_CODE = "first-of-the-month"; //$NON-NLS-1$

    private static final Set<DayOfWeek> STANDARD_WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private static final Map<String, TradeCalendar> CACHE = new HashMap<>();

    static
    {
        TradeCalendar tc = new TradeCalendar("default", Messages.LabelTradeCalendarDefault, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(SECOND_CHRISTMAS_DAY, Month.DECEMBER, 26));
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("de", Messages.LabelTradeCalendarGermany, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(easter(ASCENSION_DAY, 39).validTo(1999));
        tc.add(easter(WHIT_MONDAY, 50).validTo(1999));
        tc.add(easter(WHIT_MONDAY, 50).onlyIn(2007));
        tc.add(easter(WHIT_MONDAY, 50).validFrom(2015).validTo(2021));
        tc.add(easter(CORPUS_CHRISTI, 60).validTo(1999));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(UNIFICATION_GERMANY, Month.JUNE, 17).validFrom(1954).validTo(1990));
        tc.add(fixed(UNIFICATION_GERMANY, Month.OCTOBER, 3).validFrom(1990).validTo(2000));
        tc.add(fixed(UNIFICATION_GERMANY, Month.OCTOBER, 3).validFrom(2014).validTo(2021));
        tc.add(fixed(REFORMATION_DAY, Month.OCTOBER, 31).onlyIn(2017));
        tc.add(fixed(REPENTANCE_AND_PRAYER, Month.NOVEMBER, 16).moveTo(DayOfWeek.WEDNESDAY).validTo(1994));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(SECOND_CHRISTMAS_DAY, Month.DECEMBER, 26));
        tc.add(fixed(NEW_YEARS_EVE, Month.DECEMBER, 31));
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("nyse", Messages.LabelTradeCalendarNYSE, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(weekday(MARTIN_LUTHER_KING, 3, DayOfWeek.MONDAY, Month.JANUARY).validFrom(1998));
        tc.add(weekday(WASHINGTONS_BIRTHDAY, 3, DayOfWeek.MONDAY, Month.FEBRUARY));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(weekday(MEMORIAL, -1, DayOfWeek.MONDAY, Month.MAY));
        tc.add(fixed(JUNETEENTH, Month.JUNE, 19).moveIf(DayOfWeek.SATURDAY, -1).moveIf(DayOfWeek.SUNDAY, 1).validFrom(2022));
        tc.add(fixed(INDEPENDENCE, Month.JULY, 4).moveIf(DayOfWeek.SATURDAY, -1).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(weekday(LABOUR_DAY, 1, DayOfWeek.MONDAY, Month.SEPTEMBER));
        tc.add(weekday(THANKSGIVING, 4, DayOfWeek.THURSDAY, Month.NOVEMBER));
        tc.add(fixed(CHRISTMAS, Month.DECEMBER, 25).moveIf(DayOfWeek.SATURDAY, -1).moveIf(DayOfWeek.SUNDAY, 1));
        // one-time closings since 1990; see https://www.bcm-news.de/wp-content/uploads/closings-nyse.pdf
        // for a complete list from 1885 to 2011
        tc.add(fixed(FUNERAL_OF_PRESIDENT_NIXON, Month.APRIL, 27).onlyIn(1994));
        for (int d = 11; d <= 14; d++)
            tc.add(fixed(TERRORIST_ATTACKS, Month.SEPTEMBER, d).onlyIn(2001));
        tc.add(fixed(FUNERAL_OF_PRESIDENT_REAGAN, Month.JUNE, 11).onlyIn(2004));
        tc.add(fixed(REMEMBERANCE_OF_PRESIDENT_FORD, Month.JANUARY, 2).onlyIn(2007));
        tc.add(fixed(HURRICANE_SANDY, Month.OCTOBER, 29).onlyIn(2012));
        tc.add(fixed(HURRICANE_SANDY, Month.OCTOBER, 30).onlyIn(2012));
        CACHE.put(tc.getCode(), tc);

        // see https://www.gov.uk/bank-holidays
        tc = new TradeCalendar("lse", Messages.LabelTradeCalendarLSE, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(weekday(EARLY_MAY_BANK_HOLIDAY, 1, DayOfWeek.MONDAY, Month.MAY).exceptIn(1995).exceptIn(2020));
        tc.add(weekday(SPRING_MAY_BANK_HOLIDAY, -1, DayOfWeek.MONDAY, Month.MAY).exceptIn(1977).exceptIn(2002).exceptIn(2012).exceptIn(2022));
        tc.add(weekday(SUMMER_BANK_HOLIDAY, -1, DayOfWeek.MONDAY, Month.AUGUST));
        tc.add(fixed(CHRISTMAS, Month.DECEMBER, 25).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 2));
            // strange but true: if 25th+26th is Sun+Mon, Christmas Day is moved *beyond* Boxing Day, to Tue
        tc.add(fixed(BOXING_DAY, Month.DECEMBER, 26).moveIf(DayOfWeek.SUNDAY, 2).moveIf(DayOfWeek.SATURDAY, 2));
        // one-time holidays; see https://en.wikipedia.org/wiki/Bank_holiday
        tc.add(fixed(ROYAL_WEDDING, Month.NOVEMBER, 14).onlyIn(1973)); // wedding of Princess Anne
        tc.add(fixed(SPRING_MAY_BANK_HOLIDAY, Month.JUNE, 6).onlyIn(1977)); // moved for four-day weekend
        tc.add(fixed(ROYAL_JUBILEE, Month.JUNE, 7).onlyIn(1977)); // Silver Jubilee of Elizabeth II
        tc.add(fixed(ROYAL_WEDDING, Month.JULY, 29).onlyIn(1981)); // wedding of Charles, Prince of Wales
        tc.add(fixed(EARLY_MAY_BANK_HOLIDAY, Month.MAY, 8).onlyIn(1995)); // moved for VE Day 50th anniversary
        tc.add(fixed(MILLENNIUM, Month.DECEMBER, 31).onlyIn(1999));
        tc.add(fixed(ROYAL_JUBILEE, Month.JUNE, 3).onlyIn(2002)); // Golden Jubilee of Elizabeth II
        tc.add(fixed(SPRING_MAY_BANK_HOLIDAY, Month.JUNE, 4).onlyIn(2002)); // moved for four-day weekend
        tc.add(fixed(ROYAL_WEDDING, Month.APRIL, 29).onlyIn(2011)); // wedding of Prince William
        tc.add(fixed(SPRING_MAY_BANK_HOLIDAY, Month.JUNE, 4).onlyIn(2012)); // moved for four-day weekend
        tc.add(fixed(ROYAL_JUBILEE, Month.JUNE, 5).onlyIn(2012)); // Diamond Jubilee of Elizabeth II
        tc.add(fixed(EARLY_MAY_BANK_HOLIDAY, Month.MAY, 8).onlyIn(2020)); // moved for VE Day 75th anniversary
        tc.add(fixed(SPRING_MAY_BANK_HOLIDAY, Month.JUNE, 2).onlyIn(2022)); // moved for four-day weekend
        tc.add(fixed(ROYAL_JUBILEE, Month.JUNE, 3).onlyIn(2022)); // Platinum Jubilee of Elizabeth II
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("euronext", Messages.LabelTradeCalendarEuronext, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(CHRISTMAS, Month.DECEMBER, 25));
        tc.add(fixed(SAINT_STEPHEN, Month.DECEMBER, 26));
        CACHE.put(tc.getCode(), tc);

        // see six trading days on their official website:
        // https://six-group.com/exchanges/exchange_traded_products/trading/trading_and_settlement_calendar_de.html
        tc = new TradeCalendar("six", Messages.LabelTradeCalendarSix, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(fixed(BERCHTOLDSTAG, Month.JANUARY, 2));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(easter(ASCENSION_DAY, 39));
        tc.add(easter(WHIT_MONDAY, 50));
        tc.add(fixed(NATION_DAY, Month.AUGUST, 1));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(SECOND_CHRISTMAS_DAY, Month.DECEMBER, 26));
        tc.add(fixed(NEW_YEARS_EVE, Month.DECEMBER, 31));
        CACHE.put(tc.getCode(), tc);

        // see Italian Stock Exchange trading days on their official website:
        // https://www.borsaitaliana.it/borsaitaliana/calendario-e-orari-di-negoziazione/calendario-borsa-orari-di-negoziazione.en.htm
        tc = new TradeCalendar("ise", Messages.LabelTradeCalendarISE, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(ASSUMPTION_DAY, Month.AUGUST, 15));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(SECOND_CHRISTMAS_DAY, Month.DECEMBER, 26));
        tc.add(fixed(NEW_YEARS_EVE, Month.DECEMBER, 31));
        CACHE.put(tc.getCode(), tc);

        // see Vienna Stock Exchange trading days on their official website:
        // https://www.wienerborse.at/handel/handelsinformationen/handelskalender/
        tc = new TradeCalendar("vse", Messages.LabelTradeCalendarVSE, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(easter(WHIT_MONDAY, 50));
        tc.add(fixed(NATION_DAY, Month.OCTOBER, 26));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(CHRISTMAS, Month.DECEMBER, 25));
        tc.add(fixed(SAINT_STEPHEN, Month.DECEMBER, 26));
        tc.add(fixed(NEW_YEARS_EVE, Month.DECEMBER, 31));
        CACHE.put(tc.getCode(), tc);

        // see Moscow Exchange trading days on their official website:
        // https://www.moex.com/s371
        // https://de.wikipedia.org/wiki/Feiertage_in_Russland
        // Die offizielle Regelung in Russland lautet: Wenn ein gesetzlicher Feiertag auf einen Samstag oder Sonntag fällt, wird der Feiertag auf einen Arbeitstag verlegt.
        tc = new TradeCalendar("MICEX-RTS", Messages.LabelTradeCalendarMICEXRTS, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(fixed(NEW_YEAR_HOLIDAY, Month.JANUARY, 2));
        tc.add(fixed(NEW_YEAR_HOLIDAY, Month.JANUARY, 3));
        tc.add(fixed(NEW_YEAR_HOLIDAY, Month.JANUARY, 4));
        tc.add(fixed(NEW_YEAR_HOLIDAY, Month.JANUARY, 5));
        tc.add(fixed(CHRISTMAS_EVE_RUSSIA, Month.JANUARY, 7));
        tc.add(fixed(DEFENDER_OF_THE_FATHERLAND_DAY, Month.FEBRUARY, 23));
        tc.add(fixed(INTERNATION_WOMENS_DAY, Month.MARCH, 8).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(fixed(VICTORY_DAY, Month.MAY, 9).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(fixed(NATION_DAY, Month.JUNE, 12).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(fixed(UNITY_DAY, Month.NOVEMBER, 4).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        CACHE.put(tc.getCode(), tc);

        // see Toronto Stock Exchange trading days on their official website:
        // https://www.tsx.com/trading/calendars-and-trading-hours/calendar
        tc = new TradeCalendar("tsx", Messages.LabelTradeCalendarTSX, STANDARD_WEEKEND); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(weekday(FAMILY_DAY, 3, DayOfWeek.MONDAY, Month.FEBRUARY));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(weekday(VICTORIA_DAY, -2, DayOfWeek.MONDAY, Month.MAY));
        tc.add(fixed(NATION_DAY, Month.JULY, 1).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(weekday(CIVIC_DAY, 1, DayOfWeek.MONDAY, Month.AUGUST));
        tc.add(weekday(LABOUR_DAY, 1, DayOfWeek.MONDAY, Month.SEPTEMBER));
        tc.add(weekday(THANKSGIVING, 2, DayOfWeek.MONDAY, Month.OCTOBER));
        tc.add(fixed(CHRISTMAS, Month.DECEMBER, 25).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(fixed(BOXING_DAY, Month.DECEMBER, 26).moveIf(DayOfWeek.MONDAY, 1).moveIf(DayOfWeek.SATURDAY, 2).moveIf(DayOfWeek.SUNDAY, 2));
        CACHE.put(tc.getCode(), tc);

        // TARGET2 (banking day in euro zone)
        // see https://www.ecb.europa.eu/press/pr/date/2000/html/pr001214_4.en.html
        tc = new TradeCalendar(TARGET2_CALENDAR_CODE, Messages.LabelTradeCalendarTARGET2, STANDARD_WEEKEND);
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(SECOND_CHRISTMAS_DAY, Month.DECEMBER, 26));
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar(FIRST_OF_THE_MONTH_CODE, Messages.LabelTradeCalendarFirstOfTheMonth, EnumSet.noneOf(DayOfWeek.class))
        {
            @Override
            /* package */ void add(HolidayType type)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isHoliday(LocalDate date)
            {
                return date.getDayOfMonth() != 1;
            }

            @Override
            public Collection<Holiday> getHolidays(int year)
            {
                // Only used for GUI
                return Collections.emptySet();
            }
        };
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("empty", Messages.LabelTradeCalendarEmpty, EnumSet.noneOf(DayOfWeek.class)); //$NON-NLS-1$
        CACHE.put(tc.getCode(), tc);
}

    private TradeCalendarManager()
    {
    }

    public static Stream<TradeCalendar> getAvailableCalendar()
    {
        return CACHE.values().stream();
    }

    public static void setDefaultCalendarCode(String defaultCalendarCode)
    {
        if (!CACHE.containsKey(defaultCalendarCode))
        {
            PortfolioLog.warning(
                            MessageFormat.format("Attempting to set unknown calendar code: {0}", defaultCalendarCode)); //$NON-NLS-1$
            return;
        }

        TradeCalendarManager.defaultCalendarCode = defaultCalendarCode;
    }

    public static TradeCalendar getInstance(String calendarCode)
    {
        return CACHE.get(calendarCode);
    }

    public static TradeCalendar createInheritDefaultOption()
    {
        String description = MessageFormat.format(Messages.LabelTradeCalendarUseDefault,
                        getDefaultInstance().getDescription());
        return new TradeCalendar("", description, STANDARD_WEEKEND); //$NON-NLS-1$
    }

    public static TradeCalendar getDefaultInstance()
    {
        return CACHE.get(defaultCalendarCode);
    }

    public static TradeCalendar getInstance(Security security)
    {
        TradeCalendar calendar = null;

        if (security.getCalendar() != null)
            calendar = CACHE.get(security.getCalendar());

        return calendar == null ? getDefaultInstance() : calendar;
    }
}
