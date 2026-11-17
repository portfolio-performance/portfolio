package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import name.abuchen.portfolio.ui.update.UpdateIndicatorAddon;

public class QuitHandler
{
    @Execute
    public void execute(IWorkbench workbench, EPartService partService, MApplication application)
    {
        boolean successful = partService.saveAll(true);

        if (!successful)
            return;

        // offer the update found by the background check only after the user
        // has decided about saving the files - and has not canceled

        UpdateIndicatorAddon.promptToInstallPendingUpdate(application);

        workbench.close();
    }
}
