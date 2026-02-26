package name.abuchen.portfolio.ui.views.dashboard.charts;

import org.eclipse.jface.action.IMenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dashboard.WidgetConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public class IncludeSecuritiesConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;
    private boolean isSecuritiesIncluded = false;

    public IncludeSecuritiesConfig(WidgetDelegate<?> delegate, boolean defaultValue)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.FLAG_INCLUDE_SECURITIES.name());
        this.isSecuritiesIncluded = code != null ? Boolean.parseBoolean(code) : defaultValue;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        SimpleAction action = new SimpleAction(Messages.LabelIncludeSecuritiesInPieChart, a -> {
            this.isSecuritiesIncluded = !this.isSecuritiesIncluded;

            delegate.getWidget().getConfiguration().put(Dashboard.Config.FLAG_INCLUDE_SECURITIES.name(),
                            String.valueOf(this.isSecuritiesIncluded));

            delegate.update();
            delegate.getClient().touch();
        });

        action.setChecked(this.isSecuritiesIncluded);
        manager.add(action);
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelIncludeSecuritiesInPieChart + ": " //$NON-NLS-1$
                        + (this.isSecuritiesIncluded ? Messages.LabelYes : Messages.LabelNo);
    }

    public boolean isSecuritiesIncluded()
    {
        return this.isSecuritiesIncluded;
    }
}
