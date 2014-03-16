package name.abuchen.portfolio.ui.log;

import name.abuchen.portfolio.ui.UIConstants;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class OpenErrorLogPartHandler
{
    @Execute
    public void execute(EPartService partService)
    {
        MPart part = partService.findPart(UIConstants.Part.ERROR_LOG);
        part.setVisible(true);
        partService.activate(part, true);
    }
}
