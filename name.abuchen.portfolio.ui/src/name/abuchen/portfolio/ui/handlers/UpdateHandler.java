package name.abuchen.portfolio.ui.handlers;

import java.lang.reflect.InvocationTargetException;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.update.UpdateHelper;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

public class UpdateHandler extends AbstractHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        try
        {
            new ProgressMonitorDialog(null).run(true, true, new IRunnableWithProgress()
            {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        UpdateHelper updateHelper = new UpdateHelper();
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
                            e.getCause().getMessage(), status);
            return false;
        }
        catch (InterruptedException e)
        {
            return false;
        }

        return true;
    }
}
