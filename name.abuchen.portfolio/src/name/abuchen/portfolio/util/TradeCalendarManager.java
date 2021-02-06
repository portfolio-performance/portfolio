package name.abuchen.portfolio.util;

import static name.abuchen.portfolio.util.HolidayName.ASCENSION_DAY;
import static name.abuchen.portfolio.util.HolidayName.ASSUMPTION_DAY;
import static name.abuchen.portfolio.util.HolidayName.BERCHTOLDSTAG;
import static name.abuchen.portfolio.util.HolidayName.BOXING_DAY;
import static name.abuchen.portfolio.util.HolidayName.CHRISTMAS;
import static name.abuchen.portfolio.util.HolidayName.CHRISTMAS_EVE;
import static name.abuchen.portfolio.util.HolidayName.CHRISTMAS_EVE_RUSSIA;
import static name.abuchen.portfolio.util.HolidayName.DEFENDER_OF_THE_FATHERLAND_DAY;
import static name.abuchen.portfolio.util.HolidayName.EARLY_MAY_BANK_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.EASTER_MONDAY;
import static name.abuchen.portfolio.util.HolidayName.FIRST_CHRISTMAS_DAY;
import static name.abuchen.portfolio.util.HolidayName.FUNERAL_OF_PRESIDENT_REAGAN;
import static name.abuchen.portfolio.util.HolidayName.GOOD_FRIDAY;
import static name.abuchen.portfolio.util.HolidayName.HURRICANE_SANDY;
import static name.abuchen.portfolio.util.HolidayName.INTERNATION_WOMENS_DAY;
import static name.abuchen.portfolio.util.HolidayName.INDEPENDENCE;
import static name.abuchen.portfolio.util.HolidayName.LABOUR_DAY;
import static name.abuchen.portfolio.util.HolidayName.MARTIN_LUTHER_KING;
import static name.abuchen.portfolio.util.HolidayName.MEMORIAL;
import static name.abuchen.portfolio.util.HolidayName.NATION_DAY;
import static name.abuchen.portfolio.util.HolidayName.NEW_YEAR;
import static name.abuchen.portfolio.util.HolidayName.NEW_YEARS_EVE;
import static name.abuchen.portfolio.util.HolidayName.NEW_YEAR_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.REFORMATION_DAY;
import static name.abuchen.portfolio.util.HolidayName.REMEMBERANCE_OF_PRESIDENT_FORD;
import static name.abuchen.portfolio.util.HolidayName.SECOND_CHRISTMAS_DAY;
import static name.abuchen.portfolio.util.HolidayName.SPRING_MAY_BANK_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.SUMMER_BANK_HOLIDAY;
import static name.abuchen.portfolio.util.HolidayName.THANKSGIVING;
import static name.abuchen.portfolio.util.HolidayName.UNIFICATION_GERMANY;
import static name.abuchen.portfolio.util.HolidayName.UNITY_DAY;
import static name.abuchen.portfolio.util.HolidayName.VICTORY_DAY;
import static name.abuchen.portfolio.util.HolidayName.WASHINGTONS_BIRTHDAY;
import static name.abuchen.portfolio.util.HolidayName.WHIT_MONDAY;
import static name.abuchen.portfolio.util.HolidayType.easter;
import static name.abuchen.portfolio.util.HolidayType.fixed;
import static name.abuchen.portfolio.util.HolidayType.last;
import static name.abuchen.portfolio.util.HolidayType.weekday;

import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Security;

public class TradeCalendarManager
{
    private static String defaultCalendarCode = "default"; //$NON-NLS-1$

    private static final Map<String, TradeCalendar> CACHE = new HashMap<>();

