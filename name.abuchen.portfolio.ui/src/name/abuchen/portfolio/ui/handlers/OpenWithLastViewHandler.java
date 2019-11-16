package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.services.IServiceConstants;

import name.abuchen.portfolio.ui.UIConstants;

public class OpenWithLastViewHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part, MMenuItem menuItem)
    {
        if (!MenuHelper.getActiveClientInput(part, false).isPresent())
            return false;

        menuItem.setSelected(!part.getPersistedState().containsKey(UIConstants.PersistedState.INITIAL_VIEW));

        return true;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        part.getPersistedState().remove(UIConstants.PersistedState.INITIAL_VIEW);
    }
}
