package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

class ExcessReturnOperatorConfig extends EnumBasedConfig<ExcessReturnOperator>
{
    public ExcessReturnOperatorConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.LabelExcessReturnOperator, ExcessReturnOperator.class,
                        Dashboard.Config.CALCULATION_METHOD, Policy.EXACTLY_ONE,
                        Dashboard.Config.SECONDARY_DATA_SERIES.name());
    }
}
