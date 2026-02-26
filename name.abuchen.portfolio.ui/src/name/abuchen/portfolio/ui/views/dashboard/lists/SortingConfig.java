package name.abuchen.portfolio.ui.views.dashboard.lists;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public class SortingConfig extends EnumBasedConfig<SortDirection>
{
    public SortingConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.FollowUpWidget_Option_Sorting, SortDirection.class,
                        Dashboard.Config.SORT_DIRECTION, Policy.EXACTLY_ONE);
    }
}