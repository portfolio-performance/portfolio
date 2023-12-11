package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import name.abuchen.portfolio.ui.UIConstants;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class OpenPartHandler
{
    @Execute
    public void execute(EPartService partService, @Named(UIConstants.Parameter.PART) String partname)
    {
        MPart part = partService.findPart(partname);
        part.setVisible(true);
        partService.activate(part, true);
    }
}
