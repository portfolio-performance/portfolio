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

        String key = prefKey + ClientFilterMenu.PREF_KEY_POSTFIX;
        menu.loadPreselectedClientFilter(key);
        setImage(menu.hasActiveFilter() ? Images.FILTER_ON : Images.FILTER_OFF);

        // store/save selected filter
        addDisposeListener(e -> menu.saveSelectedFilter(key));
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
