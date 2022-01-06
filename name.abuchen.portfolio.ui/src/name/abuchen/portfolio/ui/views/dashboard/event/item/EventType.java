package name.abuchen.portfolio.ui.views.dashboard.event.item;

import java.util.ResourceBundle;

public enum EventType
{
    
    DIVIDEND_DECLARATION,
    EX_DIVIDEND,
    DIVIDEND_RECORD,
    PAYDAY,
    PAYMENT,
    STOCK_SPLIT,
    EARNINGS_REPORT,
    SHAREHOLDER_MEETING,
    NOTE,
    HOLIDAY;
    
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.ui.views.labels"); //$NON-NLS-1$
    
    
    @Override
    public String toString()
    {
        return RESOURCES.getString("event.type." + name()); //$NON-NLS-1$
    }
    
}