package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;

public class ClientFilterApplyToAll
{
    private final DashboardData dashboardData;

    public ClientFilterApplyToAll(DashboardData dashboardData)
    {
        this.dashboardData = dashboardData;
    }

    public void menuAboutToShow(MenuManager manager, Composite columnControl)
    {
        var subMenu = new MenuManager(Messages.LabelClientFilter);
        var menu = new ClientFilterMenu(dashboardData.getClient(), dashboardData.getPreferences());
        menu.addListener(filter -> apply(menu.getSelectedItem(), columnControl));
        menu.menuAboutToShow(subMenu);
        manager.add(subMenu);
    }

    private void apply(ClientFilterMenu.Item item, Composite columnControl)
    {
        for (Control child : columnControl.getChildren())
        {
            var delegate = (WidgetDelegate<?>) child.getData(DashboardView.DELEGATE_KEY);
            if (delegate != null)
                delegate.optionallyGet(ClientFilterConfig.class).ifPresent(config -> config.setSelectedItem(item));
        }
        columnControl.update();
    }
}
