package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup;
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
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, IEclipseContext context)
    {
        if (!MenuHelper.isClientPartActive(part))
            return;

        IEclipseContext childContext = context.createChild();
        childContext.set(PortfolioPart.class, (PortfolioPart) part.getObject());

        ContextInjectionFactory.make(CommandPalettePopup.class, childContext).open();

        childContext.dispose();
    }

}
