package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class ChartShowYAxisConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;
    private boolean isShowYAxis = false;

    public ChartShowYAxisConfig(WidgetDelegate<?> delegate, boolean defaultValue)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.SHOW_Y_AXIS.name());
        this.isShowYAxis = code != null ? Boolean.parseBoolean(code) : defaultValue;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        SimpleAction action = new SimpleAction(Messages.MenuChartShowYAxis, a -> {
            this.isShowYAxis = !this.isShowYAxis;

            delegate.getWidget().getConfiguration().put(Dashboard.Config.SHOW_Y_AXIS.name(),
                            String.valueOf(this.isShowYAxis));

            delegate.update();
            delegate.getClient().touch();
        });

        action.setChecked(this.isShowYAxis);
        manager.add(action);
    }

    @Override
    public String getLabel()
    {
        return Messages.MenuChartShowYAxis + ": " + (this.isShowYAxis ? Messages.LabelYes : Messages.LabelNo); //$NON-NLS-1$
    }

    public boolean getIsShowYAxis()
    {
        return this.isShowYAxis;
    }
}
