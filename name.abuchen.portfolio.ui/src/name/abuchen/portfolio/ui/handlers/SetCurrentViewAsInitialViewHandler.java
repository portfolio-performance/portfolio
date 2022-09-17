package name.abuchen.portfolio.ui.handlers;

import java.text.MessageFormat;
import java.util.Optional;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.services.IServiceConstants;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.Navigation;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class SetCurrentViewAsInitialViewHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part, MMenuItem menuItem)
    {
        if (!MenuHelper.getActiveClientInput(part, false).isPresent())
            return false;

        PortfolioPart portfolioPart = (PortfolioPart) part.getObject();

        Optional<Navigation.Item> view = portfolioPart.getSelectedItem();
        if (!view.isPresent())
            return false;

        menuItem.setLabel(MessageFormat.format(Messages.MenuSetCurrentViewAsInitialView, view.get().getLabel()));

        menuItem.setSelected(portfolioPart.getClientInput().getNavigation().getIdentifier(view.get())
                        .equals(part.getPersistedState().get(UIConstants.PersistedState.INITIAL_VIEW)));

        return true;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        MenuHelper.getActiveClientInput(part)
                        .ifPresent(clientInput -> ((PortfolioPart) part.getObject()).getSelectedItem()
                                        .ifPresent(view -> part.getPersistedState().put(
                                                        UIConstants.PersistedState.INITIAL_VIEW,
                                                        clientInput.getNavigation().getIdentifier(view))));
    }
}
