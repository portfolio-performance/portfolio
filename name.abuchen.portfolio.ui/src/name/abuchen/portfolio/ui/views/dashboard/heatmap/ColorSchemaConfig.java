package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

class ColorSchemaConfig extends EnumBasedConfig<ColorSchema>
{
    public ColorSchemaConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.LabelColorSchema, ColorSchema.class, Dashboard.Config.COLOR_SCHEMA, Policy.EXACTLY_ONE);
    }
}