package name.abuchen.portfolio.ui.views.dashboard;

import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dataseries.PerformanceMetric;

public class PerformanceMetricConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;
    private PerformanceMetric metric = PerformanceMetric.TTWROR;

    public PerformanceMetricConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;

        try
        {
            String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.METRIC.name());
            if (code != null)
                metric = PerformanceMetric.valueOf(code);
        }
        catch (IllegalArgumentException ignore)
        {
            PortfolioPlugin.log(ignore);
        }
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(metric.toString()));

        MenuManager subMenu = new MenuManager(Messages.LabelPerformanceMetric);
        Arrays.stream(PerformanceMetric.values()).forEach(m -> {
            Action action = new SimpleAction(m.toString(), a -> {
                metric = m;
                delegate.getWidget().getConfiguration().put(Dashboard.Config.METRIC.name(), m.name());
                delegate.update();
                delegate.getClient().touch();
            });
            action.setChecked(metric == m);
            subMenu.add(action);
        });

        manager.add(subMenu);
    }

    public PerformanceMetric getMetric()
    {
        return metric;
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelPerformanceMetric + ": " + metric; //$NON-NLS-1$
    }
}