    static
    {
        TradeCalendar tc = new TradeCalendar("default", Messages.LabelTradeCalendarDefault); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(SECOND_CHRISTMAS_DAY, Month.DECEMBER, 26));
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("de", Messages.LabelTradeCalendarGermany); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(easter(WHIT_MONDAY, 50));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(UNIFICATION_GERMANY, Month.OCTOBER, 3).validFrom(1990));
        tc.add(fixed(REFORMATION_DAY, Month.OCTOBER, 31).validFrom(2017).validTo(2017));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(SECOND_CHRISTMAS_DAY, Month.DECEMBER, 26));
        tc.add(fixed(NEW_YEARS_EVE, Month.DECEMBER, 31));
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("nyse", Messages.LabelTradeCalendarNYSE); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1).moveIf(DayOfWeek.SATURDAY, -1).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(fixed(REMEMBERANCE_OF_PRESIDENT_FORD, Month.JANUARY, 2).validFrom(2007).validTo(2007));
        tc.add(weekday(MARTIN_LUTHER_KING, 3, DayOfWeek.MONDAY, Month.JANUARY));
        tc.add(weekday(WASHINGTONS_BIRTHDAY, 3, DayOfWeek.MONDAY, Month.FEBRUARY));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(last(MEMORIAL, DayOfWeek.MONDAY, Month.MAY));
        tc.add(fixed(FUNERAL_OF_PRESIDENT_REAGAN, Month.JUNE, 11).validFrom(2004).validTo(2004));
        tc.add(fixed(INDEPENDENCE, Month.JULY, 4).moveIf(DayOfWeek.SATURDAY, -1).moveIf(DayOfWeek.SUNDAY, 1));
        tc.add(weekday(LABOUR_DAY, 1, DayOfWeek.MONDAY, Month.SEPTEMBER));
        tc.add(fixed(HURRICANE_SANDY, Month.OCTOBER, 29).validFrom(2012).validTo(2012));
        tc.add(fixed(HURRICANE_SANDY, Month.OCTOBER, 30).validFrom(2012).validTo(2012));
        tc.add(weekday(THANKSGIVING, 4, DayOfWeek.THURSDAY, Month.NOVEMBER));
        tc.add(fixed(CHRISTMAS, Month.DECEMBER, 25).moveIf(DayOfWeek.SATURDAY, -1).moveIf(DayOfWeek.SUNDAY, 1));
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("lse", Messages.LabelTradeCalendarLSE); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(weekday(EARLY_MAY_BANK_HOLIDAY, 1, DayOfWeek.MONDAY, Month.MAY));
        tc.add(last(SPRING_MAY_BANK_HOLIDAY, DayOfWeek.MONDAY, Month.MAY));
        tc.add(last(SUMMER_BANK_HOLIDAY, DayOfWeek.MONDAY, Month.AUGUST));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(BOXING_DAY, Month.DECEMBER, 26));
        CACHE.put(tc.getCode(), tc);

        tc = new TradeCalendar("euronext", Messages.LabelTradeCalendarEuronext); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(fixed(LABOUR_DAY, Month.MAY, 1));
        tc.add(fixed(FIRST_CHRISTMAS_DAY, Month.DECEMBER, 25));
        tc.add(fixed(BOXING_DAY, Month.DECEMBER, 26));
        CACHE.put(tc.getCode(), tc);

        // see six trading days on their official website:
        // https://six-group.com/exchanges/exchange_traded_products/trading/trading_and_settlement_calendar_de.html
        tc = new TradeCalendar("six", Messages.LabelTradeCalendarSix); //$NON-NLS-1$
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
        tc = new TradeCalendar("ise", Messages.LabelTradeCalendarISE); //$NON-NLS-1$
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
        tc = new TradeCalendar("vse", Messages.LabelTradeCalendarVSE); //$NON-NLS-1$
        tc.add(fixed(NEW_YEAR, Month.JANUARY, 1));
        tc.add(easter(GOOD_FRIDAY, -2));
        tc.add(easter(EASTER_MONDAY, 1));
        tc.add(easter(WHIT_MONDAY, 50));
        tc.add(fixed(NATION_DAY, Month.OCTOBER, 26));
        tc.add(fixed(CHRISTMAS_EVE, Month.DECEMBER, 24));
        tc.add(fixed(NEW_YEARS_EVE, Month.DECEMBER, 31));
        CACHE.put(tc.getCode(), tc);
        
        // see Moscow Exchange trading days on their official website:
        // https://www.moex.com/s371
        // https://de.wikipedia.org/wiki/Feiertage_in_Russland
        // Die offizielle Regelung in Russland lautet: Wenn ein gesetzlicher Feiertag auf einen Samstag oder Sonntag fällt, wird der Feiertag auf einen Arbeitstag verlegt.
        tc = new TradeCalendar("MICEX-RTS", Messages.LabelTradeCalendarMICEXRTS); //$NON-NLS-1$
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
        
        tc = new TradeCalendar(TradeCalendar.EMPTY_CODE, Messages.LabelTradeCalendarEmpty);
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
                            MessageFormat.format("Attempting to set unkown calendar code: {0}", defaultCalendarCode)); //$NON-NLS-1$
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
        return new TradeCalendar("", description); //$NON-NLS-1$
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
