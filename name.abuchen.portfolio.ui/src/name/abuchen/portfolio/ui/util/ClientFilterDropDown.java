package name.abuchen.portfolio.ui.util;

import java.util.function.Consumer;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;

public final class ClientFilterDropDown extends AbstractDropDown
{
    private ClientFilterMenu menu;

    public ClientFilterDropDown(ToolBar toolBar, Client client, IPreferenceStore preferences,
                    Consumer<ClientFilter> listener)
    {
        super(toolBar, Messages.MenuChooseClientFilter, Images.FILTER_OFF.image(), SWT.NONE);

        this.menu = new ClientFilterMenu(client, preferences, listener);
        this.menu.addListener(filter -> getToolItem()
                        .setImage(menu.hasActiveFilter() ? Images.FILTER_ON.image() : Images.FILTER_OFF.image()));
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        menu.menuAboutToShow(manager);
    }

    public ClientFilter getSelectedFilter()
    {
        return menu.getSelectedFilter();
    }
}
