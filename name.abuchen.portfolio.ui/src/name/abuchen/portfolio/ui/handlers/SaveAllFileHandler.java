package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class SaveAllFileHandler
{
    @Execute
    public void execute(EPartService partService)
    {
        partService.saveAll(false);
    }
}
