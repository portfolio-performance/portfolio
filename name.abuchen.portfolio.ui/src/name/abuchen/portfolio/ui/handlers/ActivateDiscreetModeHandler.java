package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.services.IServiceConstants;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.ui.UIConstants;

public class ActivateDiscreetModeHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part, MMenuItem menuItem)
    {
        menuItem.setSelected(DiscreetMode.isActive());

        return true;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, IEventBroker broker)
    {
        toggle(MenuHelper.getActiveClient(part, false).orElse(null), broker);
    }

    /**
     * Flips the discreet mode of the application and, if a file is open, stores
     * the setting in that file so that the file is reopened with the same mode.
     */
    public static void toggle(Client client, IEventBroker broker)
    {
        DiscreetMode.setActive(!DiscreetMode.isActive());

        if (client != null)
            new ClientProperties(client).setDiscreetMode(DiscreetMode.isActive());

        broker.post(UIConstants.Event.Global.DISCREET_MODE, DiscreetMode.isActive());
    }
}
