package name.abuchen.portfolio.ui.util;

import java.util.function.Consumer;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;

public final class ClientFilterDropDown extends DropDown implements IMenuListener
{
    private ClientFilterMenu menu;

    public ClientFilterDropDown(Client client, IPreferenceStore preferences, String prefKey,
                    Consumer<ClientFilter> listener)
    {
        super(Messages.MenuChooseClientFilter, Images.FILTER_OFF, SWT.NONE);
        setMenuListener(this);

        this.menu = new ClientFilterMenu(client, preferences, listener);
        this.menu.addListener(filter -> setImage(menu.hasActiveFilter() ? Images.FILTER_ON : Images.FILTER_OFF));

        String selection = preferences.getString(prefKey + ClientFilterMenu.PREF_KEY_POSTFIX);
        if (selection != null)
        {
            this.menu.getAllItems().filter(item -> item.getUUIDs().equals(selection)).findAny().ifPresent(item -> {
                this.menu.select(item);
                setImage(menu.hasActiveFilter() ? Images.FILTER_ON : Images.FILTER_OFF);
            });
        }

        addDisposeListener(e -> preferences.putValue(prefKey + ClientFilterMenu.PREF_KEY_POSTFIX,
                        this.menu.getSelectedItem().getUUIDs()));
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

    public boolean hasActiveFilter()
    {
        return menu.hasActiveFilter();
    }
}
