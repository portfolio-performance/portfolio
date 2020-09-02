package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

class HeatmapOrnamentConfig extends EnumBasedConfig<HeatmapOrnament>
{
    public HeatmapOrnamentConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.LabelHeatmapOrnament, HeatmapOrnament.class, Dashboard.Config.LAYOUT, Policy.MULTIPLE);
    }
}