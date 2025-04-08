package name.abuchen.portfolio.ui.views.dashboard.earnings;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

class EarningTypeConfig extends EnumBasedConfig<EarningType>
{
    public EarningTypeConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.LabelEarnings, EarningType.class, Dashboard.Config.EARNING_TYPE, Policy.EXACTLY_ONE);
    }
}
