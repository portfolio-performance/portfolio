package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.swt.widgets.Composite;

public interface WidgetDelegate
{

    Composite createControl(Composite parent, DashboardResources resources);

    void update(DashboardData data);

}
