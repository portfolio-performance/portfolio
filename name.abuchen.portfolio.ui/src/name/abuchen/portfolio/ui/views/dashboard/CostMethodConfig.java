package name.abuchen.portfolio.ui.views.dashboard;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;

public class CostMethodConfig extends EnumBasedConfig<CostMethod>
{
    public CostMethodConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, Messages.LabelCapitalGainsMethod, CostMethod.class, Dashboard.Config.COST_METHOD,
                        Policy.EXACTLY_ONE);
    }
}

