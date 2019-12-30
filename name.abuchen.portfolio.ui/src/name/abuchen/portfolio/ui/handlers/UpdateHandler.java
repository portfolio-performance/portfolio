package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import name.abuchen.portfolio.ui.update.UpdateHelper;

public class UpdateHandler
{
    @Execute
    public void execute(final IWorkbench workbench, final EPartService partService)
    {
        UpdateHelper updateHelper = new UpdateHelper(workbench, partService);
        updateHelper.runUpdateWithUIMonitor();
    }
}
