package name.abuchen.portfolio.ui.views.dashboard.event.item;

import java.util.ResourceBundle;

import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.ui.Images;

public enum EventType
{
    
    DIVIDEND_DECLARATION(Images.CHEVRON.image()),
    EX_DIVIDEND(Images.WARNING.image()),
    DIVIDEND_RECORD(Images.CLOCK.image()),
    PAYDAY(Images.GREEN_ARROW.image()),
    PAYMENT(Images.ACCOUNT.image()),
    STOCK_SPLIT(Images.VIEW_REBALANCING.image()),
    EARNINGS_REPORT(Images.VIEW_LINECHART.image()),
    SHAREHOLDER_MEETING(Images.INFO.image()),
    NOTE(Images.BOOKMARK.image()),
    HOLIDAY(Images.CALENDAR_OFF.image());
    
    
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.ui.views.labels"); //$NON-NLS-1$
    
    private final Image icon;
    
    private EventType(Image icon)
    {
        this.icon = icon;
    }

    public Image getIcon()
    {
        return icon;
    }

    @Override
    public String toString()
    {
        return RESOURCES.getString("event.type." + name()); //$NON-NLS-1$
    }
    
}