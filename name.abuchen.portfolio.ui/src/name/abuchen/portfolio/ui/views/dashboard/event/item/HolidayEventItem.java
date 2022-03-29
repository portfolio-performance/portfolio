package name.abuchen.portfolio.ui.views.dashboard.event.item;

import java.time.LocalDate;
import java.util.Objects;

import name.abuchen.portfolio.util.Holiday;
import name.abuchen.portfolio.util.TradeCalendar;

public class HolidayEventItem extends EventItem
{

    private final Holiday holiday;
    private final TradeCalendar tradeCalendar; 
        
    public HolidayEventItem(EventType type, LocalDate date, Holiday holiday, TradeCalendar tradeCalendar)
    {
        super(type, date);
        
        this.holiday = holiday;
        this.tradeCalendar = tradeCalendar;
    }

    public Holiday getHoliday()
    {
        return holiday;
    }

    public TradeCalendar getTradeCalendar()
    {
        return tradeCalendar;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + Objects.hash(holiday, tradeCalendar);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        HolidayEventItem other = (HolidayEventItem) obj;
        return Objects.equals(holiday, other.holiday) && Objects.equals(tradeCalendar, other.tradeCalendar);
    }

    @Override
    public String toString()
    {
        return "HolidayEventItem [type=" + getType() +              //$NON-NLS-1$
                        ", date=" + getDate() +                     //$NON-NLS-1$
                        ", holiday=" + holiday.getLabel() + "]";    //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int compareTo(EventItem o)
    {
        int result = super.compareTo(o);
        
        if (result == 0 && o instanceof HolidayEventItem)
        {
            HolidayEventItem other = (HolidayEventItem) o;
            result = holiday.getDate().compareTo(other.getHoliday().getDate());
            
            if (result == 0)
            {
                result = tradeCalendar.compareTo(other.getTradeCalendar());
            }
        }
        
        return result;
    }

}
