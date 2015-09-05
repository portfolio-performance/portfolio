package name.abuchen.portfolio.ui.handlers;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Named;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.update.UpdateHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class UpdateHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell, final IWorkbench workbench,
                    final EPartService partService)
    {
        try
        {
            new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress()
            {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        UpdateHelper updateHelper = new UpdateHelper(workbench, partService);
                        updateHelper.runUpdate(monitor, false);
                    }
                    catch (CoreException e)
                    {
                        throw new InvocationTargetException(e);
                    }
                }
            });

        }
        catch (InvocationTargetException e)
        {
            PortfolioPlugin.log(e.getCause());

            IStatus status = e.getCause() instanceof CoreException ? ((CoreException) e.getCause()).getStatus()
                            : new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getCause().getMessage(),
                                            e.getCause());

            ErrorDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                            Messages.MsgErrorUpdating, status);
        }
        catch (InterruptedException ignore)
        {}
    }
}
