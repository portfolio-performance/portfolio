package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class CloseFileHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart activePart)
    {
        return MenuHelper.isClientPartActive(activePart);
    }

    @Execute
    public void execute(@Optional @Named(IServiceConstants.ACTIVE_PART) MPart activePart, EPartService partService)
    {
        if (!MenuHelper.getActiveClientInput(activePart).isPresent())
            return;

        if (partService.savePart(activePart, true))
            partService.hidePart(activePart);
    }
}
