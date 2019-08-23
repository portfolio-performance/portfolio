package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.LabelOnly;

public class ClientFilterConfig implements WidgetConfig
{
    private WidgetDelegate<?> delegate;
    private ClientFilterMenu menu;

    public ClientFilterConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;

        this.menu = new ClientFilterMenu(delegate.getClient(), delegate.getDashboardData().getPreferences(),
                        f -> filterSelected());

        String uuid = delegate.getWidget().getConfiguration().get(Dashboard.Config.CLIENT_FILTER.name());

        this.menu.getAllItems().filter(item -> item.getUUIDs().equals(uuid)).findAny()
                        .ifPresent(item -> this.menu.select(item));
    }

    private void filterSelected()
    {
        delegate.getWidget().getConfiguration().put(Dashboard.Config.CLIENT_FILTER.name(),
                        menu.getSelectedItem().getUUIDs());

        delegate.update();
        delegate.getClient().touch();
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelClientFilter + ": " + menu.getSelectedItem().getLabel(); //$NON-NLS-1$
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

        MenuManager subMenu = new MenuManager(Messages.LabelClientFilter);
        manager.add(subMenu);

        menu.menuAboutToShow(subMenu);
    }

    public ClientFilter getSelectedFilter()
    {
        return menu.getSelectedFilter();
    }

}
