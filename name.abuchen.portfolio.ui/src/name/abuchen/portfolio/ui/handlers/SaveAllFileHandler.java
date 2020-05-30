package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class SaveAllFileHandler
{
    @CanExecute
    boolean canExecute(EPartService partService)
    {
        return !partService.getDirtyParts().isEmpty();
    }

    @Execute
    public void execute(EPartService partService)
    {
        partService.saveAll(false);
    }
}
