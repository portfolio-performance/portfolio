package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;

public abstract class WidgetDelegate
{
    private final Dashboard.Widget widget;
    private final DashboardData data;
    private final List<WidgetConfig> config = new ArrayList<>();

    public WidgetDelegate(Dashboard.Widget widget, DashboardData data)
    {
        this.widget = widget;
        this.data = data;

        addConfig(new LabelConfig(this));
    }

    protected final void addConfig(WidgetConfig config)
    {
        this.config.add(config);
    }

    protected Client getClient()
    {
        return data.getClient();
    }

    protected Dashboard.Widget getWidget()
    {
        return widget;
    }

    protected DashboardData getDashboardData()
    {
        return data;
    }

    protected <C extends WidgetConfig> C get(Class<C> type)
    {
        return type.cast(config.stream().filter(c -> type.equals(c.getClass())).findAny().get());
    }

    public void configMenuAboutToShow(IMenuManager manager)
    {
        config.stream().forEach(c -> c.menuAboutToShow(manager));
    }

    abstract Composite createControl(Composite parent, DashboardResources resources);

    abstract void update();

    abstract void attachContextMenu(IMenuListener listener);
}
