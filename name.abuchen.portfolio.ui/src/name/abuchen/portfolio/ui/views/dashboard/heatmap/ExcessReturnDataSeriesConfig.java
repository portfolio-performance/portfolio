package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public class ExcessReturnDataSeriesConfig extends DataSeriesConfig
{
    public ExcessReturnDataSeriesConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, true, true, null, Messages.LabelExcessReturnBaselineDataSeries,
                        Dashboard.Config.SECONDARY_DATA_SERIES);
    }

}
