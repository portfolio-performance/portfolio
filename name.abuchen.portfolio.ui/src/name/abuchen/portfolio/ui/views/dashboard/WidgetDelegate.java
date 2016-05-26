package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;

public abstract class WidgetDelegate
{
    private final Dashboard.Widget widget;
    private final DashboardData data;

    public WidgetDelegate(Dashboard.Widget widget, DashboardData data)
    {
        this.widget = widget;
        this.data = data;
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

    abstract Composite createControl(Composite parent, DashboardResources resources);

    abstract void update();

    abstract void attachContextMenu(IMenuListener listener);

}
