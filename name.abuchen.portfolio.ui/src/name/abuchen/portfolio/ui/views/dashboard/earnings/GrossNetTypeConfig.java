package name.abuchen.portfolio.ui.views.dashboard.earnings;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

class GrossNetTypeConfig extends EnumBasedConfig<GrossNetType>
{
    public GrossNetTypeConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.LabelGrossNetCalculation, GrossNetType.class, Dashboard.Config.NET_GROSS,
                        Policy.EXACTLY_ONE);
    }
}
