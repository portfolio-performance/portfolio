package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
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
    public void execute()
    {
        toggle();
    }

    /**
     * Flips the discreet mode by writing the preference; Preference2EnvAddon
     * applies it.
     */
    public static void toggle()
    {
        PortfolioPlugin.getDefault().getPreferenceStore()
                        .setValue(UIConstants.Preferences.DISCREET_MODE, !DiscreetMode.isActive());
    }
}
