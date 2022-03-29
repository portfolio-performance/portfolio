package name.abuchen.portfolio.ui.views.dashboard.event;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.DIVIDEND_DECLARATION;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.DIVIDEND_RECORD;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.EARNINGS_REPORT;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.EX_DIVIDEND;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.HOLIDAY;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.NOTE;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.PAYDAY;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.PAYMENT;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.SHAREHOLDER_MEETING;
import static name.abuchen.portfolio.ui.views.dashboard.event.item.EventType.STOCK_SPLIT;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.ui.views.dashboard.event.item.AccountEventItem;
import name.abuchen.portfolio.ui.views.dashboard.event.item.EventItem;
import name.abuchen.portfolio.ui.views.dashboard.event.item.EventType;
import name.abuchen.portfolio.ui.views.dashboard.event.item.HolidayEventItem;
import name.abuchen.portfolio.ui.views.dashboard.event.item.SecurityEventItem;
import name.abuchen.portfolio.util.Holiday;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;

public final class EventItemFactory
{

    private final Interval interval;
    
    public EventItemFactory(Interval interval)
    {
        this.interval = interval;
    }

    public List<EventItem> fromAccount(Account account)
    {
        List<EventItem> eventItems = new ArrayList<>();
        
        for (AccountTransaction accountTransaction : account.getTransactions())
        {
            LocalDate date = accountTransaction.getDateTime().toLocalDate();
            if (interval.contains(date)) 
            {
                Type transactionType = accountTransaction.getType();
                if (transactionType == AccountTransaction.Type.DIVIDENDS) 
                {
                    EventType eventType = PAYMENT;
                    Security security = accountTransaction.getSecurity();
                    String message = accountTransaction.getNote();
                    
                    SecurityEventItem securityEventItem = new SecurityEventItem(eventType, date, message, security);
                    
                    eventItems.add(securityEventItem);
                }
                else if (transactionType == AccountTransaction.Type.INTEREST)
                {
                    EventType eventType = PAYMENT;
                    String message = accountTransaction.getNote();
                    
                    AccountEventItem accountEventItem = new AccountEventItem(eventType, date, message, account);
                    
                    eventItems.add(accountEventItem);
                }
            }
        }
        
        return eventItems.isEmpty() ? emptyList() : eventItems;
    }
    
    public List<EventItem> fromSecurity(Security security)
    {
        List<EventItem> eventItems = new ArrayList<>();
        
        for (SecurityEvent securityEvent : security.getEvents())
        {
            LocalDate date = securityEvent.getDate();
            if (interval.contains(date)) 
            {
                EventType eventType = determineType(securityEvent);
                String message = securityEvent.getDetails();
                
                SecurityEventItem securityEventItem = new SecurityEventItem(eventType, date, message, security);
                
                eventItems.add(securityEventItem);
            }
        }
        
        return eventItems.isEmpty() ? emptyList() : eventItems;
    }
    
    public List<EventItem> fromTradeCalendar(TradeCalendar tradeCalendar)
    {
        List<EventItem> eventItems = new ArrayList<>();
        
        for (Integer year : yearsOfInterval(interval))
        {
            for (Holiday holiday : tradeCalendar.getHolidays(year))
            {
                LocalDate date = holiday.getDate();
                if (interval.contains(date)) 
                {
                    EventType eventType = HOLIDAY;
                    
                    HolidayEventItem holidayEventItem = 
                            new HolidayEventItem(eventType, date, holiday, tradeCalendar);
                    
                    eventItems.add(holidayEventItem);
                }
            }
        }
        
        return eventItems.isEmpty() ? emptyList() : eventItems;
    }
    
    private List<Integer> yearsOfInterval(Interval interval)
    {
        return interval.getYears().stream().map(Year::getValue).collect(toUnmodifiableList());
    }

    private EventType determineType(SecurityEvent securityEvent)
    {
        switch (securityEvent.getType())
        {
            case DIVIDEND_PAYMENT:
                return PAYMENT;

            case STOCK_SPLIT:
                return STOCK_SPLIT;

            case NOTE:
                return NOTE;

            case DIVIDEND_DECLARATION:
                return DIVIDEND_DECLARATION;

            case DIVIDEND_RECORD:
                return DIVIDEND_RECORD;

            case EARNINGS_REPORT:
                return EARNINGS_REPORT;

            case EX_DIVIDEND:
                return EX_DIVIDEND;

            case PAYDAY:
                return PAYDAY;

            case SHAREHOLDER_MEETING:
                return SHAREHOLDER_MEETING;

            default:
                throw new UnsupportedOperationException("unsupported security event type: " + securityEvent.getType()); //$NON-NLS-1$
        }
    }
    
}
