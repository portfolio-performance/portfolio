package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class QuitHandler
{
    @Execute
    public void execute(IWorkbench workbench, EPartService partService)
    {
        boolean successful = partService.saveAll(true);

        if (successful)
            workbench.close();
    }
}
