package name.abuchen.portfolio.util;

import java.net.URL;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.stream.Stream;

import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;
import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Security;

public class TradeCalendarManager
{
    private static String defaultCalendarCode = "trade-calendar-default"; //$NON-NLS-1$

    private static final Map<String, TradeCalendar> CACHE = new HashMap<>();

    static
    {
        // load default trading calendars

        Set<HolidayCalendar> calendars = EnumSet.of(HolidayCalendar.NYSE, HolidayCalendar.LONDON_METAL_EXCHANGE,
                        HolidayCalendar.DOW_JONES_STOXX);

        for (HolidayCalendar calendar : calendars)
        {
            try
            {
                HolidayManager tradingDayManager = HolidayManager.getInstance(ManagerParameters.create(calendar));
                String calendarCode = calendar.toString();
                CACHE.put(calendarCode, new TradeCalendar(calendar.toString(),
                                tradingDayManager.getCalendarHierarchy().getDescription(), tradingDayManager));
            }
            catch (MissingResourceException e)
            {
                // for one reason or another, when running Surefire Tycho tests,
                // the resource bundles cannot be loaded by JollyDay

                PortfolioLog.error(e);
            }
        }

        // load custom calendars

        Map<String, String> customCalendars = new HashMap<>();
        customCalendars.put("trade-calendar-default.xml", Messages.LabelTradeCalendarDefault); //$NON-NLS-1$
        customCalendars.put("trade-calendar-de.xml", Messages.LabelTradeCalendarGermany); //$NON-NLS-1$

        customCalendars.forEach((code, description) -> {
            URL url = TradeCalendarManager.class.getResource(code);
            HolidayManager m = HolidayManager.getInstance(ManagerParameters.create(url));
            CACHE.put(m.getCalendarHierarchy().getId(),
                            new TradeCalendar(m.getCalendarHierarchy().getId(), description, m));
        });
    }

    private TradeCalendarManager()
    {}

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

    public static TradeCalendar createEmpty()
    {
        String description = MessageFormat.format(Messages.LabelTradeCalendarUseDefault,
                        getDefaultInstance().getDescription());
        return new TradeCalendar("", description, null); //$NON-NLS-1$
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
