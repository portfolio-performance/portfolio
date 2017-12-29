package name.abuchen.portfolio.ui.handlers;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Named;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.update.UpdateHelper;

public class UpdateHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell, final IWorkbench workbench,
                    final EPartService partService)
    {
        try
        {
            new ProgressMonitorDialog(shell).run(true, true, monitor -> {
                try // NOSONAR
                {
                    UpdateHelper updateHelper = new UpdateHelper(workbench, partService);
                    updateHelper.runUpdate(monitor, false);
                }
                catch (CoreException e)
                {
                    PortfolioPlugin.log(e);
                    Display.getDefault().asyncExec(() -> ErrorDialog.openError(Display.getDefault().getActiveShell(),
                                    Messages.LabelError, Messages.MsgErrorUpdating, e.getStatus()));
                }
            });

        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }
}
