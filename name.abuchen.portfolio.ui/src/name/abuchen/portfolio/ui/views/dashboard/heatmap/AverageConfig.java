package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public class AverageConfig extends EnumBasedConfig<Average>
{
    public AverageConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.HeatmapOrnamentAverage, Average.class, Dashboard.Config.LAYOUT, Policy.MULTIPLE);
    }
}
