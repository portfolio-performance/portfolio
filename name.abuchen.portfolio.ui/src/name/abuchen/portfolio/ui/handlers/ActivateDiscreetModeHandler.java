package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.services.IServiceConstants;

import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.ui.PortfolioPlugin;
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
    public void execute(IEventBroker broker)
    {
        toggle(broker);
    }

    /**
     * Flips the discreet mode and stores the value in the preferences of the
     * installation, just like the value color scheme or the width of the chart
     * lines. The discreet mode is not a property of a single file: it hides the
     * amounts of all open files and of all files that are opened later, and it
     * survives a restart of the program. The preference is read at startup and
     * whenever it changes (see Preference2EnvAddon).
     */
    public static void toggle(IEventBroker broker)
    {
        boolean active = !DiscreetMode.isActive();

        PortfolioPlugin.getDefault().getPreferenceStore()
                        .setValue(UIConstants.Preferences.DISCREET_MODE, active);

        // writing the preference notifies the parts that listen to it, and those
        // apply the mode and send out the event; only do it here if that has not
        // happened yet
        if (DiscreetMode.isActive() == active)
            return;

        DiscreetMode.setActive(active);

        if (broker != null)
            broker.post(UIConstants.Event.Global.DISCREET_MODE, active);
    }
}
