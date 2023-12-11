package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class OpenCommandPaletteHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Named(UIConstants.Parameter.TYPE) String type)
    {
        if (!MenuHelper.isClientPartActive(part))
            return;

        // use the EclipseContext of the active view

        // We need a context that lives longer than the palette popup so that
        // commands can be executed after the popup has been closed. We cannot
        // create a new context here because we have no event when to dispose
        // it. We cannot use the context of the part because it does not include
        // enough objects for the palette elements.

        // If there is no active view, then the client is either locked
        // (password not provided), loading, or failed to load. In these cases
        // we do not show a command palette at all.

        PortfolioPart portfolioPart = (PortfolioPart) part.getObject();
        final java.util.Optional<AbstractFinanceView> currentView = portfolioPart.getCurrentView();
        if (currentView.isEmpty())
            return;

        new CommandPalettePopup(currentView.get().getContext(), type).open();
    }

}
