package name.abuchen.portfolio.ui.views.dashboard.charts;

import org.eclipse.jface.action.IMenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dashboard.WidgetConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public class IncludeUnassignedCategoryConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;
    private boolean isUnassignedIncluded = false;

    public IncludeUnassignedCategoryConfig(WidgetDelegate<?> delegate, boolean defaultValue)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.FLAG_INCLUDE_UNASSIGNED.name());
        this.isUnassignedIncluded = code != null ? Boolean.parseBoolean(code) : defaultValue;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        SimpleAction action = new SimpleAction(Messages.LabelIncludeUnassignedCategoryInCharts, a -> {
            this.isUnassignedIncluded = !this.isUnassignedIncluded;

            delegate.getWidget().getConfiguration().put(Dashboard.Config.FLAG_INCLUDE_UNASSIGNED.name(),
                            String.valueOf(this.isUnassignedIncluded));

            delegate.update();
            delegate.getClient().touch();
        });

        action.setChecked(this.isUnassignedIncluded);
        manager.add(action);
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelIncludeUnassignedCategoryInCharts + ": " //$NON-NLS-1$
                        + (this.isUnassignedIncluded ? Messages.LabelYes : Messages.LabelNo);
    }

    public boolean isUnassignedCategoryIncluded()
    {
        return this.isUnassignedIncluded;
    }
}
