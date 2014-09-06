package name.abuchen.portfolio.ui.views.columns;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class WatchlistTargetPriceColumnLabelProvider extends ColumnLabelProvider
{

    private Watchlist watchlist;

    public WatchlistTargetPriceColumnLabelProvider(Watchlist watchlist)
    {
        this.watchlist = watchlist;
    }

    @Override
    public String getText(Object element)
    {
        Float targetPrice = watchlist.getTargetPriceForSecurity((Security) element);
        if (targetPrice == null) { return "not set"; }
        return targetPrice.toString();
    }

}
